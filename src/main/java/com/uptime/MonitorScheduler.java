package com.uptime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitorScheduler {

    private final MonitorService monitorService;

    public MonitorScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void runChecks() {
        monitorService.updateStatuses();
    }
}
