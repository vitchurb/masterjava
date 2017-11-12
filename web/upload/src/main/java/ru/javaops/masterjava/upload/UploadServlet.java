package ru.javaops.masterjava.upload;

import org.thymeleaf.context.WebContext;
import ru.javaops.masterjava.upload.to.SaveUserResult;
import ru.javaops.masterjava.xml.util.JaxbParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static ru.javaops.masterjava.common.web.ThymeleafListener.engine;

@WebServlet(urlPatterns = "/", loadOnStartup = 1)
@MultipartConfig(maxFileSize = 100_000_000,
        maxRequestSize = 100_000_000,
        fileSizeThreshold = 1_000_000)
public class UploadServlet extends HttpServlet {

    private final UserProcessor userProcessor = new UserProcessor();
    private static final JaxbParser JAXB_PARSER = new JaxbParser(ru.javaops.masterjava.xml.schema.User.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final WebContext webContext = new WebContext(req, resp, req.getServletContext(), req.getLocale());
        engine.process("upload", webContext, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final WebContext webContext = new WebContext(req, resp, req.getServletContext(), req.getLocale());

        try {
            Integer batchSizeInt = null;
            try {
                batchSizeInt = Integer.parseInt(req.getParameter("batchSize"));
                if (batchSizeInt < 1)
                    batchSizeInt = null;
            } catch (NumberFormatException ignored) {
            }
            if (batchSizeInt == null)
                throw new IllegalStateException("Batch size have not been specified");

//            http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
            Part filePart = req.getPart("fileToUpload");
            if (filePart.getSize() == 0) {
                throw new IllegalStateException("Upload file have not been selected");
            }
            try (InputStream is = filePart.getInputStream()) {
                List<SaveUserResult> usersResult = userProcessor.process(JAXB_PARSER, is, batchSizeInt);
                webContext.setVariable("users", usersResult);
                engine.process("result", webContext, resp.getWriter());
            }
        } catch (Exception e) {
            webContext.setVariable("exception", e);
            engine.process("exception", webContext, resp.getWriter());
        }
    }
}
