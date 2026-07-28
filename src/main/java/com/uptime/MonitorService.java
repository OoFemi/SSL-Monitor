package com.uptime;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MonitorService {

    private final Database database;
    private final UptimeChecker uptimeChecker;
    private final SSLChecker sslChecker;

    public MonitorService(Database database, UptimeChecker uptimeChecker, SSLChecker sslChecker) {
        this.database = database;
        this.uptimeChecker = uptimeChecker;
        this.sslChecker = sslChecker;
    }

    public List<MonitoredUrl> getAllMonitoredUrls() {
        return database.getAllUrls();
    }

    public void addMonitoredUrl(MonitoredUrl url) {
        url.setUp(false);
        url.setResponseTime(0);
        url.setSslDays(0);
        url.setLastChecked(System.currentTimeMillis());
        database.saveUrl(url);
    }

    public void removeMonitoredUrl(String url) {
        database.deleteUrl(url);
    }

    public void updateStatuses() {
        for (MonitoredUrl url : database.getAllUrls()) {
            boolean isUp = uptimeChecker.checkStatus(url.getUrl());
            int latency = uptimeChecker.measureLatency(url.getUrl());
            int sslDays = sslChecker.getDaysRemaining(url.getUrl());

            url.setUp(isUp);
            url.setResponseTime(latency);
            url.setSslDays(sslDays);
            url.setLastChecked(System.currentTimeMillis());

            database.updateUrl(url.getId(), url);
        }
    }
}
