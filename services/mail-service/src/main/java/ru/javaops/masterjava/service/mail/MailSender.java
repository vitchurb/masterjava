package ru.javaops.masterjava.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.service.mail.dao.MailSendLogItemDao;
import ru.javaops.masterjava.service.mail.model.MailSendLogItem;
import ru.javaops.masterjava.service.mail.model.type.SendingResult;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MailSender {
    private static final MailSendLogItemDao mailSendLogItemDao = DBIProvider.getDao(MailSendLogItemDao.class);

    static void sendMail(List<Addressee> to, List<Addressee> cc, String subject, String body) {
        log.info("Send mail to \'" + to + "\' cc \'" + cc + "\' subject \'" + subject + (log.isDebugEnabled() ? "\nbody=" + body : ""));
        try {
            Email simpleEmail = new SimpleEmail();
            simpleEmail.setHostName(MailConfig.getHost());
            simpleEmail.setSmtpPort(MailConfig.getPort());
            simpleEmail.setAuthenticator(new DefaultAuthenticator(MailConfig.getUsername(),
                    MailConfig.getPassword()));
            simpleEmail.setSSLOnConnect(MailConfig.getUseSSL());
            simpleEmail.setStartTLSEnabled(MailConfig.getUseTLS());
            simpleEmail.setDebug(MailConfig.getDebug());
            simpleEmail.setFrom(MailConfig.getUsername(), MailConfig.getFromName());
            simpleEmail.setSubject(subject);
            simpleEmail.setMsg(body);
            if (to != null)
                for (Addressee addressee : to) {
                    simpleEmail.addTo(addressee.getEmail(), addressee.getName());
                }
            if (cc != null)
                for (Addressee addressee : cc) {
                    simpleEmail.addCc(addressee.getEmail(), addressee.getName());
                }
            String resultSend = simpleEmail.send();
            MailSendLogItem logItem = new MailSendLogItem(new Date(),
                    (to == null ? 0 : to.size()) + (cc == null ? 0 : cc.size()),
                    SendingResult.SUCCESS, resultSend,
                    getAddressesAsString(to), getAddressesAsString(cc));
            mailSendLogItemDao.insert(logItem);
        } catch (Exception e) {
            log.error("error", e);
            MailSendLogItem logItem = new MailSendLogItem(new Date(),
                    (to == null ? 0 : to.size()) + (cc == null ? 0 : cc.size()),
                    SendingResult.ERROR, e.toString(),
                    getAddressesAsString(to), getAddressesAsString(cc));
            mailSendLogItemDao.insert(logItem);
        }
    }

    private static String getAddressesAsString(final List<Addressee> addressees) {
        if (addressees == null)
            return null;
        return addressees.stream()
                .map(Addressee::getEmail)
                .collect(Collectors.joining(","));
    }
}
