package com.uptime;

import org.springframework.stereotype.Component;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Component
public class SSLChecker {

    public int getDaysRemaining(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            X509Certificate cert = (X509Certificate) conn.getServerCertificates()[0];
            long diff = cert.getNotAfter().getTime() - System.currentTimeMillis();
            return (int) TimeUnit.MILLISECONDS.toDays(diff);
        } catch (Exception e) {
            return 0;
        }
    }
}

