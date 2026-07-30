package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Map;

public class App {
    public static void main(String[] args) {
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
}
