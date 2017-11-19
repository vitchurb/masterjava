package ru.javaops.masterjava.persist.dao;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.model.Group;
import ru.javaops.masterjava.persist.model.Project;
import ru.javaops.masterjava.persist.model.types.GroupType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vit on 19.11.2017.
 */
@RegisterMapperFactory(EntityMapperFactory.class)
public abstract class GroupDao implements AbstractDao {

    public Group insert(Group group) {
        if (group.isNew()) {
            int id = insertGeneratedId(group, group.getProject().getId());
            group.setId(id);
        } else {
            insertWitId(group, group.getProject().getId());
        }
        return group;
    }

    @SqlQuery("SELECT nextval('group_seq')")
    abstract int getNextVal();

    @Transaction
    public int getSeqAndSkip(int step) {
        int id = getNextVal();
        DBIProvider.getDBI().useHandle(h -> h.execute("ALTER SEQUENCE group_seq RESTART WITH " + (id + step)));
        return id;
    }

    @SqlUpdate("INSERT INTO groups (name, type, project_id) " +
            "VALUES (:name, CAST(:type AS GROUP_TYPE), :projectId) ")
    @GetGeneratedKeys
    abstract int insertGeneratedId(@BindBean Group group, @Bind("projectId") Integer projectId);

    @SqlUpdate("INSERT INTO groups (id, name, type, project_id) " +
            "VALUES (:id, :name, CAST(:type AS GROUP_TYPE), :projectId) ")
    abstract void insertWitId(@BindBean Group group, @Bind("projectId") Integer projectId);

    @SqlQuery("SELECT groups.*, projects.name as projectName, projects.description as projectDescription FROM groups " +
            " LEFT JOIN projects on projects.id = groups.project_id " +
            " ORDER BY name LIMIT :it")
    @Mapper(GroupWithProjectMapper.class)
    public abstract List<Group> getWithLimit(@Bind int limit);

    @SqlQuery("SELECT groups.*, projects.name as projectName, projects.description as projectDescription FROM groups " +
            " LEFT JOIN projects on projects.id = groups.project_id ")
    @Mapper(GroupWithProjectMapper.class)
    public abstract List<Group> getAllUnordered();

    @SqlUpdate("DELETE FROM groups")
    @Override
    public abstract void clean();

    //    https://habrahabr.ru/post/264281/
    @SqlBatch("INSERT INTO groups (id, name, type, project_id) " +
            " VALUES (:id, :name, CAST(:type AS GROUP_TYPE), :projectId)" +
            " ON CONFLICT (name) DO NOTHING")
    protected abstract int[] insertBatchSkipDublicates(@BindBean List<Group> groups,
                                                       @Bind("projectId") List<Integer> projectIds,
                                                       @BatchChunkSize int chunkSize);

    public int[] insertBatchSkipDublicates(@BindBean List<Group> groups, @BatchChunkSize int chunkSize) {
        List<Integer> projectIds = new ArrayList<>(groups.size());
        for (Group group : groups) {
            projectIds.add(group.getProject().getId());
        }
        return insertBatchSkipDublicates(groups, projectIds, chunkSize);
    }

    public static class GroupWithProjectMapper implements ResultSetMapper<Group> {
        public Group map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            Map<Integer, Project> projectMap = (Map<Integer, Project>) ctx.getAttribute("projects");
            if (projectMap == null) {
                projectMap = new HashMap<>();
                ctx.setAttribute("cities", projectMap);
            }
            Project project = projectMap.get(rs.getInt("project_id"));
            if (project == null) {
                project = new Project(rs.getInt("project_id"), rs.getString("projectName"),
                        rs.getString("projectDescription"));
                projectMap.put(project.getId(), project);
            }

            Group g = new Group(rs.getInt("id"), rs.getString("name"),
                    GroupType.valueOf(rs.getString("type")),
                    project);
            return g;
        }
    }

}
