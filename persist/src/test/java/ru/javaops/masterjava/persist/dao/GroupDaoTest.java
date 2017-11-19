package ru.javaops.masterjava.persist.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.javaops.masterjava.persist.UserTestData;
import ru.javaops.masterjava.persist.model.Group;

import java.util.List;

import static ru.javaops.masterjava.persist.UserTestData.FIRST3_GROUPS;

public class GroupDaoTest extends AbstractDaoTest<GroupDao> {

    public GroupDaoTest() {
        super(GroupDao.class);
    }

    @BeforeClass
    public static void init() throws Exception {
        UserTestData.init();
    }

    @Before
    public void setUp() throws Exception {
        UserTestData.setUp();
    }

    @Test
    public void getWithLimit() {
        List<Group> groups = dao.getWithLimit(5);
        List<Group> groupsUnordered = dao.getAllUnordered();
        Assert.assertEquals(FIRST3_GROUPS, groups);
        Assert.assertEquals(3, groupsUnordered.size());
    }

    @Test
    public void insertBatch() throws Exception {
        dao.clean();
        dao.insertBatchSkipDublicates(FIRST3_GROUPS, 1);
        List<Group> groups = dao.getWithLimit(100);
        Assert.assertEquals(3, groups.size());
    }

    @Test
    public void getSeqAndSkip() throws Exception {
        int seq1 = dao.getSeqAndSkip(5);
        int seq2 = dao.getSeqAndSkip(1);
        Assert.assertEquals(5, seq2 - seq1);
    }
}