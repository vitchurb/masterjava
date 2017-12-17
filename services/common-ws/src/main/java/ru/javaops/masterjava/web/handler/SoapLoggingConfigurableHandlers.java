package ru.javaops.masterjava.web.handler;


import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import ru.javaops.masterjava.config.Configs;

@Slf4j
public abstract class SoapLoggingConfigurableHandlers extends SoapLoggingHandlers {
    private static Config HOSTS;

    static {
        HOSTS = Configs.getConfig("hosts.conf", "hosts.mail");
    }

    protected SoapLoggingConfigurableHandlers(Level loggingLevel) {
        super(loggingLevel);
    }

    public static class ClientHandler extends SoapLoggingConfigurableHandlers {
        public ClientHandler(Level loggingLevel) {
            super(loggingLevel);
        }

        @Override
        protected boolean isRequest(boolean isOutbound) {
            return isOutbound;
        }
    }

    public static class ServerHandler extends SoapLoggingConfigurableHandlers {

        public ServerHandler() {
            super(Level.valueOf(HOSTS.getString("debug.server")));
        }

        @Override
        protected boolean isRequest(boolean isOutbound) {
            return !isOutbound;
        }
    }
}
