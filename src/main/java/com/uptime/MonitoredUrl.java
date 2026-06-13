package com.uptime;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class MonitoredUrl {

    private int id;
    private String url;
    private String category;
    private List<String> tags = new ArrayList<>();

    @JsonProperty("isUp")   // 👈 maps JSON field "isUp" to this variable
    private boolean isUp;

    private int responseTime;
    private int sslDays;
    private long lastChecked;
    private List<Integer> historyResponseTimes = new ArrayList<>();

    public MonitoredUrl() {}

    public MonitoredUrl(int id, String url, String category, List<String> tags,
                        boolean isUp, int responseTime, int sslDays,
                        long lastChecked, List<Integer> historyResponseTimes) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.isUp = isUp;
        this.responseTime = responseTime;
        this.sslDays = sslDays;
        this.lastChecked = lastChecked;
        this.historyResponseTimes = historyResponseTimes != null
                ? historyResponseTimes
                : new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @JsonProperty("isUp")   // 👈 ensures JSON serialization uses "isUp"
    public boolean isUp() { return isUp; }
    public void setUp(boolean up) { isUp = up; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
        historyResponseTimes.add(responseTime);
        if (historyResponseTimes.size() > 20) historyResponseTimes.remove(0);
    }

    public int getSslDays() { return sslDays; }
    public void setSslDays(int sslDays) { this.sslDays = sslDays; }

    public long getLastChecked() { return lastChecked; }
    public void setLastChecked(long lastChecked) { this.lastChecked = lastChecked; }

    public List<Integer> getHistoryResponseTimes() { return historyResponseTimes; }
    public void setHistoryResponseTimes(List<Integer> historyResponseTimes) {
        this.historyResponseTimes = historyResponseTimes != null
                ? historyResponseTimes
                : new ArrayList<>();
    }
}
