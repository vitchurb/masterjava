package ru.javaops.masterjava.service.mail.listeners;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.service.mail.GroupResult;
import ru.javaops.masterjava.service.mail.MailServiceExecutor;
import ru.javaops.masterjava.service.mail.MailWSClient;
import ru.javaops.masterjava.service.mail.util.Attachments;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.ByteArrayInputStream;
import java.util.Collections;

@WebListener
@Slf4j
public class JmsMailListener implements ServletContextListener {
    private Thread listenerThread = null;
    private QueueConnection connection;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            InitialContext initCtx = new InitialContext();
            QueueConnectionFactory connectionFactory =
                    (QueueConnectionFactory) initCtx.lookup("java:comp/env/jms/ConnectionFactory");
            connection = connectionFactory.createQueueConnection();
            QueueSession queueSession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = (Queue) initCtx.lookup("java:comp/env/jms/queue/MailQueue");
            QueueReceiver receiver = queueSession.createReceiver(queue);
            connection.start();
            log.info("Listen JMS messages ...");
            listenerThread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Message m = receiver.receive();
                        if (m instanceof ObjectMessage) {
                            ObjectMessage om = (ObjectMessage) m;
                            String users = om.getStringProperty("users");
                            String subject = om.getStringProperty("subject");
                            String body = om.getStringProperty("body");
                            log.info("Received ObjectMessage with users: '{}' subject: '{}' body: '{}' ",
                                    users, subject, body);
                            GroupResult gr = MailServiceExecutor.sendBulk(MailWSClient.split(users), subject, body,
                                    Collections.emptyList());
                            log.info("Result sending email : {}", gr);
                        } else if (m instanceof BytesMessage) {
                            BytesMessage om = (BytesMessage) m;
                            String users = om.getStringProperty("users");
                            String subject = om.getStringProperty("subject");
                            String body = om.getStringProperty("body");
                            String attachName = om.getStringProperty("attachName");
                            long size = om.getBodyLength();
                            byte[] data = new byte[(int) size];
                            int resReadSata = om.readBytes(data);

                            log.info("Received BytesMessage with users: '{}' subject: '{}' body: '{}' filename: '{}' fileSize: `{}`",
                                    users, subject, body, attachName, resReadSata);
                            GroupResult gr = MailServiceExecutor.sendBulk(MailWSClient.split(users), subject, body,
                                    ImmutableList.of(Attachments.getAttachment(attachName, new ByteArrayInputStream(data))));
                            log.info("Result sending email : {}", gr);
                        }
                    }
                } catch (Exception e) {
                    log.error("Receiving messages failed: " + e.getMessage(), e);
                }
            });
            listenerThread.start();
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                log.warn("Couldn't close JMSConnection: ", ex);
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}