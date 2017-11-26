package ru.javaops.masterjava.upload;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.thymeleaf.util.StringUtils;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.GroupDao;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.dao.UserGroupDao;
import ru.javaops.masterjava.persist.model.City;
import ru.javaops.masterjava.persist.model.Group;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserGroup;
import ru.javaops.masterjava.persist.model.type.UserFlag;
import ru.javaops.masterjava.upload.PayloadProcessor.FailedEmails;
import ru.javaops.masterjava.xml.schema.ObjectFactory;
import ru.javaops.masterjava.xml.util.JaxbParser;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class UserProcessor {
    private static final int NUMBER_THREADS = 4;

    private static final JaxbParser jaxbParser = new JaxbParser(ObjectFactory.class);
    private static UserDao userDao = DBIProvider.getDao(UserDao.class);
    private static GroupDao groupDao = DBIProvider.getDao(GroupDao.class);
    private static UserGroupDao userGroupDao = DBIProvider.getDao(UserGroupDao.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);

    /*
     * return failed users chunks
     */
    public List<FailedEmails> process(final StaxStreamProcessor processor, Map<String, City> cities, int chunkSize) throws XMLStreamException, JAXBException {
        log.info("Start processing with chunkSize=" + chunkSize);

        Map<List<User>, Future<List<String>>> chunkFutures = new LinkedHashMap<>();  // ordered map (users -> chunk future)
        List<Future> chunkGroupsFutures = new ArrayList<>();


        int id = userDao.getSeqAndSkip(chunkSize);
        List<User> chunk = new ArrayList<>(chunkSize);
        val unmarshaller = jaxbParser.createUnmarshaller();
        List<FailedEmails> failed = new ArrayList<>();

        Map<User, Set<Integer>> groupsRefsByUsers = new HashMap<>();
        Map<String, Group> groupMap = groupDao.getAsMap();

        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            String cityRef = processor.getAttribute("city");  // unmarshal doesn't get city ref
            String groupsRef = processor.getAttribute("groupRefs");
            ru.javaops.masterjava.xml.schema.User xmlUser = unmarshaller.unmarshal(processor.getReader(), ru.javaops.masterjava.xml.schema.User.class);

            if (cities.get(cityRef) == null) {
                failed.add(new FailedEmails(xmlUser.getEmail(), "City '" + cityRef + "' is not present in DB"));
            } else {
                final User user = new User(id++, xmlUser.getValue(), xmlUser.getEmail(), UserFlag.valueOf(xmlUser.getFlag().value()), cityRef);
                String errorGroup = null;
                Set<Integer> groupIds = new HashSet<>();
                String[] groupRefsArray = StringUtils.isEmpty(groupsRef) ?
                        new String[0] : groupsRef.split(" ");
                for (String groupRef : groupRefsArray) {
                    Group group = groupMap.get(groupRef);
                    if (group == null) {
                        errorGroup = groupRef;
                        break;
                    } else {
                        groupIds.add(group.getId());
                    }
                }
                if (errorGroup != null) {
                    failed.add(new FailedEmails(xmlUser.getEmail(), "Group '" + errorGroup + "' is not present in DB"));
                } else {
                    groupsRefsByUsers.put(user, groupIds);
                    chunk.add(user);
                    if (chunk.size() == chunkSize) {
                        addChunkFutures(chunkFutures, chunk);
                        chunk = new ArrayList<>(chunkSize);
                        id = userDao.getSeqAndSkip(chunkSize);
                    }
                }
            }
        }

        if (!chunk.isEmpty()) {
            addChunkFutures(chunkFutures, chunk);
        }

        List<String> allAlreadyPresents = new ArrayList<>();
        Set<String> emailsWithoutErrors = new HashSet<>();

        chunkFutures.forEach((chunkItem, future) -> {
            try {
                List<String> alreadyPresentsInChunk = future.get();
                log.info("{} successfully executed with already presents: {}", getEmailRange(chunkItem), alreadyPresentsInChunk);
                allAlreadyPresents.addAll(alreadyPresentsInChunk);
                Set<String> alreadyPresentsInChunkSet = new HashSet<>(alreadyPresentsInChunk);

                Set<String> emailsFromChunkWithoutErrors = chunkItem.stream()
                        .map(User::getEmail)
                        .filter(email -> !alreadyPresentsInChunkSet.contains(email))
                        .collect(Collectors.toSet());

                emailsWithoutErrors.addAll(emailsFromChunkWithoutErrors);
            } catch (InterruptedException | ExecutionException e) {
                log.error(getEmailRange(chunkItem) + " failed", e);
                failed.add(new FailedEmails(getEmailRange(chunkItem), e.toString()));
            }
        });
        if (!allAlreadyPresents.isEmpty()) {
            failed.add(new FailedEmails(allAlreadyPresents.toString(), "already presents"));
        }

        List<UserGroup> userGroupChunk = new ArrayList<>();
        for (Map.Entry<User, Set<Integer>> entry : groupsRefsByUsers.entrySet()) {
            User user = entry.getKey();
            if (emailsWithoutErrors.contains(user.getEmail())) {
                for (Integer ref : entry.getValue()) {
                    UserGroup userGroup = new UserGroup(user.getId(), ref);
                    userGroupChunk.add(userGroup);
                    if (userGroupChunk.size() == chunkSize) {
                        addChunkGroupFutures(chunkGroupsFutures, userGroupChunk);
                        userGroupChunk = new ArrayList<>(chunkSize);
                    }
                }
            }
        }

        if (!userGroupChunk.isEmpty()) {
            addChunkGroupFutures(chunkGroupsFutures, userGroupChunk);
        }

        //Process results of inserting groups
        chunkGroupsFutures.forEach((future) -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Adding groups for users failed", e);
                throw new RuntimeException("Error adding groups for users");
            }
        });
        return failed;
    }

    private void addChunkFutures(Map<List<User>, Future<List<String>>> chunkFutures, List<User> chunk) {
        Future<List<String>> future = executorService.submit(() -> userDao.insertAndGetConflictEmails(chunk));
        chunkFutures.put(chunk, future);
        log.info("Submit chunk: " + getEmailRange(chunk));
    }

    private void addChunkGroupFutures(List<Future> chunkGroupsFutures, List<UserGroup> chunk) {
        Future future = executorService.submit(() -> userGroupDao.insertBatch(chunk));
        chunkGroupsFutures.add(future);
        log.info("Submit chunk groups");
    }

    private String getEmailRange(List<User> chunk) {
        return String.format("[%s-%s]", chunk.get(0).getEmail(), chunk.get(chunk.size() - 1).getEmail());
    }
}
