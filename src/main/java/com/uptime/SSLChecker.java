package com.uptime;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SSLChecker {

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
                return diffInMillies / (1000 * 60 * 60 * 24);
            }
        } catch (Exception e) {
            System.out.println("Failed to check SSL for " + targetUrl + ": " + e.getMessage());
        }
        return -1; // Indicates failure or non-HTTPS
    }
}
