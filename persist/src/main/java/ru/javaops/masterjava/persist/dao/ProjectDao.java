package ru.javaops.masterjava.persist.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.model.Project;

import java.util.List;

@RegisterMapperFactory(EntityMapperFactory.class)
public abstract class ProjectDao implements AbstractDao {

    public Project insert(Project project) {
        if (project.isNew()) {
            int id = insertGeneratedId(project);
            project.setId(id);
        } else {
            insertWitId(project);
        }
        return project;
    }

    @SqlQuery("SELECT nextval('project_seq')")
    abstract int getNextVal();

    @Transaction
    public int getSeqAndSkip(int step) {
        int id = getNextVal();
        DBIProvider.getDBI().useHandle(h -> h.execute("ALTER SEQUENCE project_seq RESTART WITH " + (id + step)));
        return id;
    }

    @SqlUpdate("INSERT INTO projects (name, description) " +
            "VALUES (:name, :description) ")
    @GetGeneratedKeys
    abstract int insertGeneratedId(@BindBean Project project);

    @SqlUpdate("INSERT INTO projects (id, name, description) " +
            " VALUES (:id, :name, :description) ")
    abstract void insertWitId(@BindBean Project project);

    @SqlQuery("SELECT * FROM projects ORDER BY name LIMIT :it")
    public abstract List<Project> getWithLimit(@Bind int limit);

    @SqlQuery("SELECT * FROM projects")
    public abstract List<Project> getAllUnordered();

    @SqlUpdate("DELETE FROM projects")
    @Override
    public abstract void clean();

    //    https://habrahabr.ru/post/264281/
    @SqlBatch("INSERT INTO projects (id, name, description) " +
            "VALUES (:id, :name, :description)" +
            "ON CONFLICT (name) DO NOTHING")
    public abstract int[] insertBatchSkipDublicates(@BindBean List<Project> projects, @BatchChunkSize int chunkSize);


}
