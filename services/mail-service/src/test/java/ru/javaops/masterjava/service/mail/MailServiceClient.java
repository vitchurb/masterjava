package ru.javaops.masterjava.service.mail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.event.Level;
import ru.javaops.masterjava.web.WebStateException;
import ru.javaops.masterjava.web.handler.SoapLoggingConfigurableHandlers;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class MailServiceClient {

    public static void main(String[] args) throws MalformedURLException, WebStateException {
        Service service = Service.create(
                new URL("http://localhost:8080/mail/mailService?wsdl"),
                new QName("http://mail.javaops.ru/", "MailServiceImplService"));

        List<Attachment> attachments = ImmutableList.of(
                new Attachment("version.html", new DataHandler(new File("config_templates/version.html").toURI().toURL())));

        MailService mailService = service.getPort(MailService.class);
        Map<String, Object> requestContext = ((BindingProvider) mailService).getRequestContext();
        requestContext.put(BindingProvider.USERNAME_PROPERTY, "user");
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, "password");
        Binding binding = ((BindingProvider) mailService).getBinding();
        List<Handler> handlerList = binding.getHandlerChain();
        handlerList.add(new SoapLoggingConfigurableHandlers.ClientHandler(Level.DEBUG));
        binding.setHandlerChain(handlerList);

        String state = mailService.sendToGroup(ImmutableSet.of(new Addressee("masterjava@javaops.ru", null)), null,
                "Group mail subject", "Group mail body", attachments);
        System.out.println("Group mail state: " + state);

        GroupResult groupResult = mailService.sendBulk(ImmutableSet.of(
                new Addressee("Мастер Java <masterjava@javaops.ru>"),
                new Addressee("Bad Email <bad_email.ru>")), "Bulk mail subject", "Bulk mail body", attachments);
        System.out.println("\nBulk mail groupResult:\n" + groupResult);
    }
}
