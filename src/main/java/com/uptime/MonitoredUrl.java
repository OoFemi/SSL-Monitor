package com.uptime;

import java.util.List;

public class MonitoredUrl {
    private int id;
    private String url;
    private String category;
    private List<String> tags;
    private boolean isUp;
    private int responseTime;
    private int sslDays;

    public MonitoredUrl() {}

    public MonitoredUrl(int id, String url, String category, List<String> tags,
                        boolean isUp, int responseTime, int sslDays) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.tags = tags;
        this.isUp = isUp;
        this.responseTime = responseTime;
        this.sslDays = sslDays;
    }

    public int getId() { return id; }
    public String getUrl() { return url; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public boolean isUp() { return isUp; }
    public int getResponseTime() { return responseTime; }
    public int getSslDays() { return sslDays; }

    public void setId(int id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setCategory(String category) { this.category = category; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUp(boolean up) { isUp = up; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }
    public void setSslDays(int sslDays) { this.sslDays = sslDays; }
}
