package ru.javaops.masterjava.service.mail;

import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.web.WsClient;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

@Slf4j
public class MailWSClient {
    private static WsClient<MailService> WS_CLIENT;

    static {
        try {
            WS_CLIENT = new WsClient<>(new URL("file:/apps/masterjava/config/wsdl/mailService.wsdl"),
                    new QName("http://mail.javaops.ru/", "MailServiceImplService"),
                    MailService.class);
            WS_CLIENT.init("mail", "/mail/mailService?wsdl");
        } catch (MalformedURLException e) {
            log.error("Error creating WS_CLIENT: " + e, e);
        }
    }


    public static String sendToGroup(final Set<Addressee> to, final Set<Addressee> cc, final String subject, final String body) {
        log.info("Send mail to '" + to + "' cc '" + cc + "' subject '" + subject + (log.isDebugEnabled() ? "\nbody=" + body : ""));
        return WS_CLIENT.getPort().sendToGroup(to, cc, subject, body);
    }
}
