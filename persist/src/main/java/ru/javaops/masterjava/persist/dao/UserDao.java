package ru.javaops.masterjava.persist.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;

import java.util.ArrayList;
import java.util.List;

@RegisterMapperFactory(EntityMapperFactory.class)
@UseStringTemplate3StatementLocator
public abstract class UserDao implements AbstractDao {

    public User insert(User user) {
        if (user.isNew()) {
            int id = insertGeneratedId(user);
            user.setId(id);
        } else {
            insertWitId(user);
        }
        return user;
    }

    public int[] insertNewBatchNoConflictEmail(List<User> users) {
        List<String> fullNames = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        List<UserFlag> userFlags = new ArrayList<>();
        users.forEach(u -> {
            fullNames.add(u.getFullName());
            emails.add(u.getEmail());
            userFlags.add(u.getFlag());
        });
        int[] insertedIds = insertBatchNoConflictEmail(fullNames, emails, userFlags);
        return insertedIds;
    }

    @SqlBatch("INSERT INTO users (full_name, email, flag) VALUES (:fullName, :email, CAST(:flag AS user_flag)) ON CONFLICT (email) DO NOTHING;")
    @GetGeneratedKeys
    abstract int[] insertBatchNoConflictEmail(@Bind("fullName") List<String> fullNames,
                                              @Bind("email") List<String> emails,
                                              @Bind("flag") List<UserFlag> userFlags);

    @SqlUpdate("INSERT INTO users (full_name, email, flag) VALUES (:fullName, :email, CAST(:flag AS user_flag)) ")
    @GetGeneratedKeys
    abstract int insertGeneratedId(@BindBean User user);

    @SqlUpdate("INSERT INTO users (id, full_name, email, flag) VALUES (:id, :fullName, :email, CAST(:flag AS user_flag)) ")
    abstract void insertWitId(@BindBean User user);

    @SqlQuery("SELECT * FROM users ORDER BY full_name, email LIMIT :it")
    public abstract List<User> getWithLimit(@Bind int limit);

    @SqlQuery("SELECT users.email FROM users where email in (<emails>) and id not in (<ids>);")
    public abstract List<String> getEmailByEmailsNotInIds(
            @BindIn(onEmpty = BindIn.EmptyHandling.NULL, value = "emails") List<String> emails,
            @BindIn(value = "ids") int[] ids);

    @SqlQuery("SELECT users.email FROM users where email in (<emails>) ")
    public abstract List<String> getEmailByEmails(
            @BindIn(onEmpty = BindIn.EmptyHandling.NULL, value = "emails") List<String> emails);

    //   http://stackoverflow.com/questions/13223820/postgresql-delete-all-content
    @SqlUpdate("TRUNCATE users")
    @Override
    public abstract void clean();
}
