package com.uptime;

public class AdminUser {
    private int id;
    private String username;
    private String email;
    private String phone;

    public AdminUser(int id, String username, String email, String phone) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
