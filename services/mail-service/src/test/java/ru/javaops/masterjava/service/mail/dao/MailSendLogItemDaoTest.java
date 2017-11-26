package ru.javaops.masterjava.service.mail.dao;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.javaops.masterjava.service.mail.MailSendLogItemTestData;
import ru.javaops.masterjava.service.mail.model.MailSendLogItem;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.javaops.masterjava.service.mail.MailSendLogItemTestData.ITEMS;

/**
 * Created by vit on 26.11.2017.
 */
public class MailSendLogItemDaoTest extends AbstractDaoTest<MailSendLogItemDao> {
    public MailSendLogItemDaoTest() {
        super(MailSendLogItemDao.class);
    }

    @BeforeClass
    public static void init() throws Exception {
        MailSendLogItemTestData.init();
    }

    @Before
    public void setUp() throws Exception {
        MailSendLogItemTestData.setUp();
    }

    @Test
    public void getAll() throws Exception {
        final List<MailSendLogItem> items = dao.getAll();
        assertEquals(ITEMS, items);
        System.out.println(items);
    }

}