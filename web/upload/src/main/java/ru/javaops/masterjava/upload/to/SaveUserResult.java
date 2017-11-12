package ru.javaops.masterjava.upload.to;

import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;

/**
 * Created by vit on 12.11.2017.
 */
public class SaveUserResult {
    private String fullName;
    private String email;
    private UserFlag flag;
    private Result result;
    private String exceptionDetails;

    public SaveUserResult(User user, Result result, String exceptionDetails) {
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.flag = user.getFlag();
        this.result = result;
        this.exceptionDetails = exceptionDetails;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserFlag getFlag() {
        return flag;
    }

    public void setFlag(UserFlag flag) {
        this.flag = flag;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public String getExceptionDetails() {
        return exceptionDetails;
    }

    public void setExceptionDetails(String exceptionDetails) {
        this.exceptionDetails = exceptionDetails;
    }

    public enum Result {
        SUCCESS {
            @Override
            public String toString() {
                return "Success";
            }
        },
        EMAIL_ALREADY_EXISTS {
            @Override
            public String toString() {
                return "User with such email already exists";
            }
        },
        EXCEPTION {
            @Override
            public String toString() {
                return "Exception";
            }
        }
    }
}
