package ru.javaops.masterjava.upload;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.GroupDao;
import ru.javaops.masterjava.persist.dao.ProjectDao;
import ru.javaops.masterjava.persist.model.Group;
import ru.javaops.masterjava.persist.model.Project;
import ru.javaops.masterjava.persist.model.type.GroupType;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vit on 26.11.2017.
 */
@Slf4j
public class ProjectProcessor {
    private final ProjectDao projectDao = DBIProvider.getDao(ProjectDao.class);
    private final GroupDao groupDao = DBIProvider.getDao(GroupDao.class);

    public void process(StaxStreamProcessor processor) throws XMLStreamException {
        val projectsMap = projectDao.getAsMap();
        val newProjectsMap = new HashMap<Project, List<Group>>();

        while (processor.startElement("Project", "Projects")) {
            val name = processor.getAttribute("name");
            String projectDescription = processor.getElementValue("description");
            if (!projectsMap.containsKey(name)) {
                List<Group> newGroups = new ArrayList<>();
                while (processor.startElement("Group", "Project")) {
                    String groupName = processor.getAttribute("name");
                    GroupType groupType = GroupType.valueOf(processor.getAttribute("type"));
                    Group group = new Group(groupName, groupType, 0);
                    newGroups.add(group);
                }
                newProjectsMap.put(new Project(name, projectDescription), newGroups);
            }
        }
        List<Group> groupsForInserting = new ArrayList<>();
        for (Map.Entry<Project, List<Group>> entry : newProjectsMap.entrySet()) {
            projectDao.insert(entry.getKey());
            int projectId = entry.getKey().getId();
            for (Group group : entry.getValue()) {
                group.setProjectId(projectId);
            }
            groupsForInserting.addAll(entry.getValue());
        }
        if (!groupsForInserting.isEmpty())
            groupDao.insertBatch(groupsForInserting);

        log.info("Insert projects: " + newProjectsMap.size());
    }

}
