package ru.javaops.masterjava.xml;

import org.xml.sax.helpers.AttributesImpl;
import ru.javaops.masterjava.xml.schema.*;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by vch on 28.10.2017.
 * Реализовать класс  MainXml , которые принимает параметром имя проекта в тестовом xml и
 * выводит отсортированный список его участников (использовать JAXB).
 */
public class MainXml {

    public static List<UserFullNameEmail> getUsersForProject(Payload payload, String projectName) throws IOException, JAXBException, org.xml.sax.SAXException {
        List<UserFullNameEmail> usersList = new ArrayList<>();
        for (User user : payload.getUsers().getUser()) {
            for (GroupForUser groupForUser : user.getGroupsForUser().getGroupForUser()) {
                if (projectName.equals(((ProjectType) ((GroupType) groupForUser.getGroup()).getProject()).getValue())) {
                    usersList.add(new UserFullNameEmail(user.getFullName(), user.getEmail()));
                    break;
                }
            }
        }
        Collections.sort(usersList);
        return new ArrayList<>(usersList);
    }


    public static List<UserFullNameEmail> getUsersForProjectStax(InputStream is, String projectWantedName) throws XMLStreamException {
        if (projectWantedName == null)
            return Collections.emptyList();
        try (StaxStreamProcessor processor = new StaxStreamProcessor(is)) {
            XMLStreamReader reader = processor.getReader();
            String projectIdForWantedProject = null;
            List<UserInformation> usersList = new ArrayList<>();

            Map<String, String> projectIdByGroupId = new HashMap<>();
            boolean wasStartedUser = false;
            boolean wasStartedGroupsForUser = false;
            String currentUserEmail = null;
            String currentUserName = null;
            Set<String> groupsForCurrentUser = null;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLEvent.START_ELEMENT) {
                    String startElementName = reader.getLocalName();
                    if (startElementName == null)
                        continue;
                    switch (startElementName) {
                        case "User":
                            currentUserName = null;
                            groupsForCurrentUser = new HashSet<>();
                            wasStartedGroupsForUser = false;
                            wasStartedUser = true;
                            currentUserEmail = reader.getAttributeValue(null, "email");
                            break;
                        case "GroupsForUser":
                            wasStartedGroupsForUser = true;
                            break;
                        case "fullName":
                            currentUserName = reader.getElementText();
                            break;
                        case "GroupForUser":
                            if (wasStartedGroupsForUser) {
                                groupsForCurrentUser.add(reader.getAttributeValue(null, "group"));
                            }
                            break;
                        case "Group":
                            projectIdByGroupId.put(
                                    reader.getAttributeValue(null, "id"),
                                    reader.getAttributeValue(null, "project"));
                            break;
                        case "Project":
                            String projectId = reader.getAttributeValue(null, "id");
                            String projectName = reader.getElementText();
                            if (projectWantedName.equals(projectName)) {
                                projectIdForWantedProject = projectId;
                            }
                            break;
                    }
                } else if (event == XMLEvent.END_ELEMENT && wasStartedUser) {
                    String elementName = reader.getLocalName();
                    if (elementName == null)
                        continue;
                    switch (elementName) {
                        case "User":
                            wasStartedUser = false;
                            usersList.add(new UserInformation(currentUserName, currentUserEmail, groupsForCurrentUser));
                            groupsForCurrentUser = null;
                            break;
                        case "GroupsForUser":
                            wasStartedGroupsForUser = false;
                            break;
                    }
                }

            }
            //Конец обработки файла
            List<UserFullNameEmail> resultUserList = new ArrayList<>();
            for (UserInformation userInformation : usersList) {
                for (String groupId : userInformation.groupsIds) {
                    String projectIdForGroup = projectIdByGroupId.get(groupId);
                    if (projectIdForGroup != null && projectIdForGroup.equals(projectIdForWantedProject)) {
                        resultUserList.add(new UserFullNameEmail(userInformation.fullname, userInformation.email));
                        break;
                    }
                }
            }
            Collections.sort(resultUserList);
            return resultUserList;
        }
    }

    public static void writeAllUsersToHtml(Payload payload, OutputStream op)
            throws TransformerConfigurationException, XMLStreamException,
            IOException, org.xml.sax.SAXException {
        String encoding = "UTF-8";
        OutputStreamWriter writer = new OutputStreamWriter(op, encoding);
        StreamResult streamResult = new StreamResult(writer);

        SAXTransformerFactory saxFactory =
                (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler tHandler = saxFactory.newTransformerHandler();
        tHandler.setResult(streamResult);

        Transformer transformer = tHandler.getTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        writer.write("<!DOCTYPE html>\n");
        writer.flush();
        tHandler.startDocument();
        tHandler.startElement("", "", "html", new AttributesImpl());
        tHandler.startElement("", "", "head", new AttributesImpl());
        tHandler.startElement("", "", "title", new AttributesImpl());
        tHandler.characters("Users".toCharArray(), 0, "Users".length());
        tHandler.endElement("", "", "title");
        tHandler.endElement("", "", "head");
        tHandler.startElement("", "", "body", new AttributesImpl());
        AttributesImpl atr = new AttributesImpl();
        atr.addAttribute(null, "border", "border", null, "1");
        tHandler.startElement("", "", "table", atr);
        tHandler.startElement("", "", "tr", new AttributesImpl());
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("User name".toCharArray(), 0, "User name".length());
        tHandler.endElement("", "", "td");
        tHandler.startElement("", "", "td", new AttributesImpl());
        tHandler.characters("E-mail".toCharArray(), 0, "E-mail".length());
        tHandler.endElement("", "", "td");
        tHandler.endElement("", "", "tr");
        for (User user : payload.getUsers().getUser()) {
            tHandler.startElement("", "", "tr", new AttributesImpl());
            tHandler.startElement("", "", "td", new AttributesImpl());
            if (user.getFullName() != null)
                tHandler.characters(user.getFullName().toCharArray(), 0, user.getFullName().length());
            tHandler.endElement("", "", "td");
            tHandler.startElement("", "", "td", new AttributesImpl());
            if (user.getEmail() != null)
                tHandler.characters(user.getEmail().toCharArray(), 0, user.getEmail().length());
            tHandler.endElement("", "", "td");
            tHandler.endElement("", "", "tr");
        }
        tHandler.endElement("", "", "table");
        tHandler.endElement("", "", "body");
        tHandler.endElement("", "", "html");
        tHandler.endDocument();
        writer.close();

    }

    private static class UserInformation {
        String fullname;
        String email;
        Set<String> groupsIds;

        private UserInformation(String fullname, String email, Set<String> groupsIds) {
            this.fullname = fullname;
            this.email = email;
            this.groupsIds = groupsIds;
        }
    }

    public static class UserFullNameEmail implements Comparable<UserFullNameEmail> {
        private final String fullName;
        private final String email;

        public UserFullNameEmail(String fullName, String email) {
            this.fullName = fullName;
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public int compareTo(UserFullNameEmail that) {
            if (this == that)
                return 0;
            int res = 0;
            if (this.fullName == null && that.fullName != null) {
                res = -1;
            } else if (this.fullName != null && that.fullName == null) {
                res = 1;
            } else if (this.fullName != null) {
                res = this.fullName.compareTo(that.fullName);
            }
            if (res != 0)
                return res;
            if (this.email == null && that.email != null) {
                res = -1;
            } else if (this.email != null && that.email == null) {
                res = 1;
            } else if (this.email != null) {
                res = this.email.compareTo(that.email);
            }
            return res;
        }
    }


}
