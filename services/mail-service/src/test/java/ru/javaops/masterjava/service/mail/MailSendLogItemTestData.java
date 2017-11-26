package ru.javaops.masterjava.service.mail;

import com.google.common.collect.ImmutableList;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.service.mail.dao.MailSendLogItemDao;
import ru.javaops.masterjava.service.mail.model.MailSendLogItem;
import ru.javaops.masterjava.service.mail.model.type.SendingResult;

import java.util.Date;
import java.util.List;

/**
 * Created by vit on 26.11.2017.
 */
public class MailSendLogItemTestData {

    public static MailSendLogItem LOG_ITEM_1;
    public static MailSendLogItem LOG_ITEM_2;

    public static List<MailSendLogItem> ITEMS;

    public static void init() {
        LOG_ITEM_1 = new MailSendLogItem(new Date(), 5, SendingResult.ERROR, "Error_1", "vb@g.com", null);
        LOG_ITEM_2 = new MailSendLogItem(new Date(), 7, SendingResult.SUCCESS, "Error_2", "vb4@g.com", "v7b4@g.com");

        ITEMS = ImmutableList.of(LOG_ITEM_1, LOG_ITEM_2);
    }

    public static void setUp() {
        MailSendLogItemDao dao = DBIProvider.getDao(MailSendLogItemDao.class);
        dao.clean();
        DBIProvider.getDBI().useTransaction((conn, status) -> {
            dao.insert(LOG_ITEM_1);
            dao.insert(LOG_ITEM_2);
        });
    }

}
