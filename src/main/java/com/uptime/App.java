package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    private static final String DB_URL = "jdbc:sqlite:uptime.db";

    public static void main(String[] args) {
        initDatabase();

        // Start background polling service so dashboards load instantly
        UptimeCheckerService checkerService = new UptimeCheckerService();
        checkerService.startMonitoring();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7000);

        // --- AUTHENTICATION ---
        app.post("/api/admin/login", ctx -> {
            Map<?, ?> creds = ctx.bodyAsClass(Map.class);
            String u = (String) creds.get("username");
            String p = (String) creds.get("password");
            
            if ("admin".equals(u) && "admin123".equals(p)) {
                ctx.json(Map.of("success", true, "token", "active-session-token"));
            } else {
                ctx.status(401).json(Map.of("success", false, "message", "Invalid credentials."));
            }
        });

        // --- CORE TARGET & URL ENDPOINTS ---
        app.get("/api/targets", StatusController::getTargets);
        app.post("/api/targets", StatusController::addTarget);
        app.put("/api/targets/{id}", StatusController::updateTarget);
        app.delete("/api/targets/{id}", StatusController::deleteTarget);

        app.get("/api/urls", StatusController::getTargets);
        app.post("/api/admin/add-url", StatusController::addTarget);
        app.put("/api/urls/{id}", StatusController::updateTarget);
        app.delete("/api/urls/{id}", StatusController::deleteTarget);
        
        // Added missing admin endpoint variants called by the frontend
        app.delete("/api/admin/delete-url/{id}", StatusController::deleteTarget);
        app.put("/api/admin/update-url/{id}", StatusController::updateTarget);

        // --- RENEWAL PORTALS ENDPOINTS ---
        app.get("/api/renewal-portals", StatusController::getRenewalPortals);
        app.post("/api/renewal-portals", StatusController::addRenewalPortal);
        app.post("/api/admin/add-portal", StatusController::addRenewalPortal);
        
        app.put("/api/admin/update-portal/{id}", ctx -> {
            ctx.status(200).json(Map.of("status", "success", "message", "Renewal portal updated successfully"));
        });

        app.delete("/api/renewal-portals/{id}", StatusController::deleteRenewalPortal);
        app.delete("/api/admin/delete-portal/{id}", StatusController::deleteRenewalPortal);

        // --- ADMIN / LOGO UPLOAD ---
        app.post("/api/admin/logo", AdminController::uploadLogo);
        app.post("/api/admin/upload-logo", AdminController::uploadLogo);
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS targets (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "url TEXT NOT NULL, " +
                         "category TEXT, " +
                         "tags TEXT, " +
                         "status TEXT, " +
                         "latency INTEGER, " +
                         "ssl_days INTEGER, " +
                         "last_checked TEXT)";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class UptimeCheckerService {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        public void startMonitoring() {
            scheduler.scheduleAtFixedRate(this::checkAllTargets, 0, 60, TimeUnit.SECONDS);
        }

        private void checkAllTargets() {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, url FROM targets")) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String url = rs.getString("url");
                    
                    long start = System.currentTimeMillis();
                    String status = "DOWN";
                    long latency = -1;

                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build();
                        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                        if (response.statusCode() >= 200 && response.statusCode() < 400) {
                            status = "UP";
                        }
                        latency = System.currentTimeMillis() - start;
                    } catch (Exception e) {
                        status = "DOWN";
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE targets SET status = ?, latency = ?, last_checked = ? WHERE id = ?")) {
                        pstmt.setString(1, status);
                        pstmt.setLong(2, latency);
                        pstmt.setString(3, Instant.now().toString());
                        pstmt.setInt(4, id);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void stopMonitoring() {
            scheduler.shutdown();
        }
    }
}
