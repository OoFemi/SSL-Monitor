package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    private static final String DB_URL = "jdbc:sqlite:uptime.db";
    private static final Path SETTINGS_FILE = Paths.get("data", "settings.json");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        initDatabase();

        UptimeCheckerService checkerService = new UptimeCheckerService();
        checkerService.startMonitoring();

        startCertificateScheduler();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
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

        // --- SETTINGS & EMAIL NOTIFICATION ENDPOINTS ---
        app.get("/api/settings", App::getSettings);
        app.post("/api/settings", App::saveSettings);
        app.post("/api/settings/test-email", App::sendTestEmailHandler);
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

    public static void getSettings(Context ctx) {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String content = Files.readString(SETTINGS_FILE);
                ctx.result(content).contentType("application/json");
            } else {
                ctx.result("{}").contentType("application/json");
            }
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", "Failed to read settings"));
        }
    }

    public static void saveSettings(Context ctx) {
        try {
            String body = ctx.body();
            objectMapper.readTree(body);
            Files.writeString(SETTINGS_FILE, body);
            ctx.json(Map.of("status", "success", "message", "Settings saved successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid JSON payload"));
        }
    }

    public static void sendTestEmailHandler(Context ctx) {
        try {
            ObjectNode req = objectMapper.readValue(ctx.body(), ObjectNode.class);
            String recipient = req.has("email") ? req.get("email").asText() : null;

            if (recipient == null || recipient.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Recipient email is required"));
                return;
            }

            boolean sent = sendEmail(
                recipient, 
                "Test Alert - SSL Monitor", 
                "This is a test dispatch email from your SSL Monitor application. Your SMTP integration is working correctly!"
            );

            if (sent) {
                ctx.json(Map.of("status", "success", "message", "Test email dispatched successfully"));
            } else {
                ctx.status(500).json(Map.of("error", "Failed to send email. Check server SMTP configs."));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public static boolean sendEmail(String toEmail, String subject, String bodyContent) {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv("SMTP_PORT") != null ? System.getenv("SMTP_PORT") : "587";
        String user = System.getenv("SMTP_USER");
        String pass = System.getenv("SMTP_PASSWORD");
        String mailFrom = System.getenv("MAIL_FROM") != null ? System.getenv("MAIL_FROM") : "ssl-monitor@uptime.local";

        if (host == null || user == null || pass == null) {
            System.err.println("SMTP Environment variables are not fully configured!");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(bodyContent);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void startCertificateScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String targetEmail = getSavedAlertEmail();
                if (targetEmail == null || targetEmail.isEmpty()) {
                    return;
                }

                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT url, ssl_days FROM targets")) {

                    while (rs.next()) {
                        String url = rs.getString("url");
                        int daysRemaining = rs.getInt("ssl_days");

                        if (daysRemaining == 14 || daysRemaining == 7 || daysRemaining == 3 || daysRemaining == 2) {
                            sendEmail(targetEmail, 
                                "WARNING: SSL Certificate for " + url + " Expires in " + daysRemaining + " Days", 
                                "Hello,\n\nThe SSL certificate for " + url + " is set to expire in " + daysRemaining + " days."
                            );
                        } else if (daysRemaining <= 0 && daysRemaining != -1) {
                            sendEmail(targetEmail, 
                                "CRITICAL: SSL Certificate Outage Risk for " + url, 
                                "CRITICAL ALERT: The SSL certificate for " + url + " is expiring or has expired!"
                            );
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 24, TimeUnit.HOURS);
    }

    private static String getSavedAlertEmail() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                ObjectNode node = objectMapper.readValue(SETTINGS_FILE.toFile(), ObjectNode.class);
                if (node.has("alertEmail")) {
                    return node.get("alertEmail").asText();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
