package ru.javaops.masterjava.service.mail;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.web.WebStateException;

import javax.activation.DataHandler;
import java.net.MalformedURLException;
import java.nio.file.Paths;

@Slf4j
public class MailWSClientMain {
    public static void main(String[] args) throws WebStateException, MalformedURLException {
        String state = MailWSClient.sendToGroup(
                ImmutableSet.of(new Addressee("To <masterjava@javaops.ru>")),
                ImmutableSet.of(new Addressee("Copy <masterjava@javaops.ru>")), "Subject", "Body555",
                new Attachment("filenma1.xml", new DataHandler(
                        Paths.get("d:\\_payload.xml").toUri().toURL())));

        System.out.println(state);
    }
}