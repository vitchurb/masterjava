package ru.javaops.masterjava.webapp;

import com.google.common.collect.ImmutableMap;
import org.thymeleaf.context.WebContext;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.service.mail.Addressee;
import ru.javaops.masterjava.service.mail.MailWSClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.javaops.masterjava.common.web.ThymeleafListener.engine;

@WebServlet("")
public class UsersServlet extends HttpServlet {
    private UserDao userDao = DBIProvider.getDao(UserDao.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final WebContext webContext = new WebContext(req, resp, req.getServletContext(), req.getLocale(),
                ImmutableMap.of("users", userDao.getWithLimit(20)));
        engine.process("users", webContext, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Integer> usersIds = new HashSet<>();
        for (String key : req.getParameterMap().keySet()) {
            if (key.startsWith("user_")) {
                usersIds.add(Integer.parseInt(key.substring(5)));
            }
        }
        String sentResult = "Error: no users to send email";
        if (!usersIds.isEmpty()) {
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            List<User> users = userDao.getByIds(usersIds);
            Set<Addressee> addressees = users.stream()
                    .map(user -> new Addressee(user.getEmail(), user.getFullName()))
                    .collect(Collectors.toSet());
            sentResult = MailWSClient.sendToGroup(addressees, null, subject, body);
        }
        final WebContext webContext = new WebContext(req, resp, req.getServletContext(), req.getLocale(),
                ImmutableMap.of("sentResult", sentResult));
        engine.process("result", webContext, resp.getWriter());
    }
}
