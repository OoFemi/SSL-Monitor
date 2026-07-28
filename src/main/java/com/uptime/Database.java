package com.uptime;

import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class Database {

    // In-memory store of monitored URLs
    private static final List<MonitoredUrl> urls = new ArrayList<>();
    private static int nextId = 1;

    public List<MonitoredUrl> getAllUrls() {
        return new ArrayList<>(urls);
    }

    public void saveUrl(MonitoredUrl url) {
        url.setId(nextId++);
        urls.add(url);
    }

    public void updateUrl(int id, MonitoredUrl url) {
        for (int i = 0; i < urls.size(); i++) {
            if (urls.get(i).getId() == id) {
                urls.set(i, url);
                return;
            }
        }
    }

    public void deleteUrl(String urlString) {
        urls.removeIf(u -> u.getUrl().equalsIgnoreCase(urlString));
    }

    /**
     * ✅ Alerts for EmailService
     * Generate alerts if endpoints are down or SSL certificates are expiring soon.
     */
    public static List<EndpointStatus> getAlerts() {
        List<EndpointStatus> alerts = new ArrayList<>();
        for (MonitoredUrl u : urls) {
            if (!u.isUp()) {
                alerts.add(new EndpointStatus(u.getUrl(), "Endpoint is DOWN"));
            }
            if (u.getSslDays() < 10) {
                alerts.add(new EndpointStatus(u.getUrl(), "SSL certificate expiring soon"));
            }
        }
        return alerts;
    }
}
