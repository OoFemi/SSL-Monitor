package com.uptime;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

public class UptimeChecker {

    public static void runSingleCheck(MonitoredUrl u, Database db, AlertService alerts) {

        boolean previousUp = u.isUp();
        int previousSslDays = u.getSslDays();

        try {
            long start = System.currentTimeMillis();

            java.net.URL urlObj = new java.net.URL(u.getUrl());
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            boolean isUp = code >= 200 && code < 400;
            long responseTime = System.currentTimeMillis() - start;

            int sslDays = 0;

            // SSL CHECK
            if (u.getUrl().startsWith("https")) {
                try {
                    HttpsURLConnection https = (HttpsURLConnection) conn;
                    https.connect();
                    X509Certificate cert = (X509Certificate) https.getServerCertificates()[0];
                    Instant expiry = cert.getNotAfter().toInstant();
                    sslDays = (int) Duration.between(Instant.now(), expiry).toDays();
                } catch (Exception ignored) {}
            }

            // UPDATE OBJECT
            u.setUp(isUp);
            u.setResponseTime((int) responseTime);
            u.setSslDays(sslDays);
            u.setLastChecked(System.currentTimeMillis());

            db.updateUrl(u.getId(), u);

            // -------------------------
            // ALERT LOGIC
            // -------------------------

            // DOWN ALERT
            if (previousUp && !isUp) {
                alerts.sendDownAlert(u.getUrl());
            }

            // RECOVERY ALERT
            if (!previousUp && isUp) {
                alerts.sendRecoveryAlert(u.getUrl());
            }

            // SSL EXPIRED
            if (sslDays <= 0 && previousSslDays > 0) {
                alerts.sendSSLAlert(u.getUrl(), sslDays);
            }

            // SSL RESTORED
            if (sslDays > 0 && previousSslDays <= 0) {
                alerts.sendEmail("SSL RESTORED", u.getUrl());
            }

            // SLOW RESPONSE ALERT (>2000ms)
            if (isUp && responseTime > 2000) {
                alerts.sendSlowAlert(u.getUrl(), responseTime);
            }

        } catch (Exception e) {

            u.setUp(false);
            u.setResponseTime(0);
            u.setSslDays(0);
            u.setLastChecked(System.currentTimeMillis());
            db.updateUrl(u.getId(), u);

            if (previousUp) {
                alerts.sendDownAlert(u.getUrl());
            }
        }
    }
}
