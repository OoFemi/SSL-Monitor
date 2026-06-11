package com.uptime;

import java.net.HttpURLConnection;
import java.net.URL;

public class UptimeChecker {

    public static void check(MonitoredUrl u) {
        long start = System.currentTimeMillis();

        try {
            URL urlObj = new URL(u.getUrl().trim());
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            boolean isUp = (code >= 200 && code < 400);

            u.setUp(isUp);
            u.setResponseTime((int) (System.currentTimeMillis() - start));

        } catch (Exception e) {
            u.setUp(false);
            u.setResponseTime(0);
        }
    }
}
