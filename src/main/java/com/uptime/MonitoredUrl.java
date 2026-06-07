package com.uptime;

import java.util.List;

public class MonitoredUrl {

    private int id;
    private String url;
    private String category;
    private List<String> tags;

    public MonitoredUrl() {}

    public MonitoredUrl(int id, String url, String category, List<String> tags) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.tags = tags;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

