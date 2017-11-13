package ru.javaops.masterjava.upload.to;

import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;

/**
 * Created by vit on 12.11.2017.
 */
public class SaveUserResult {
    private final String fullName;
    private final String email;
    private final UserFlag flag;
    private final Result result;

    public SaveUserResult(User user, Result result, String exceptionDetails) {
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.flag = user.getFlag();
        this.result = result;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public UserFlag getFlag() {
        return flag;
    }

    public Result getResult() {
        return result;
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
