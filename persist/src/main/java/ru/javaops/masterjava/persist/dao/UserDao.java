package ru.javaops.masterjava.persist.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import one.util.streamex.IntStreamEx;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.model.City;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.types.UserFlag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterMapperFactory(EntityMapperFactory.class)
public abstract class UserDao implements AbstractDao {

    public User insert(User user) {
        if (user.isNew()) {
            int id = insertGeneratedId(user, user.getCity().getId());
            user.setId(id);
        } else {
            insertWitId(user, user.getCity().getId());
        }
        return user;
    }

    @SqlQuery("SELECT nextval('user_seq')")
    abstract int getNextVal();

    @Transaction
    public int getSeqAndSkip(int step) {
        int id = getNextVal();
        DBIProvider.getDBI().useHandle(h -> h.execute("ALTER SEQUENCE user_seq RESTART WITH " + (id + step)));
        return id;
    }

    @SqlUpdate("INSERT INTO users (full_name, email, flag, city_id) " +
            "VALUES (:fullName, :email, CAST(:flag AS USER_FLAG), :cityId) ")
    @GetGeneratedKeys
    abstract int insertGeneratedId(@BindBean User user, @Bind("cityId") Integer cityId);

    @SqlUpdate("INSERT INTO users (id, full_name, email, flag, city_id) " +
            "VALUES (:id, :fullName, :email, CAST(:flag AS USER_FLAG), :cityId) ")
    abstract void insertWitId(@BindBean User user, @Bind("cityId") Integer cityId);

    @SqlQuery("SELECT users.*, cities.code as cityCode, cities.name as cityName FROM users " +
            " LEFT JOIN cities on cities.id = users.city_id " +
            " ORDER BY full_name, email LIMIT :it")
    @Mapper(UserWithCityMapper.class)
    public abstract List<User> getWithLimit(@Bind int limit);

    //   http://stackoverflow.com/questions/13223820/postgresql-delete-all-content
    @SqlUpdate("TRUNCATE users")
    @Override
    public abstract void clean();

    //    https://habrahabr.ru/post/264281/
    @SqlBatch("INSERT INTO users (id, full_name, email, flag, city_id) " +
            " VALUES (:id, :fullName, :email, CAST(:flag AS USER_FLAG), :cityId)" +
            " ON CONFLICT DO NOTHING")
    protected abstract int[] insertBatch(@BindBean List<User> users,
                                         @Bind("cityId") List<Integer> cityIds,
                                         @BatchChunkSize int chunkSize);


    public int[] insertBatch(@BindBean List<User> users, @BatchChunkSize int chunkSize) {
        List<Integer> cityIds = new ArrayList<>(users.size());
        for (User user : users) {
            cityIds.add(user.getCity().getId());
        }
        return insertBatch(users, cityIds, chunkSize);
    }

    public List<String> insertAndGetConflictEmails(List<User> users) {
        int[] result = insertBatch(users, users.size());
        return IntStreamEx.range(0, users.size())
                .filter(i -> result[i] == 0)
                .mapToObj(index -> users.get(index).getEmail())
                .toList();
    }

    public static class UserWithCityMapper implements ResultSetMapper<User> {
        public User map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            Map<Integer, City> cities = (Map<Integer, City>) ctx.getAttribute("cities");
            if (cities == null) {
                cities = new HashMap<>();
                ctx.setAttribute("cities", cities);
            }
            City city = cities.get(rs.getInt("city_id"));
            if (city == null) {
                city = new City(rs.getInt("city_id"), rs.getString("cityCode"),
                        rs.getString("cityName"));
                cities.put(city.getId(), city);
            }
            User u = new User(rs.getInt("id"), rs.getString("full_name"),
                    rs.getString("email"),
                    UserFlag.valueOf(rs.getString("flag")),
                    city);
            return u;
        }
    }

}
