package ru.javaops.masterjava.persist;

import com.google.common.collect.ImmutableList;
import ru.javaops.masterjava.persist.dao.CityDao;
import ru.javaops.masterjava.persist.dao.GroupDao;
import ru.javaops.masterjava.persist.dao.ProjectDao;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.City;
import ru.javaops.masterjava.persist.model.Group;
import ru.javaops.masterjava.persist.model.Project;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.types.GroupType;
import ru.javaops.masterjava.persist.model.types.UserFlag;

import java.util.List;

public class UserTestData {
    public static City CITY_SPB;
    public static City CITY_MOW;
    public static City CITY_KIV;
    public static City CITY_MNSK;

    public static User ADMIN;
    public static User DELETED;
    public static User FULL_NAME;
    public static User USER1;
    public static User USER2;
    public static User USER3;

    public static Project PROJECT1;
    public static Project PROJECT2;
    public static Project PROJECT3;

    public static Group GROUP1;
    public static Group GROUP2;
    public static Group GROUP3;

    public static List<City> FIRST4_CITIES;
    public static List<User> FIRST5_USERS;
    public static List<Project> FIRST3_PROJECTS;
    public static List<Group> FIRST3_GROUPS;

    public static void init() {
        CITY_SPB = new City("spb", "Санкт-Петербург");
        CITY_MOW = new City("mow", "Москва");
        CITY_KIV = new City("kiv", "Киев");
        CITY_MNSK = new City("mnsk", "Минск");
        FIRST4_CITIES = ImmutableList.of(CITY_KIV, CITY_MNSK, CITY_MOW, CITY_SPB);

        ADMIN = new User("Admin", "admin@javaops.ru", UserFlag.superuser, CITY_SPB);
        DELETED = new User("Deleted", "deleted@yandex.ru", UserFlag.deleted, CITY_SPB);
        FULL_NAME = new User("Full Name", "gmail@gmail.com", UserFlag.active, CITY_KIV);
        USER1 = new User("User1", "user1@gmail.com", UserFlag.active, CITY_MOW);
        USER2 = new User("User2", "user2@yandex.ru", UserFlag.active, CITY_KIV);
        USER3 = new User("User3", "user3@yandex.ru", UserFlag.active, CITY_MNSK);
        FIRST5_USERS = ImmutableList.of(ADMIN, DELETED, FULL_NAME, USER1, USER2);

        PROJECT1 = new Project("Project1", "Description1");
        PROJECT2 = new Project("Project2", "Description2");
        PROJECT3 = new Project("Project3", "Description3");
        FIRST3_PROJECTS = ImmutableList.of(PROJECT1, PROJECT2, PROJECT3);

        GROUP1 = new Group("group1", GroupType.CURRENT, PROJECT1);
        GROUP2 = new Group("group2", GroupType.FINISHED, PROJECT1);
        GROUP3 = new Group("group3", GroupType.REGISTERING, PROJECT2);
        FIRST3_GROUPS = ImmutableList.of(GROUP1, GROUP2, GROUP3);


    }

    public static void setUp() {
        CityDao cityDao = DBIProvider.getDao(CityDao.class);
        ProjectDao projectDao = DBIProvider.getDao(ProjectDao.class);
        GroupDao groupDao = DBIProvider.getDao(GroupDao.class);
        UserDao userDao = DBIProvider.getDao(UserDao.class);
        userDao.clean();
        cityDao.clean();
        groupDao.clean();
        projectDao.clean();
        DBIProvider.getDBI().useTransaction((conn, status) -> {
            FIRST4_CITIES.forEach(cityDao::insert);
            FIRST3_PROJECTS.forEach(projectDao::insert);
            FIRST3_GROUPS.forEach(groupDao::insert);
            FIRST5_USERS.forEach(userDao::insert);
            userDao.insert(USER3);
        });
    }
}
