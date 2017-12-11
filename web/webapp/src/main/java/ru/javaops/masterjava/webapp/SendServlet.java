package ru.javaops.masterjava.webapp;

import com.google.common.io.ByteStreams;
import com.sun.xml.ws.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.service.mail.Attachment;
import ru.javaops.masterjava.service.mail.GroupResult;
import ru.javaops.masterjava.service.mail.MailWSClient;

import javax.activation.DataHandler;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

@WebServlet("/send")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10) //10 MB in memory limit
@Slf4j
public class SendServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String result;
        try {
            Part filePart = req.getPart("file");
            byte[] fileContent = null;
            String filename = null;
            if (filePart != null) {
                filename = filePart.getSubmittedFileName();
                try (InputStream inputStream = filePart.getInputStream()) {
                    fileContent = ByteStreams.toByteArray(inputStream);
                }
            }
            log.info("Start sending");
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            String users = req.getParameter("users");
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            GroupResult groupResult = MailWSClient.sendBulk(MailWSClient.split(users), subject, body,
                    filePart == null ? null :
                            new Attachment(filename,
                                    new DataHandler(new ByteArrayDataSource(fileContent, filePart.getContentType()))));
            result = groupResult.toString();
            log.info("Processing finished with result: {}", result);
        } catch (Exception e) {
            log.error("Processing failed", e);
            result = e.toString();
        }
        resp.getWriter().write(result);
    }
}
