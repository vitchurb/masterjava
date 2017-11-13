package ru.javaops.masterjava.upload;

import org.slf4j.Logger;
import ru.javaops.masterjava.common.threadpool.ThreadPoolExecutorGrowing;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;
import ru.javaops.masterjava.upload.to.SaveChunkError;
import ru.javaops.masterjava.upload.to.SaveUserResult;
import ru.javaops.masterjava.upload.to.SavingResult;
import ru.javaops.masterjava.xml.util.JaxbParser;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class UserProcessor {
    private static final Logger log = getLogger(UserProcessor.class);

    private static final JaxbParser JAXB_PARSER = new JaxbParser(ru.javaops.masterjava.xml.schema.User.class);

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutorGrowing(0, 30, 10, TimeUnit.SECONDS);


    public SavingResult process(final InputStream is,
                                final int batchSize) throws XMLStreamException, JAXBException {
        final StaxStreamProcessor processor = new StaxStreamProcessor(is);
        //информация о проблемах с отдельными пользователями
        final Map<Integer, List<SaveUserResult>> usersWithResultsByBatchNum = new TreeMap<>();
        //информация о проблемах с целыми Batch
        final Map<Integer, SaveChunkError> chunksWithErrorsByBatchNum = new TreeMap<>();

        final HashMap<Future<InsertingResultsWithNum>, ChunkInfo> futures = new HashMap<>();

        List<User> usersForBatch = new ArrayList<>();

        UserDao dao = DBIProvider.getDao(UserDao.class);

        ExecutorCompletionService<InsertingResultsWithNum> completionService = new ExecutorCompletionService<>(executor);
        int countTasks = 0;
        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            ru.javaops.masterjava.xml.schema.User userXml = JAXB_PARSER.unmarshal(processor.getReader(), ru.javaops.masterjava.xml.schema.User.class);
            final User user = new User(userXml.getValue(), userXml.getEmail(), UserFlag.valueOf(userXml.getFlag().value()));

            usersForBatch.add(user);
            if (usersForBatch.size() >= batchSize) {
                futures.put(insertUsers(completionService, dao, countTasks, usersForBatch),
                        new ChunkInfo(countTasks, usersForBatch.get(0).getEmail(),
                                usersForBatch.get(usersForBatch.size() - 1).getEmail()));
                countTasks++;
                usersForBatch = new ArrayList<>();
            }
        }
        if (!usersForBatch.isEmpty()) {
            futures.put(insertUsers(completionService, dao, countTasks, usersForBatch),
                    new ChunkInfo(countTasks, usersForBatch.get(0).getEmail(),
                            usersForBatch.get(usersForBatch.size() - 1).getEmail()));
            countTasks++;
        }

        for (int i = 0; i < countTasks; i++) {
            Future<InsertingResultsWithNum> future = null;
            try {
                future = completionService.take();
                InsertingResultsWithNum usersResult = future.get();
                usersWithResultsByBatchNum.put(usersResult.getNum(), usersResult.getResults());
            } catch (Exception e) {
                ChunkInfo chunkInfo = null;
                if (future != null)
                    chunkInfo = futures.get(future);
                log.error("Error inserting new users chunk:", e);
                if (chunkInfo != null) { //Exception inside future.get()
                    chunksWithErrorsByBatchNum.put(chunkInfo.getNum(),
                            new SaveChunkError(chunkInfo.getFirstEmail(), chunkInfo.getLastEmail(),
                                    SaveUserResult.Result.EXCEPTION,
                                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                } else { //Exception inside completionService
                    throw new RuntimeException(e);
                }
            }
        }
        return new SavingResult(usersWithResultsByBatchNum.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()),
                chunksWithErrorsByBatchNum.values());
    }

    private Future<InsertingResultsWithNum> insertUsers(CompletionService<InsertingResultsWithNum> completionService,
                                                        UserDao userDao,
                                                        final int numBatch, final List<User> usersForBatch) {
        return completionService.submit(() -> {
            List<SaveUserResult> resultsInserting = new ArrayList<>();

            DBIProvider.getDBI().useTransaction((conn, status) -> {
                int[] newIds = userDao.insertNewBatchNoConflictEmail(usersForBatch);
                List<String> emails = usersForBatch.stream()
                        .map(User::getEmail)
                        .collect(Collectors.toList());
                List<String> emailAlreadyInDatabase;
                if (newIds.length != usersForBatch.size()) {
                    if (newIds.length > 0)
                        emailAlreadyInDatabase = userDao.getEmailByEmailsNotInIds(emails, newIds);
                    else
                        emailAlreadyInDatabase = userDao.getEmailByEmails(emails);
                    Set<String> emailAlreadyInDatabaseSet = new HashSet<>(emailAlreadyInDatabase);

                    for (User user : usersForBatch) {
                        if (emailAlreadyInDatabaseSet.contains(user.getEmail())) {
                            resultsInserting.add(new SaveUserResult(user, SaveUserResult.Result.EMAIL_ALREADY_EXISTS, null));
                        }
                    }
                }
            });
            return new InsertingResultsWithNum(numBatch, resultsInserting);
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

    private static class ChunkInfo {
        final int num; //number of batch;
        final String firstEmail;
        final String lastEmail;

        ChunkInfo(int num, String firstEmail, String lastEmail) {
            this.num = num;
            this.firstEmail = firstEmail;
            this.lastEmail = lastEmail;
        }

        public int getNum() {
            return num;
        }

        public String getFirstEmail() {
            return firstEmail;
        }

        public String getLastEmail() {
            return lastEmail;
        }
    }
}
