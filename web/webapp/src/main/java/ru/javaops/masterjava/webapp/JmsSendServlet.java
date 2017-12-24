package ru.javaops.masterjava.webapp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalStateException;

@WebServlet("/sendJms")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, //10 MB in memory limit
        maxFileSize = 1024 * 1024 * 20)
@Slf4j
public class JmsSendServlet extends HttpServlet {
    private Connection connection;
    private Session session;
    private MessageProducer producer;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            InitialContext initCtx = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) initCtx.lookup("java:comp/env/jms/ConnectionFactory");
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer((Destination) initCtx.lookup("java:comp/env/jms/queue/MailQueue"));
        } catch (Exception e) {
            throw new IllegalStateException("JMS init failed", e);
        }
    }

    @Override
    public void destroy() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                log.warn("Couldn't close JMSConnection: ", ex);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String result;
        try {
            log.info("Start sending");
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            String users = req.getParameter("users");
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            Part filePart = req.getPart("attach");

            if (filePart == null) {
                result = sendJms(users, subject, body);
            } else {
                result = sendJmsF(users, subject, body,
                        filePart.getSubmittedFileName(),
                        filePart.getInputStream());
            }
            log.info("Processing finished with result: {}", result);
        } catch (Exception e) {
            log.error("Processing failed", e);
            result = e.toString();
        }
        resp.getWriter().write(result);
    }

    private synchronized String sendJms(String users, String subject, String body) throws JMSException {
        ObjectMessage objectMessage = session.createObjectMessage();
        objectMessage.setStringProperty("users", users);
        objectMessage.setStringProperty("subject", subject);
        objectMessage.setStringProperty("body", body);
        producer.send(objectMessage);
        return "Successfully sent JMS message";
    }

    private synchronized String sendJmsF(String users, String subject, String body,
                                         String filename, InputStream attachDataStream) throws JMSException, IOException {
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(IOUtils.toByteArray(attachDataStream));
        bytesMessage.setStringProperty("users", users);
        bytesMessage.setStringProperty("subject", subject);
        bytesMessage.setStringProperty("body", body);
        bytesMessage.setStringProperty("attachName", filename);
        producer.send(bytesMessage);
        return "Successfully sent JMS message";
    }
}