package com.uptime;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;

public class SSLChecker {

    // Update these with your Gmail and corporate details (or load them from your configuration manager)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String GMAIL_USER = "sslactions@gmail.com";
    private static final String GMAIL_APP_PASS = "pbkj mvaf khnn sxjk";
    private static final String CORPORATE_EMAIL = "femi.adeyemi@fob.ng";

    public static long getDaysUntilExpiration(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.connect();

            Certificate[] certs = conn.getServerCertificates();
            if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                X509Certificate x509Cert = (X509Certificate) certs[0];
                Date expirationDate = x509Cert.getNotAfter();
                long diffInMillies = expirationDate.getTime() - System.currentTimeMillis();
                long daysRemaining = diffInMillies / (1000 * 60 * 60 * 24);

                // Check triggers based on your UI rules
                checkAndSendAlerts(targetUrl, daysRemaining);

                return daysRemaining;
            }
        } catch (Exception e) {
            System.out.println("Failed to check SSL for " + targetUrl + ": " + e.getMessage());
        }
        return -1; // Indicates failure or non-HTTPS
    }

    private static void checkAndSendAlerts(String targetUrl, long daysRemaining) {
        if (daysRemaining == 14 || daysRemaining == 7 || daysRemaining == 3 || daysRemaining == 2) {
            sendEmailAlert(
                "WARNING: SSL Certificate Expiration Notice",
                "The SSL certificate for " + targetUrl + " will expire in " + daysRemaining + " days."
            );
        } else if (daysRemaining <= 0) {
            sendEmailAlert(
                "CRITICAL: SSL Certificate Expired!",
                "The SSL certificate for " + targetUrl + " has expired. Immediate renewal is required."
            );
        }
    }

    public static void sendEmailAlert(String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USER, GMAIL_APP_PASS);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(CORPORATE_EMAIL));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("SSL Alert email successfully sent to " + CORPORATE_EMAIL);
        } catch (MessagingException e) {
            System.out.println("Failed to send email alert: " + e.getMessage());
        }
    }
}
