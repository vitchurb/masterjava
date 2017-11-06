package ru.javaops.masterjava.mail.to;

import java.util.List;

/**
 * Created by vit on 06.11.2017.
 */
public class GroupResult {
    private final int success; // number of successfully sent email
    private final List<MailResult> failed; // failed emails with causes
    private final String failedCause;  // global fail cause

    public GroupResult(int success, List<MailResult> failed, String failedCause) {
        this.success = success;
        this.failed = failed;
        this.failedCause = failedCause;
    }

    @Override
    public String toString() {
        return "Success: " + success + '\n' +
                "Failed: " + failed.toString() + '\n' +
                (failedCause == null ? "" : "Failed cause" + failedCause);
    }
}
