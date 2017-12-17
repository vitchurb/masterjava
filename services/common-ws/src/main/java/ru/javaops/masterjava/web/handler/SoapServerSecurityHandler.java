package ru.javaops.masterjava.web.handler;

import com.sun.xml.ws.api.handler.MessageHandlerContext;
import ru.javaops.masterjava.web.AuthUtil;

import javax.xml.ws.handler.MessageContext;
import java.util.List;
import java.util.Map;

/**
 * Created by vit on 17.12.2017.
 */
public class SoapServerSecurityHandler extends SoapBaseHandler {
    @Override
    public boolean handleMessage(MessageHandlerContext context) {
        Map<String, List<String>> headers = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);

        int code = AuthUtil.checkBasicAuth(headers,
                AuthUtil.encodeBasicAuthHeader("user", "password"));
        if (code != 0) {
            context.put(MessageContext.HTTP_RESPONSE_CODE, code);
            throw new SecurityException();
        }

        return true;
    }

    @Override
    public boolean handleFault(MessageHandlerContext context) {
        return false;
    }

    public static class ServerHandler extends SoapServerSecurityHandler {

    }

}
