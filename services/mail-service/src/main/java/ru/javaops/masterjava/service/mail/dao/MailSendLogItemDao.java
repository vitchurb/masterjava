package ru.javaops.masterjava.service.mail.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import ru.javaops.masterjava.persist.dao.AbstractDao;
import ru.javaops.masterjava.service.mail.model.MailSendLogItem;

import java.util.List;

/**
 * Created by vit on 26.11.2017.
 */
@RegisterMapperFactory(EntityMapperFactory.class)
public abstract class MailSendLogItemDao implements AbstractDao {
    @SqlUpdate("TRUNCATE mail_send_log_item CASCADE ")
    @Override
    public abstract void clean();

    @SqlQuery("SELECT * FROM mail_send_log_item ORDER BY id")
    public abstract List<MailSendLogItem> getAll();


    @SqlUpdate("INSERT INTO mail_send_log_item (dt, count_addresses, result, result_comment, addresses_to, addresses_cc) " +
            " VALUES (:dt, :countAddresses, CAST(:result AS MAIL_SEND_LOG_ITEM_RESULT), :resultComment, :addressesTo, :addressesCc)")
    @GetGeneratedKeys
    public abstract int insertGeneratedId(@BindBean MailSendLogItem mailSendLogItem);

    public void insert(MailSendLogItem mailSendLogItem) {
        int id = insertGeneratedId(mailSendLogItem);
        mailSendLogItem.setId(id);
    }

}
