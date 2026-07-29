package com.uptime;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatusController {

    private static final List<Map<String, Object>> targets = new ArrayList<>();
    private static final List<Map<String, Object>> renewalPortals = new ArrayList<>();

    public static void getTargets(Context ctx) {
        ctx.json(targets);
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
