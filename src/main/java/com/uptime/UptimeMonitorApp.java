package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UptimeMonitorApp {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
        }).start(7000);

        Database db = new Database();
        AlertService alerts = new AlertService();

        // Scheduled uptime checks
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            List<MonitoredUrl> urls = db.getAllUrls();
            urls.forEach(u -> UptimeChecker.runSingleCheck(u, db, alerts));
            System.out.println("✔ Live check completed at " + LocalTime.now());
        }, 0, 2, TimeUnit.MINUTES);

        // -----------------------------
        // URL CRUD API
        // -----------------------------
        app.get("/api/urls", ctx -> ctx.json(db.getAllUrls()));

        app.get("/api/urls/export", ctx -> {
            String csv = db.exportCsv();
            ctx.header("Content-Type", "text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"uptime.csv\"");
            ctx.result(csv);
        });

        app.post("/api/admin/add-url", ctx -> {
            try {
                MonitoredUrl url = ctx.bodyAsClass(MonitoredUrl.class);
                if (url.getTags() == null) url.setTags(new java.util.ArrayList<>());
                if (url.getHistoryResponseTimes() == null) url.setHistoryResponseTimes(new java.util.ArrayList<>());
                db.saveUrl(url);
                UptimeChecker.runSingleCheck(url, db, alerts);
                ctx.status(201).result("Saved");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Server Error: " + e.getMessage());
            }
        });

        app.put("/api/admin/url/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                MonitoredUrl url = ctx.bodyAsClass(MonitoredUrl.class);
                url.setId(id);
                if (url.getTags() == null) url.setTags(new java.util.ArrayList<>());
                if (url.getHistoryResponseTimes() == null) url.setHistoryResponseTimes(new java.util.ArrayList<>());
                db.updateUrl(id, url);
                ctx.status(200).result("Updated");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Server Error: " + e.getMessage());
            }
        });

        app.delete("/api/admin/delete-url/{id}", ctx -> {
            db.deleteUrl(Integer.parseInt(ctx.pathParam("id")));
            ctx.status(200).result("Deleted");
        });

        // -----------------------------
        // LOGS
        // -----------------------------
        app.get("/api/logs", ctx -> ctx.json(alerts.getLogs()));

        // -----------------------------
        // EMAIL SETTINGS API (NEW)
        // -----------------------------
        app.get("/api/settings/email", ctx -> {
            EmailSettings settings = ConfigManager.loadEmailSettings();
            ctx.json(settings);
        });

        app.post("/api/settings/email", ctx -> {
            try {
                EmailSettings settings = ctx.bodyAsClass(EmailSettings.class);
                ConfigManager.saveEmailSettings(settings);
                ctx.status(200).result("Email settings saved.");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error saving settings: " + e.getMessage());
            }
        });

        // -----------------------------
        // ROOT REDIRECT
        // -----------------------------
        app.get("/", ctx -> ctx.redirect("/dashboard.html"));
    }
}
