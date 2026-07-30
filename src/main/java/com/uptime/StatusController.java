package com.uptime;

import io.javalin.http.Context;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class StatusController {

    private static final String STORAGE_FILE = "data/targets.json";
    private static final String PORTALS_FILE = "data/portals.json";
    
    private static final List<Map<String, Object>> targets = new ArrayList<>();
    private static final List<Map<String, Object>> renewalPortals = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        loadTargetsFromFile();
        loadPortalsFromFile();
    }

    private static synchronized void loadTargetsFromFile() {
        try {
            File file = new File(STORAGE_FILE);
            if (file.exists() && file.length() > 0) {
                String content = Files.readString(Path.of(STORAGE_FILE));
                List<Map<String, Object>> loaded = objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
                if (loaded != null) { 
                    targets.addAll(loaded); 
                }
            } else {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static synchronized void saveTargetsToFile() {
        try {
            File file = new File(STORAGE_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, targets);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static synchronized void loadPortalsFromFile() {
        try {
            File file = new File(PORTALS_FILE);
            if (file.exists() && file.length() > 0) {
                String content = Files.readString(Path.of(PORTALS_FILE));
                List<Map<String, Object>> loaded = objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
                if (loaded != null) { 
                    renewalPortals.addAll(loaded); 
                }
            } else {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static synchronized void savePortalsToFile() {
        try {
            File file = new File(PORTALS_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, renewalPortals);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    public static void getTargets(Context ctx) {
        List<Map<String, Object>> updatedTargets = new ArrayList<>();
        
        synchronized (targets) {
            for (int i = 0; i < targets.size(); i++) {
                Map<String, Object> target = targets.get(i);
                Map<String, Object> evaluated = new HashMap<>(target);
                evaluated.put("id", i); // Frontend needs this ID for Edit/Delete
                
                String urlStr = (String) target.get("url");

                if (urlStr != null && !urlStr.isEmpty()) {
                    long startTime = System.currentTimeMillis();
                    try {
                        boolean isHttps = urlStr.toLowerCase().startsWith("https://");
                        String formattedUrl = urlStr;
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://" + formattedUrl;
                            isHttps = true;
                        }

                        URL url = URI.create(formattedUrl).toURL();
                        
                        if (isHttps) {
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
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(4000);
                            conn.setRequestMethod("GET");
                            conn.connect();
                            int responseCode = conn.getResponseCode();
                            
                            evaluated.put("isUp", responseCode >= 200 && responseCode < 400);
                            evaluated.put("responseTime", System.currentTimeMillis() - startTime);
                            evaluated.put("lastChecked", System.currentTimeMillis());
                            evaluated.put("sslDays", -1); 
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
        }

        ctx.json(updatedTargets);
    }

    public static void getRenewalPortals(Context ctx) {
        synchronized (renewalPortals) {
            List<Map<String, Object>> indexedPortals = new ArrayList<>();
            for (int i = 0; i < renewalPortals.size(); i++) {
                Map<String, Object> p = new HashMap<>(renewalPortals.get(i));
                p.put("id", i); // Frontend needs this ID for Delete
                indexedPortals.add(p);
            }
            ctx.json(indexedPortals);
        }
    }

    public static void addRenewalPortal(Context ctx) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body != null) {
                synchronized (renewalPortals) {
                    renewalPortals.add(body);
                    savePortalsToFile();
                }
                ctx.status(201).json(Map.of("message", "Renewal provider saved successfully"));
                return;
            }
            ctx.status(400).json(Map.of("error", "Invalid payload"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public static void deleteRenewalPortal(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            synchronized (renewalPortals) {
                if (id >= 0 && id < renewalPortals.size()) {
                    renewalPortals.remove(id);
                    savePortalsToFile();
                    ctx.json(Map.of("message", "Renewal provider deleted successfully"));
                } else {
                    ctx.status(404).json(Map.of("error", "Provider not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public static void addTarget(Context ctx) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body != null) {
                synchronized (targets) {
                    targets.add(body);
                    saveTargetsToFile();
                }
                ctx.status(201).json(Map.of("message", "Target added successfully"));
                return;
            }
            ctx.status(400).json(Map.of("error", "Invalid payload"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public static void updateTarget(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            synchronized (targets) {
                if (id >= 0 && id < targets.size()) {
                    targets.set(id, body);
                    saveTargetsToFile();
                    ctx.json(Map.of("message", "Target updated successfully"));
                } else {
                    ctx.status(404).json(Map.of("error", "Target not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    public static void deleteTarget(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            
            synchronized (targets) {
                if (id >= 0 && id < targets.size()) {
                    targets.remove(id);
                    saveTargetsToFile();
                    ctx.json(Map.of("message", "Target deleted successfully"));
                } else {
                    ctx.status(404).json(Map.of("error", "Target not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }
}
