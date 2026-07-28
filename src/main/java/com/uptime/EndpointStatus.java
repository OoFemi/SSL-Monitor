package com.uptime;

public class EndpointStatus {
    private String url;
    private String message;

    public EndpointStatus(String url, String message) {
        this.url = url;
        this.message = message;
    }

    public String getUrl() { return url; }
    public String getMessage() { return message; }
}
