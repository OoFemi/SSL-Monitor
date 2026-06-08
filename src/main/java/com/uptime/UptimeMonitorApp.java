package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

public class UptimeMonitorApp {

    private static final String CONFIG_FILE = "config.json";
    private static final String HISTORY_DIR = "history";
    private static final String ADMIN_KEY = "fob-secret-key";

    private static List<MonitoredUrl> monitoredUrls = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        loadConfig();
        new File(HISTORY_DIR).mkdirs();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
        }).start(80);

        app.get("/", ctx -> ctx.redirect("/overview.html"));
        app.get("/admin", ctx -> ctx.redirect("/admin.html"));

        app.get("/api/overview", ctx -> {
            List<Map<String, Object>> results = new ArrayList<>();

            for (MonitoredUrl u : monitoredUrls) {
                Map<String, Object> row = new HashMap<>();
                row.put("url", u.getUrl());
                row.put("category", u.getCategory());
                row.put("tags", u.getTags());

                long start = System.currentTimeMillis();
                boolean isUp = checkStatus(u.getUrl());
                long responseTime = System.currentTimeMillis() - start;
                int sslDays = getSSLDays(u.getUrl());

                row.put("isUp", isUp);
                row.put("responseTime", responseTime);
                row.put("sslDays", sslDays);

                logHistory(u.getUrl(), isUp, responseTime, sslDays);

                results.add(row);
            }

            ctx.json(results);
        });

        app.get("/api/admin/urls", ctx -> {
            if (!isAdmin(ctx)) {
                ctx.status(401).result("Unauthorized");
                return;
            }

            int page = Integer.parseInt(Optional.ofNullable(ctx.queryParam("page")).orElse("1"));
            int size = Integer.parseInt(Optional.ofNullable(ctx.queryParam("size")).orElse("10"));

            int start = (page - 1) * size;
            if (start >= monitoredUrls.size()) start = 0;
            int end = Math.min(start + size, monitoredUrls.size());

            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("total", monitoredUrls.size());
            response.put("items", monitoredUrls.subList(start, end));

            ctx.json(response);
        });

        app.post("/api/admin/urls", ctx -> {
            if (!isAdmin(ctx)) {
                ctx.status(401).result("Unauthorized");
                return;
            }

            MonitoredUrl url = mapper.readValue(ctx.body(), MonitoredUrl.class);
            url.setId(generateId());
            if (url.getTags() == null) url.setTags(new ArrayList<>());
            monitoredUrls.add(url);
            saveConfig();
            ctx.status(201).json(url);
        });

        app.put("/api/admin/urls/{id}", ctx -> {
            if (!isAdmin(ctx)) {
                ctx.status(401).result("Unauthorized");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            MonitoredUrl updated = mapper.readValue(ctx.body(), MonitoredUrl.class);

            for (MonitoredUrl u : monitoredUrls) {
                if (u.getId() == id) {
                    u.setUrl(updated.getUrl());
                    u.setCategory(updated.getCategory());
                    u.setTags(updated.getTags());
                }
            }
            saveConfig();
            ctx.status(200);
        });

        app.delete("/api/admin/urls/{id}", ctx -> {
            if (!isAdmin(ctx)) {
                ctx.status(401).result("Unauthorized");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            monitoredUrls.removeIf(u -> u.getId() == id);
            saveConfig();
            ctx.status(200);
        });

        app.get("/api/history", ctx -> {
            String url = ctx.queryParam("url");
            if (url == null || url.isEmpty()) {
                ctx.json(Collections.emptyList());
                return;
            }

            File file = new File(HISTORY_DIR + "/" + url.replaceAll("[^a-zA-Z0-9]", "_") + ".log");
            if (!file.exists()) {
                ctx.json(Collections.emptyList());
                return;
            }

            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            ctx.json(lines);
        });

        System.out.println("🔥 Server running at http://localhost/");
    }

    private static boolean isAdmin(Context ctx) {
        return ADMIN_KEY.equals(ctx.header("X-ADMIN-KEY"));
    }

    private static void loadConfig() throws IOException {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            monitoredUrls = new ArrayList<>();
            return;
        }
        Map<String, List<MonitoredUrl>> data =
                mapper.readValue(file, new TypeReference<Map<String, List<MonitoredUrl>>>() {});
        monitoredUrls = data.getOrDefault("monitoredUrls", new ArrayList<>());
    }

    private static void saveConfig() throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("monitoredUrls", monitoredUrls);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), data);
    }

    private static int generateId() {
        return monitoredUrls.stream().mapToInt(MonitoredUrl::getId).max().orElse(0) + 1;
    }

    private static boolean checkStatus(String urlStr) {
        try {
            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private static int getSSLDays(String urlStr) {
        try {
            if (!urlStr.startsWith("https")) return -1;

            URL url = new URI(urlStr).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();

            Certificate[] certs = conn.getServerCertificates();
            X509Certificate cert = (X509Certificate) certs[0];

            long diff = cert.getNotAfter().getTime() - System.currentTimeMillis();
            return (int) (diff / (1000 * 60 * 60 * 24));

        } catch (Exception e) {
            return -1;
        }
    }

    private static void logHistory(String url, boolean up, long time, int sslDays) {
        try {
            String fileName = HISTORY_DIR + "/" + url.replaceAll("[^a-zA-Z0-9]", "_") + ".log";
            String line = String.format(
                    "%s | UP: %s | %d ms | SSL: %d days",
                    new Date(), up, time, sslDays
            );
            java.nio.file.Files.write(
                    new File(fileName).toPath(),
                    (line + "\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {}
    }
}
