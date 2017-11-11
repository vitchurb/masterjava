package ru.javaops.masterjava.web;

import org.thymeleaf.context.WebContext;
import ru.javaops.masterjava.web.utils.ThymeleafAppUtil;
import ru.javaops.masterjava.xml.schema.User;
import ru.javaops.masterjava.xml.service.XmlService;
import ru.javaops.masterjava.xml.util.JaxbParser;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

/**
 * Created by vit on 05.11.2017.
 */
@MultipartConfig(maxFileSize = 10_000_000,
        maxRequestSize = 10_000_000,
        fileSizeThreshold = 1_000_000)
@WebServlet(name = "UploadServlet", urlPatterns = {"/upload", "/"})
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final JaxbParser JAXB_PARSER = new JaxbParser(User.class);


    @Override
    public void init() {
        ServletContext context = getServletContext();
        ThymeleafAppUtil.init(context);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Part xmlPart = request.getPart("xmlfile");
        if (xmlPart == null || xmlPart.getSize() == 0) {
            response.sendRedirect("upload");
        } else {
            try (InputStream is = xmlPart.getInputStream()) {
                Collection<User> users = XmlService.processUsersByStax(JAXB_PARSER, is);
                process(request, response, users);
            } catch (XMLStreamException | JAXBException e) {
                throw new ServletException(e);
            }
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        process(request, response, null);
    }

    public void process(HttpServletRequest request, HttpServletResponse response, Collection<User> users)
            throws IOException {
        WebContext ctx = new WebContext(request, response, request.getServletContext(),
                request.getLocale());
        ctx.setVariable("users", users);
        ctx.setVariable("currentDate", new Date());
        ThymeleafAppUtil.getTemplateEngine().process("upload", ctx, response.getWriter());
    }


}