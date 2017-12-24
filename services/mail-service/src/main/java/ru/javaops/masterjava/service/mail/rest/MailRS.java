package ru.javaops.masterjava.service.mail.rest;


import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;
import ru.javaops.masterjava.ExceptionType;
import ru.javaops.masterjava.service.mail.GroupResult;
import ru.javaops.masterjava.service.mail.MailServiceExecutor;
import ru.javaops.masterjava.service.mail.MailWSClient;
import ru.javaops.masterjava.service.mail.util.Attachments;
import ru.javaops.masterjava.web.WebStateException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Path("/")
public class MailRS {
    private static final int MAX_FILE_SIZE = 10_000_000;

    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "Test";
    }

    @POST
    @Path("send")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public GroupResult send(
            @Context final HttpServletRequest request,
            @FormDataParam("attach") InputStream uploadedInputStream,
            @FormDataParam("attach") FormDataContentDisposition fileDetail,
            @NotBlank @FormDataParam("users") String users,
            @FormDataParam("subject") String subject,
            @NotBlank @FormDataParam("body") String body) throws WebStateException {
        //https://stackoverflow.com/questions/45931762/valid-bean-validation-not-working-in-jersey
        try {
            request.setCharacterEncoding("UTF-8");
            if (request.getContentLength() < 0 || request.getContentLength() > MAX_FILE_SIZE)
                throw new WebStateException("TOO LARGE FILE", ExceptionType.ATTACHMENT);
            boolean hasAttach = fileDetail != null && uploadedInputStream != null;
            String fileName = null;
            byte[] bytes = null;
            if (hasAttach) {
                fileName = new String(fileDetail.getFileName().getBytes("iso-8859-1"), "UTF-8");
                bytes = IOUtils.toByteArray(uploadedInputStream);
            }

            return MailServiceExecutor.sendBulk(MailWSClient.split(users), subject, body,
                    !hasAttach ? Collections.EMPTY_LIST :
                            ImmutableList.of(Attachments.getAttachment(fileName, new ByteArrayInputStream(bytes))));
        } catch (IOException e) {
            throw new WebStateException(e, ExceptionType.ATTACHMENT);
        }
    }
}