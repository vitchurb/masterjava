package ru.javaops.masterjava.mail.service;

import ru.javaops.masterjava.mail.to.GroupResult;
import ru.javaops.masterjava.mail.to.MailResult;

import java.util.Set;

public interface MailService {

    GroupResult sendToList(final String template, final Set<String> emails) throws Exception;

    // dummy realization
    MailResult sendToUser(String template, String email) throws Exception;

}