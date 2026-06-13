package com.uptime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class AlertService {

    private final List<String> logs = new ArrayList<>();

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPass;
    private String fromEmail;
    private String toEmail;

    public AlertService() {
        EmailSettings s = ConfigManager.loadEmailSettings();
        this.smtpHost = s.smtpHost;
        this.smtpPort = s.smtpPort;
        this.smtpUser = s.smtpUser;
        this.smtpPass = s.smtpPass;
        this.fromEmail = s.fromEmail;
        this.toEmail = s.toEmail;

        log("Email settings loaded: " + smtpHost + ":" + smtpPort + " -> " + toEmail);
    }

    private void log(String text) {
        String entry = "[" + LocalDateTime.now() + "] " + text;
        logs.add(entry);
        System.out.println(entry);
    }

    public List<String> getLogs() {
        return logs;
    }

    public void sendEmail(String subject, String message) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPass);
                }
            });

            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromEmail));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            email.setSubject(subject);
            email.setText(message);

            Transport.send(email);
            log("EMAIL SENT: " + subject);

        } catch (Exception e) {
            log("EMAIL FAILED: " + e.getMessage());
        }
    }

    public void sendDownAlert(String url) {
        String msg = "The website is DOWN.\nURL: " + url + "\nTime: " + LocalDateTime.now();
        log("DOWN ALERT: " + url);
        sendEmail("⚠️ Website DOWN: " + url, msg);
    }

    public void sendRecoveryAlert(String url) {
        String msg = "The website is back ONLINE.\nURL: " + url + "\nTime: " + LocalDateTime.now();
        log("RECOVERY ALERT: " + url);
        sendEmail("✅ Website RECOVERED: " + url, msg);
    }

    public void sendSSLAlert(String url, int daysLeft) {
        String msg = "SSL certificate expires in " + daysLeft + " days.\nURL: " + url;
        log("SSL ALERT: " + url + " (" + daysLeft + " days left)");
        sendEmail("🔒 SSL Expiry Warning: " + url, msg);
    }

    public void sendSlowAlert(String url, long responseTime) {
        String msg = "Slow response detected: " + responseTime + "ms\nURL: " + url;
        log("SLOW ALERT: " + url + " (" + responseTime + "ms)");
        sendEmail("🐌 Slow Response: " + url, msg);
    }
}
