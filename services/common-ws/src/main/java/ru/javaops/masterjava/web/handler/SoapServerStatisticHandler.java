package ru.javaops.masterjava.web.handler;

import com.sun.xml.ws.api.handler.MessageHandlerContext;
import com.sun.xml.ws.message.stream.StreamMessage;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.web.Statistics;

import javax.xml.ws.handler.MessageContext;
import java.util.Date;

/**
 * Created by vit on 17.12.2017.
 */
@Slf4j
public class SoapServerStatisticHandler extends SoapBaseHandler {
    private static final String DATE_START_PARAM_NAME = "SoapServerStatisticHandler_DATE_START";
    private static final String RESULT_PARAM_NAME = "SoapServerStatisticHandler_RESULT";

    @Override
    public boolean handleMessage(MessageHandlerContext context) {
        log.info((isOutbound(context) ? "SOAP out : " : "SOAP in: ") + context.getMessage().getPayloadLocalPart());
        context.put(DATE_START_PARAM_NAME, new Date());
        return true;
    }

    @Override
    public boolean handleFault(MessageHandlerContext context) {
        log.info((isOutbound(context) ? "SOAP out : " : "SOAP in: ") + context.getMessage().getPayloadLocalPart());
        context.put(RESULT_PARAM_NAME, Boolean.FALSE);
        return true;
    }

    @Override
    public void close(MessageContext context) {
        Date dtStart = (Date) context.get(DATE_START_PARAM_NAME);
        Boolean result = (Boolean) context.get(RESULT_PARAM_NAME);
        StreamMessage requestMessage = ((StreamMessage) context.get("REQUEST_MSG"));
        Statistics.count("" + (requestMessage == null ? "" : requestMessage.getPayloadLocalPart())
                        + " " + context.get("javax.xml.ws.wsdl.operation"),
                dtStart == null ? 0 : dtStart.getTime(),
                result == Boolean.FALSE ? Statistics.RESULT.FAIL : Statistics.RESULT.SUCCESS);
        super.close(context);
    }

    public static class ServerHandler extends SoapServerStatisticHandler {

    }

}
