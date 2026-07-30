package com.uptime;

import io.javalin.http.Context;
import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusController {

    private static final List<Map<String, Object>> targets = new ArrayList<>();
    private static final List<Map<String, Object>> renewalPortals = new ArrayList<>();

    public static void getTargets(Context ctx) {
        // Dynamically inspect each target in real-time when the dashboard fetches data
        List<Map<String, Object>> updatedTargets = new ArrayList<>();
        
        for (Map<String, Object> target : targets) {
            Map<String, Object> evaluated = new HashMap<>(target);
            String urlStr = (String) target.get("url");

            if (urlStr != null && !urlStr.isEmpty()) {
                long startTime = System.currentTimeMillis();
                try {
                    String formattedUrl = urlStr;
                    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                        formattedUrl = "https://" + formattedUrl;
                    }

                    URL url = URI.create(formattedUrl).toURL();
                    
                    if (formattedUrl.startsWith("https://")) {
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setConnectTimeout(4000);
                        conn.setReadTimeout(4000);
                        conn.setRequestMethod("GET");
                        conn.connect();

                        int responseCode = conn.getResponseCode();
                        long responseTime = System.currentTimeMillis() - startTime;

                        evaluated.put("isUp", responseCode >= 200 && responseCode < 400);
                        evaluated.put("responseTime", responseTime);
                        evaluated.put("lastChecked", System.currentTimeMillis());

                        // Extract SSL Certificate Expiration
                        Certificate[] certs = conn.getServerCertificates();
                        if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                            X509Certificate x509Cert = (X509Certificate) certs[0];
                            Date expirationDate = x509Cert.getNotAfter();
                            long diffInMillis = expirationDate.getTime() - System.currentTimeMillis();
                            long sslDays = diffInMillis / (1000 * 60 * 60 * 24);
                            evaluated.put("sslDays", Math.max(0, sslDays));
                        } else {
                            evaluated.put("sslDays", 30);
                        }
                        conn.disconnect();
                    } else {
                        // Fallback handling for plain HTTP targets
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(4000);
                        conn.setRequestMethod("GET");
                        conn.connect();
                        int responseCode = conn.getResponseCode();
                        
                        evaluated.put("isUp", responseCode >= 200 && responseCode < 400);
                        evaluated.put("responseTime", System.currentTimeMillis() - startTime);
                        evaluated.put("lastChecked", System.currentTimeMillis());
                        evaluated.put("sslDays", 0);
                        conn.disconnect();
                    }

                } catch (Exception e) {
                    evaluated.put("isUp", false);
                    evaluated.put("responseTime", 0L);
                    evaluated.put("sslDays", 0);
                    evaluated.put("lastChecked", System.currentTimeMillis());
                }
            }
            updatedTargets.add(evaluated);
        }

        ctx.json(updatedTargets);
    }

    public static void getRenewalPortals(Context ctx) {
        ctx.json(renewalPortals);
    }

    public static void addTarget(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body != null) {
                targets.add(body);
                ctx.status(201).json(Map.of("message", "Target added successfully"));
                return;
            }
            ctx.status(400).json(Map.of("error", "Invalid payload"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }
}
