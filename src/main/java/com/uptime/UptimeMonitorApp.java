package com.uptime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UptimeMonitorApp {
    public static void main(String[] args) {
        SpringApplication.run(UptimeMonitorApp.class, args);
    }
}
