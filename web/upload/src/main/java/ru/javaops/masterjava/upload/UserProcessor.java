package ru.javaops.masterjava.upload;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.CityDao;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.City;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.types.UserFlag;
import ru.javaops.masterjava.xml.schema.ObjectFactory;
import ru.javaops.masterjava.xml.util.JaxbParser;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
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
    private static CityDao cityDao = DBIProvider.getDao(CityDao.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);

    @AllArgsConstructor
    public static class FailedEmails {
        public String emailsOrRange;
        public String reason;

        @Override
        public String toString() {
            return emailsOrRange + " : " + reason;
        }
    }

    /*
     * return failed users chunks
     */
    public List<FailedEmails> process(final InputStream is, int chunkSize) throws XMLStreamException, JAXBException {
        log.info("Start processing with chunkSize=" + chunkSize);

        Map<String, Future<List<String>>> chunkFutures = new LinkedHashMap<>();  // ordered map (emailRange -> chunk future)
        Set<Future<List<String>>> chunkCityFutures = new HashSet<>();

        val processor = new StaxStreamProcessor(is);
        val unmarshaller = jaxbParser.createUnmarshaller();

        int idCity = cityDao.getSeqAndSkip(chunkSize);
        List<City> chunkCity = new ArrayList<>();

        Map<String, City> citiesMap = new HashMap<>();
        while (processor.startElement("City", "Cities")) {
            ru.javaops.masterjava.xml.schema.CityType xmlCity = unmarshaller.unmarshal(processor.getReader(), ru.javaops.masterjava.xml.schema.CityType.class);
            final City city = new City(idCity++, xmlCity.getId(), xmlCity.getValue());
            chunkCity.add(city);
            if (chunkCity.size() == chunkSize) {
                addChunkCitiesFutures(chunkCityFutures, chunkCity);
                chunkCity = new ArrayList<>(chunkSize);
                idCity = userDao.getSeqAndSkip(chunkSize);
            }

            citiesMap.put(city.getCode(), city);
        }
        if (!chunkCity.isEmpty()) {
            addChunkCitiesFutures(chunkCityFutures, chunkCity);
        }
        //Коды городов, только что добавленных в БД
        //Entity для них надо будет брать из citiesMap
        Set<String> justInsertedToDbCodesSet = new HashSet<>();
        chunkCityFutures.forEach((future) -> {
            try {
                List<String> insertedCodes = future.get();
                justInsertedToDbCodesSet.addAll(insertedCodes);
            } catch (InterruptedException | ExecutionException e) {
                log.error("failed adding cities", e);
            }
        });

        //коды городов, которые уже были в БД. Для них надо получить id
        List<String> codesToRequestFromDb = citiesMap.keySet()
                .stream()
                .filter(c -> !justInsertedToDbCodesSet.contains(c))
                .collect(Collectors.toList());
        List<City> citiesFromDb = codesToRequestFromDb.isEmpty() ?
                Collections.EMPTY_LIST :
                cityDao.getByCodesUnordered(codesToRequestFromDb);
        Map<String, City> citiesFromDbMap = citiesFromDb
                .stream()
                .collect(Collectors.toMap(City::getCode, c -> c));

        int id = userDao.getSeqAndSkip(chunkSize);
        List<User> chunk = new ArrayList<>(chunkSize);
        List<FailedEmails> failed = new ArrayList<>();

        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            String active = processor.getAttribute("flag");
            String cityCode = processor.getAttribute("city");
            String email = processor.getAttribute("email");
            String name = processor.getValue(XMLEvent.START_ELEMENT);
            City city = null;
            if (justInsertedToDbCodesSet.contains(cityCode))
                city = citiesMap.get(cityCode);
            else
                city = citiesFromDbMap.get(cityCode);
            if (city == null) {
                failed.add(new FailedEmails(email, "city not found"));
            } else {
                final User user = new User(id++, name, email, UserFlag.valueOf(active), city);
                chunk.add(user);
                if (chunk.size() == chunkSize) {
                    addChunkFutures(chunkFutures, chunk);
                    chunk = new ArrayList<>(chunkSize);
                    id = userDao.getSeqAndSkip(chunkSize);
                }
            }
        }

        if (!chunk.isEmpty()) {
            addChunkFutures(chunkFutures, chunk);
        }

        List<String> allAlreadyPresents = new ArrayList<>();
        chunkFutures.forEach((emailRange, future) -> {
            try {
                List<String> alreadyPresentsInChunk = future.get();
                log.info("{} successfully executed with already presents: {}", emailRange, alreadyPresentsInChunk);
                allAlreadyPresents.addAll(alreadyPresentsInChunk);
            } catch (InterruptedException | ExecutionException e) {
                log.error(emailRange + " failed", e);
                failed.add(new FailedEmails(emailRange, e.toString()));
            }
        });
        if (!allAlreadyPresents.isEmpty()) {
            failed.add(new FailedEmails(allAlreadyPresents.toString(), "already presents"));
        }
        return failed;
    }

    private void addChunkFutures(Map<String, Future<List<String>>> chunkFutures, List<User> chunk) {
        String emailRange = String.format("[%s-%s]", chunk.get(0).getEmail(), chunk.get(chunk.size() - 1).getEmail());
        Future<List<String>> future = executorService.submit(() -> userDao.insertAndGetConflictEmails(chunk));
        chunkFutures.put(emailRange, future);
        log.info("Submit chunk: " + emailRange);
    }

    private void addChunkCitiesFutures(Set<Future<List<String>>> chunkFutures, List<City> chunk) {
        Future<List<String>> future = executorService.submit(() -> cityDao.insertBatchGetInsertedCodes(chunk));
        chunkFutures.add(future);
    }
}
