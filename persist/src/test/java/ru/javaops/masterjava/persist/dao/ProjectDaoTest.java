package ru.javaops.masterjava.persist.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.UserTestData;
import ru.javaops.masterjava.persist.model.Project;

import java.util.List;

import static ru.javaops.masterjava.persist.UserTestData.FIRST3_PROJECTS;

public class ProjectDaoTest extends AbstractDaoTest<ProjectDao> {

    private static final GroupDao groupDao = DBIProvider.getDao(GroupDao.class);

    public ProjectDaoTest() {
        super(ProjectDao.class);
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
    public void getList() {
        List<Project> projects = dao.getWithLimit(5);
        List<Project> projectsUnordered = dao.getAllUnordered();
        Assert.assertEquals(FIRST3_PROJECTS, projects);
        Assert.assertEquals(3, projectsUnordered.size());
    }

    @Test
    public void insertBatch() throws Exception {
        groupDao.clean();
        dao.clean();
        dao.insertBatchSkipDublicates(FIRST3_PROJECTS, 3);
        Assert.assertEquals(3, dao.getWithLimit(100).size());
    }

    @Test
    public void getSeqAndSkip() throws Exception {
        int seq1 = dao.getSeqAndSkip(5);
        int seq2 = dao.getSeqAndSkip(1);
        Assert.assertEquals(5, seq2 - seq1);
    }
}