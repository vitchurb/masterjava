package ru.javaops.masterjava.xml.service;

import ru.javaops.masterjava.xml.schema.User;
import ru.javaops.masterjava.xml.util.JaxbParser;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by vit on 06.11.2017.
 */
public class XmlService {
    private static final Comparator<User> USER_COMPARATOR = Comparator.comparing(User::getValue).thenComparing(User::getEmail);

    public static Collection<User> processUsersByStax(JaxbParser jaxbParser,
                                                      InputStream is) throws XMLStreamException, JAXBException {
        StaxStreamProcessor processor = new StaxStreamProcessor(is);
        Set<User> users = new TreeSet<>(USER_COMPARATOR);

        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            users.add(jaxbParser.unmarshal(processor.getReader(), User.class));
        }
        return users;
    }
}
