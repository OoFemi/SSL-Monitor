package com.uptime;

public class AlertService {

    public void sendAlert(String subject, String message) {
        // TODO: integrate real email/SMS providers here.
        System.out.println("ALERT: " + subject + " | " + message);
    }
}

