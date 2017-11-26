package ru.javaops.masterjava.service.mail;

import com.typesafe.config.Config;
import ru.javaops.masterjava.config.Configs;

/**
 * Created by vit on 26.11.2017.
 */
public class MailConfig {

    private static class MailSettings {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private Boolean useSSL;
        private Boolean useTLS;
        private Boolean debug;
        private String fromName;
    }

    private static class StaticHolder {
        static MailSettings mailSettings;

        static {
            MailSettings tmpMailSettings = new MailSettings();
            Config mailConfig = Configs.getConfig("mail.conf", "mail");
            tmpMailSettings.host = mailConfig.getString("host");
            tmpMailSettings.port = mailConfig.getInt("port");
            tmpMailSettings.username = mailConfig.getString("username");
            tmpMailSettings.password = mailConfig.getString("password");
            tmpMailSettings.useSSL = mailConfig.getBoolean("useSSL");
            tmpMailSettings.useTLS = mailConfig.getBoolean("useTLS");
            tmpMailSettings.debug = mailConfig.getBoolean("debug");
            tmpMailSettings.fromName = mailConfig.getString("fromName");
            mailSettings = tmpMailSettings;
        }

    }

    public static String getHost() {
        return StaticHolder.mailSettings.host;
    }

    public static Integer getPort() {
        return StaticHolder.mailSettings.port;
    }

    public static String getUsername() {
        return StaticHolder.mailSettings.username;
    }

    public static String getPassword() {
        return StaticHolder.mailSettings.password;
    }

    public static boolean getUseSSL() {
        return StaticHolder.mailSettings.useSSL == null ? false : StaticHolder.mailSettings.useSSL;
    }

    public static boolean getUseTLS() {
        return StaticHolder.mailSettings.useTLS == null ? false : StaticHolder.mailSettings.useTLS;
    }

    public static boolean getDebug() {
        return StaticHolder.mailSettings.debug == null ? null : StaticHolder.mailSettings.debug;
    }

    public static String getFromName() {
        return StaticHolder.mailSettings.fromName;
    }


}