package com.uptime;

import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

public class SSLChecker {

    public static void check(MonitoredUrl u) {
        int sslDays = 0;

        try {
            // Only HTTPS has SSL
            if (!u.getUrl().toLowerCase().startsWith("https")) {
                u.setSslDays(0);
                return;
            }

            // Fully-qualified URL class to avoid conflicts
            java.net.URL urlObj = new java.net.URL(u.getUrl().trim());
            HttpsURLConnection https = (HttpsURLConnection) urlObj.openConnection();

            https.setConnectTimeout(8000);
            https.setReadTimeout(8000);
            https.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            https.connect();

            // Extract certificate
            X509Certificate cert =
                    (X509Certificate) https.getServerCertificates()[0];

            Instant expiry = cert.getNotAfter().toInstant();
            sslDays = (int) Duration.between(Instant.now(), expiry).toDays();

        } catch (Exception ignored) {
            sslDays = 0;
        }

        u.setSslDays(Math.max(sslDays, 0));
    }
}
