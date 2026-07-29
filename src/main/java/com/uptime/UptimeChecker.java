package com.uptime;

import java.net.HttpURLConnection;
import java.net.URL;

public class UptimeChecker {

    public static EndpointResult checkEndpoint(String targetUrl) {
        long startTime = System.currentTimeMillis();
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            
            String status = (responseCode >= 200 && responseCode < 400) ? "UP" : "DOWN";
            return new EndpointResult(status, responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new EndpointResult("DOWN", responseTime);
        }
    }

    public static class EndpointResult {
        private final String status;
        private final long responseTime;

        public EndpointResult(String status, long responseTime) {
            this.status = status;
            this.responseTime = responseTime;
        }

        public String getStatus() { return status; }
        public long getResponseTime() { return responseTime; }
    }
}
