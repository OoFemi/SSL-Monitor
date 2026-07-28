package com.uptime;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/status")
@CrossOrigin(origins = "*")
public class StatusController {

    private final MonitorService monitorService;

    public StatusController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/list")
    public List<MonitoredUrl> listEndpoints() {
        return monitorService.getAllMonitoredUrls();
    }

    @PostMapping("/add")
    public MonitoredUrl addEndpoint(@RequestBody MonitoredUrl url) {
        monitorService.addMonitoredUrl(url);
        return url;
    }

    @PostMapping("/delete")
    public String deleteEndpoint(@RequestBody MonitoredUrl url) {
        monitorService.removeMonitoredUrl(url.getUrl());
        return "Deleted";
    }

    @GetMapping("/ping")
    public String ping() {
        return "Status API is running";
    }
}
