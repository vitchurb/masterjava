package ru.javaops.masterjava.persist.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.UserTestData;
import ru.javaops.masterjava.persist.model.City;

import java.util.List;
import java.util.stream.Collectors;

import static ru.javaops.masterjava.persist.UserTestData.FIRST4_CITIES;

public class CityDaoTest extends AbstractDaoTest<CityDao> {

    private static final UserDao userDao = DBIProvider.getDao(UserDao.class);

    public CityDaoTest() {
        super(CityDao.class);
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
        List<City> cities = dao.getWithLimit(5);
        Assert.assertEquals(FIRST4_CITIES, cities);
        List<String> codes = cities.stream()
                .map(City::getCode)
                .collect(Collectors.toList());
        List<City> citiesByCodes = dao.getByCodesUnordered(codes);
        Assert.assertEquals(citiesByCodes.size(), 4);
    }

    @Test
    public void insertBatch() throws Exception {
        userDao.clean();
        dao.clean();
        dao.insertBatchSkipDublicates(FIRST4_CITIES);
        Assert.assertEquals(4, dao.getWithLimit(100).size());
    }

    @Test
    public void getSeqAndSkip() throws Exception {
        int seq1 = dao.getSeqAndSkip(5);
        int seq2 = dao.getSeqAndSkip(1);
        Assert.assertEquals(5, seq2 - seq1);
    }
}