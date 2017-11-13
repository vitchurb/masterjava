package ru.javaops.masterjava.upload.to;

/**
 * Created by vit on 13.11.2017.
 */
public class SaveChunkError {
    private final String firstEmail;
    private final String lastEmail;
    private final SaveUserResult.Result result;
    private final String exceptionDetails;

    public SaveChunkError(String firstEmail, String lastEmail, SaveUserResult.Result result, String exceptionDetails) {
        this.firstEmail = firstEmail;
        this.lastEmail = lastEmail;
        this.result = result;
        this.exceptionDetails = exceptionDetails;
    }

    public String getFirstEmail() {
        return firstEmail;
    }

    public String getLastEmail() {
        return lastEmail;
    }

    public SaveUserResult.Result getResult() {
        return result;
    }

    public String getExceptionDetails() {
        return exceptionDetails;
    }
}
