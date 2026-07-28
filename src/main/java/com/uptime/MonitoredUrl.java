package com.uptime;

public class MonitoredUrl {
    private int id;
    private String url;
    private String category;
    private boolean up;
    private int responseTime;
    private int sslDays;
    private long lastChecked;

    public MonitoredUrl() {}

    public MonitoredUrl(int id, String url, String category) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.up = false;
        this.responseTime = 0;
        this.sslDays = 0;
        this.lastChecked = System.currentTimeMillis();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isUp() { return up; }
    public void setUp(boolean up) { this.up = up; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

    public int getSslDays() { return sslDays; }
    public void setSslDays(int sslDays) { this.sslDays = sslDays; }

    public long getLastChecked() { return lastChecked; }
    public void setLastChecked(long lastChecked) { this.lastChecked = lastChecked; }
}
