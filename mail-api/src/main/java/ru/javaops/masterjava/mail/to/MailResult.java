package ru.javaops.masterjava.mail.to;

import static ru.javaops.masterjava.mail.constants.MailConstants.OK;

/**
 * Created by vit on 06.11.2017.
 */
public class MailResult {
    private final String email;
    private final String result;

    public static MailResult ok(String email) {
        return new MailResult(email, OK);
    }

    public static MailResult error(String email, String error) {
        return new MailResult(email, error);
    }

    public boolean isOk() {
        return OK.equals(result);
    }

    private MailResult(String email, String cause) {
        this.email = email;
        this.result = cause;
    }

    @Override
    public String toString() {
        return '(' + email + ',' + result + ')';
    }
}
