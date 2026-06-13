package com.uptime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";

    public static void saveEmailSettings(EmailSettings s) throws IOException {
        Properties props = new Properties();
        props.setProperty("smtpHost", s.smtpHost);
        props.setProperty("smtpPort", String.valueOf(s.smtpPort));
        props.setProperty("smtpUser", s.smtpUser);
        props.setProperty("smtpPass", s.smtpPass);
        props.setProperty("fromEmail", s.fromEmail);
        props.setProperty("toEmail", s.toEmail);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "FOB Uptime Monitor – Email Settings");
        }
    }

    public static EmailSettings loadEmailSettings() {
        EmailSettings s = new EmailSettings();

        // Default values
        s.smtpHost = "smtp.office365.com";
        s.smtpPort = 587;
        s.smtpUser = "alerts@yourdomain.com";
        s.smtpPass = "yourAppPassword";
        s.fromEmail = "alerts@yourdomain.com";
        s.toEmail = "you@yourdomain.com";

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);

            s.smtpHost = props.getProperty("smtpHost", s.smtpHost);
            s.smtpPort = Integer.parseInt(props.getProperty("smtpPort", String.valueOf(s.smtpPort)));
            s.smtpUser = props.getProperty("smtpUser", s.smtpUser);
            s.smtpPass = props.getProperty("smtpPass", s.smtpPass);
            s.fromEmail = props.getProperty("fromEmail", s.fromEmail);
            s.toEmail = props.getProperty("toEmail", s.toEmail);

        } catch (Exception ignored) {
            // First run or missing file — defaults are fine
        }

        return s;
    }
}
