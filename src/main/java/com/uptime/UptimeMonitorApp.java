package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UptimeMonitorApp {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
        }).start(7000);

        Database db = new Database();
        AlertService alerts = new AlertService();

        // 🔁 Live uptime + SSL checks every 2 minutes
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            List<MonitoredUrl> urls = db.getAllUrls();
            for (MonitoredUrl u : urls) {
                boolean previousUp = u.isUp();
                try {
                    long start = System.currentTimeMillis();
                    HttpURLConnection conn = (HttpURLConnection) new URI(u.getUrl()).toURL().openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();

                    boolean isUp = conn.getResponseCode() < 400;
                    long responseTime = System.currentTimeMillis() - start;

                    int sslDays = 0;
                    if (u.getUrl().startsWith("https")) {
                        try {
                            HttpsURLConnection https = (HttpsURLConnection) conn;
                            https.connect();
                            X509Certificate cert = (X509Certificate) https.getServerCertificates()[0];
                            Instant expiry = cert.getNotAfter().toInstant();
                            sslDays = (int) Duration.between(Instant.now(), expiry).toDays();
                        } catch (Exception ignored) {}
                    }

                    u.setUp(isUp);
                    u.setResponseTime((int) responseTime);
                    u.setSslDays(sslDays);
                    db.updateUrl(u.getId(), u);

                    if (previousUp && !isUp) {
                        alerts.sendAlert("DOWN: " + u.getUrl(),
                                "URL is DOWN: " + u.getUrl());
                    }
                    if (sslDays > 0 && sslDays <= 7) {
                        alerts.sendAlert("SSL expiring soon: " + u.getUrl(),
                                "SSL certificate expires in " + sslDays + " days for " + u.getUrl());
                    }

                } catch (Exception e) {
                    u.setUp(false);
                    u.setResponseTime(0);
                    u.setSslDays(0);
                    db.updateUrl(u.getId(), u);

                    if (previousUp) {
                        alerts.sendAlert("DOWN: " + u.getUrl(),
                                "URL is DOWN: " + u.getUrl());
                    }
                }
            }
            System.out.println("✔ Live check completed at " + LocalTime.now());
        }, 0, 2, TimeUnit.MINUTES);

        // Dashboard API
        app.get("/api/urls", ctx -> ctx.json(db.getAllUrls()));

        // CSV export
        app.get("/api/urls/export", ctx -> {
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"uptime.csv\"");
            StringBuilder sb = new StringBuilder();
            sb.append("id,url,category,tags,isUp,responseTime,sslDays\n");
            for (MonitoredUrl u : db.getAllUrls()) {
                sb.append(u.getId()).append(",")
                  .append(u.getUrl()).append(",")
                  .append(u.getCategory()).append(",")
                  .append(String.join("|", u.getTags())).append(",")
                  .append(u.isUp()).append(",")
                  .append(u.getResponseTime()).append(",")
                  .append(u.getSslDays()).append("\n");
            }
            ctx.result(sb.toString());
        });

        // Multi-user admin login
        app.post("/api/admin/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            AdminUser user = db.getAdminUser(username, password);
            if (user != null) {
                ctx.status(200).json(user);
            } else {
                ctx.status(401).result("Invalid credentials");
            }
        });

        // Add URL
        app.post("/api/admin/add-url", ctx -> {
            MonitoredUrl url = ctx.bodyAsClass(MonitoredUrl.class);
            db.saveUrl(url);
            ctx.status(201).result("Saved");
        });

        // Edit URL
        app.put("/api/admin/edit-url/:id", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            MonitoredUrl url = ctx.bodyAsClass(MonitoredUrl.class);
            db.updateUrl(id, url);
            ctx.status(200).result("Updated");
        });

        // Delete URL
        app.delete("/api/admin/delete-url/:id", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            db.deleteUrl(id);
            ctx.status(200).result("Deleted");
        });

        app.get("/", ctx -> ctx.redirect("/dashboard.html"));
    }
}
