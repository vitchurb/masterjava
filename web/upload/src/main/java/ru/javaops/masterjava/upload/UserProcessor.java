package ru.javaops.masterjava.upload;

import org.slf4j.Logger;
import ru.javaops.masterjava.common.threadpool.ThreadPoolExecutorGrowing;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;
import ru.javaops.masterjava.upload.to.SaveUserResult;
import ru.javaops.masterjava.xml.util.JaxbParser;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class UserProcessor {
    private static final Logger log = getLogger(UserProcessor.class);

    private static ThreadPoolExecutor executor =
            new ThreadPoolExecutorGrowing(0, 30, 10, TimeUnit.SECONDS);


    public List<SaveUserResult> process(final JaxbParser jaxbParser,
                                        final InputStream is,
                                        final int batchSize) throws XMLStreamException, JAXBException {
        final StaxStreamProcessor processor = new StaxStreamProcessor(is);
        final ConcurrentMap<Integer, List<SaveUserResult>> usersWithResultsByBatchNum = new ConcurrentSkipListMap<>();

        List<User> usersForBatch = new ArrayList<>();

        UserDao dao = DBIProvider.getDao(UserDao.class);

        ExecutorCompletionService completionService = new ExecutorCompletionService<Boolean>(executor);
        int countTasks = 0;
        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            ru.javaops.masterjava.xml.schema.User userXml = jaxbParser.unmarshal(processor.getReader(), ru.javaops.masterjava.xml.schema.User.class);
            final User user = new User(userXml.getValue(), userXml.getEmail(), UserFlag.valueOf(userXml.getFlag().value()));

            usersForBatch.add(user);
            if (usersForBatch.size() >= batchSize) {
                insertUsers(completionService, dao, countTasks, usersForBatch);
                countTasks++;
                usersForBatch = new ArrayList<>();
            }
        }
        if (!usersForBatch.isEmpty()) {
            insertUsers(completionService, dao, countTasks, usersForBatch);
            countTasks++;
        }

        for (int i = 0; i < countTasks; i++) {
            try {
                InsertingResultsWithNum usersResult =
                        (InsertingResultsWithNum) completionService.take().get();
                usersWithResultsByBatchNum.put(usersResult.getNum(), usersResult.getResults());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return usersWithResultsByBatchNum.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private void insertUsers(CompletionService completionService,
                             UserDao userDao,
                             final int numBatch, final List<User> usersForBatch) {
        completionService.submit(() -> {
            try {
                List<SaveUserResult> resultsInserting = new ArrayList<>();

                DBIProvider.getDBI().useTransaction((conn, status) -> {
                    int[] newIds = userDao.insertNewBatchNoConflictEmail(usersForBatch);
                    List<String> emails = usersForBatch.stream()
                            .map(User::getEmail)
                            .collect(Collectors.toList());
                    List<String> emailAlreadyInDatabase;
                    if (newIds.length > 0)
                        emailAlreadyInDatabase = userDao.getEmailByEmailsNotInIds(emails, newIds);
                    else
                        emailAlreadyInDatabase = userDao.getEmailByEmails(emails);
                    Set<String> emailAlreadyInDatabaseSet = new HashSet(emailAlreadyInDatabase);

                    for (User user : usersForBatch) {
                        if (emailAlreadyInDatabaseSet.contains(user.getEmail())) {
                            resultsInserting.add(new SaveUserResult(user, SaveUserResult.Result.EMAIL_ALREADY_EXISTS, null));
                        }
                    }
                });
                return new InsertingResultsWithNum(numBatch, resultsInserting);
            } catch (Exception e) {
                log.error("Error inserting new users chunk:", e);
                //log, set all records as error
                List<SaveUserResult> resultsInserting = new ArrayList<>();
                for (User user : usersForBatch) {
                    resultsInserting.add(new SaveUserResult(user, SaveUserResult.Result.EXCEPTION,
                            e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                }
                return new InsertingResultsWithNum(numBatch, resultsInserting);
            }
        });


    }

    private static class InsertingResultsWithNum {
        final int num; //number of batch;
        final List<SaveUserResult> results;

        InsertingResultsWithNum(int num, List<SaveUserResult> results) {
            this.num = num;
            this.results = results;
        }

        int getNum() {
            return num;
        }

        List<SaveUserResult> getResults() {
            return results;
        }
    }
}
