package com.uptime;

import java.sql.*;
import java.util.*;

public class Database {
    private Connection conn;

    public Database() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:uptime.db");
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS urls (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, category TEXT, tags TEXT, " +
                "isUp INTEGER, responseTime INTEGER, sslDays INTEGER)"
            );
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS admin_users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, password TEXT, email TEXT, phone TEXT)"
            );
            // Seed default admin if none
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS c FROM admin_users");
            if (rs.next() && rs.getInt("c") == 0) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO admin_users (username, password, email, phone) VALUES (?, ?, ?, ?)"
                );
                stmt.setString(1, "admin");
                stmt.setString(2, "admin123");
                stmt.setString(3, "admin@example.com");
                stmt.setString(4, "+441234567890");
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveUrl(MonitoredUrl url) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO urls (url, category, tags, isUp, responseTime, sslDays) VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, url.getUrl());
            stmt.setString(2, url.getCategory());
            stmt.setString(3, String.join(",", url.getTags()));
            stmt.setInt(4, 0);
            stmt.setInt(5, 0);
            stmt.setInt(6, 0);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUrl(int id, MonitoredUrl url) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE urls SET url=?, category=?, tags=?, isUp=?, responseTime=?, sslDays=? WHERE id=?"
            );
            stmt.setString(1, url.getUrl());
            stmt.setString(2, url.getCategory());
            stmt.setString(3, String.join(",", url.getTags()));
            stmt.setInt(4, url.isUp() ? 1 : 0);
            stmt.setInt(5, url.getResponseTime());
            stmt.setInt(6, url.getSslDays());
            stmt.setInt(7, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteUrl(int id) {
        try {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM urls WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MonitoredUrl> getAllUrls() {
        List<MonitoredUrl> urls = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM urls");
            while (rs.next()) {
                List<String> tags = rs.getString("tags") == null ? new ArrayList<>() :
                        Arrays.asList(rs.getString("tags").split(","));

                urls.add(new MonitoredUrl(
                    rs.getInt("id"),
                    rs.getString("url"),
                    rs.getString("category"),
                    tags,
                    rs.getInt("isUp") == 1,
                    rs.getInt("responseTime"),
                    rs.getInt("sslDays")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }

    public AdminUser getAdminUser(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM admin_users WHERE username=? AND password=?"
            );
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AdminUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
