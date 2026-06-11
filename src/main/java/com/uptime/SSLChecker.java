package com.uptime;

import javax.net.ssl.*;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class SSLChecker {

    public static void check(MonitoredUrl u) {
        try {
            URL urlObj = new URL(u.getUrl().trim());
            String host = urlObj.getHost();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, 443)) {
                socket.setSoTimeout(5000);
                socket.startHandshake();

                SSLSession session = socket.getSession();
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];

                long diff = cert.getNotAfter().getTime() - System.currentTimeMillis();
                long days = TimeUnit.MILLISECONDS.toDays(diff);

                u.setSslDays((int) Math.max(days, 0));
            }

        } catch (Exception e) {
            u.setSslDays(0);
        }
    }
}
