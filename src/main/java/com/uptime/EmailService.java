package com.uptime;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAlerts() {
        // Get alerts from Database
        List<EndpointStatus> alerts = Database.getAlerts();

        if (alerts.isEmpty()) {
            return; // nothing to send
        }

        StringBuilder body = new StringBuilder("FOB Uptime Monitor Alerts:\n\n");
        for (EndpointStatus alert : alerts) {
            body.append("URL: ").append(alert.getUrl())
                .append("\nIssue: ").append(alert.getMessage())
                .append("\n\n");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("admin@example.com"); // replace with your recipient
        message.setSubject("FOB Uptime Monitor Alerts");
        message.setText(body.toString());

        mailSender.send(message);
    }
}
