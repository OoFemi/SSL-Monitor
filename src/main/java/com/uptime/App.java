package com.uptime;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7000);

        // Core Target and URL Endpoints matching admin.html / dashboard.html
        app.get("/api/targets", StatusController::getTargets);
        app.post("/api/targets", StatusController::addTarget);
        app.get("/api/urls", StatusController::getTargets);
        app.post("/api/admin/add-url", StatusController::addTarget);

        // Renewal Portals Endpoints
        app.get("/api/renewal-portals", StatusController::getRenewalPortals);

        // Admin Endpoints
        app.post("/api/admin/logo", AdminController::uploadLogo);
    }
}
