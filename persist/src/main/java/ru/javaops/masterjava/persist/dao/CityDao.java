package ru.javaops.masterjava.persist.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import one.util.streamex.IntStreamEx;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.model.City;

import java.util.List;

@RegisterMapperFactory(EntityMapperFactory.class)
@UseStringTemplate3StatementLocator
public abstract class CityDao implements AbstractDao {

    public City insert(City city) {
        if (city.isNew()) {
            int id = insertGeneratedId(city);
            city.setId(id);
        } else {
            insertWitId(city);
        }
        return city;
    }

    @SqlQuery("SELECT nextval('city_seq')")
    abstract int getNextVal();

    @Transaction
    public int getSeqAndSkip(int step) {
        int id = getNextVal();
        DBIProvider.getDBI().useHandle(h -> h.execute("ALTER SEQUENCE city_seq RESTART WITH " + (id + step)));
        return id;
    }

    @SqlUpdate("INSERT INTO cities (code, name) VALUES (:code, :name) ")
    @GetGeneratedKeys
    abstract int insertGeneratedId(@BindBean City city);

    @SqlUpdate("INSERT INTO cities (id, code, name) VALUES (:id, :code, :name) ")
    abstract void insertWitId(@BindBean City city);

    @SqlQuery("SELECT * FROM cities ORDER BY code LIMIT :it")
    public abstract List<City> getWithLimit(@Bind int limit);

    @SqlQuery("SELECT * FROM cities ")
    public abstract List<City> getAllUnordered();

    @SqlQuery("SELECT * FROM cities WHERE code in (<codes>) ")
    public abstract List<City> getByCodesUnordered(
            @BindIn(onEmpty = BindIn.EmptyHandling.NULL, value = "codes") List<String> codes);

    @SqlUpdate("DELETE FROM cities")
    @Override
    public abstract void clean();

    //    https://habrahabr.ru/post/264281/
    @SqlBatch("INSERT INTO cities (id, code, name) VALUES (:id, :code, :name)" +
            "ON CONFLICT (code) DO NOTHING")
    protected abstract int[] insertBatchSkipDublicates(@BindBean List<City> cities);

    public List<String> insertBatchGetInsertedCodes(@BindBean List<City> cities) {
        int[] resultArray = insertBatchSkipDublicates(cities);
        return IntStreamEx.range(0, cities.size())
                .filter(i -> resultArray[i] != 0)
                .mapToObj(index -> cities.get(index).getCode())
                .toList();
    }
}
