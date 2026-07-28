package com.fobsslmonitor;

import java.util.concurrent.*;

public class UptimeMonitorApp {
    public static void main(String[] args) {
        Config.load();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            MonitorService.checkAllEndpoints();
            EmailService.sendAlertsIfNeeded();
        }, 0, Config.getPollIntervalHours(), TimeUnit.HOURS);
        System.out.println("FOB SSL Monitor started. Refresh every 6 hours.");
    }
}
