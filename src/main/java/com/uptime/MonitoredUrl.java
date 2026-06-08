package com.uptime;

import java.util.Arrays;
import java.util.List;

public class MonitoredUrl {

    private int id;
    private String url;
    private String category;
    private List<String> tags;

    // Default constructor (required for Jackson)
    public MonitoredUrl() {}

    // Full constructor
    public MonitoredUrl(int id, String url, String category, List<String> tags) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.tags = tags;
    }

    // -------- SAMPLE URL FACTORY --------
    public static List<MonitoredUrl> sampleData() {
        return Arrays.asList(
            new MonitoredUrl(
                1,
                "https://google.com",
                "Search Engine",
                Arrays.asList("global", "public", "ssl")
            ),
            new MonitoredUrl(
                2,
                "https://github.com",
                "Developer Tools",
                Arrays.asList("code", "repo", "ssl")
            ),
            new MonitoredUrl(
                3,
                "https://microsoft.com",
                "Enterprise",
                Arrays.asList("production", "uptime", "ssl")
            ),
            new MonitoredUrl(
                4,
                "https://fobmonitor.com",
                "Internal",
                Arrays.asList("fob", "monitor", "ssl")
            )
        );
    }

    // -------- GETTERS & SETTERS --------
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

    @Override
    public String toString() {
        return "MonitoredUrl{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", category='" + category + '\'' +
                ", tags=" + tags +
                '}';
    }
}
