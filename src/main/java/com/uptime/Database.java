package com.uptime;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Database {
    private Connection conn;

    public Database() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:uptime.db");
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws SQLException {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS urls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT,
                category TEXT,
                tags TEXT,
                isUp INTEGER,
                responseTime INTEGER,
                sslDays INTEGER,
                lastChecked INTEGER,
                history TEXT
            );
        """);
    }

    public List<MonitoredUrl> getAllUrls() {
        List<MonitoredUrl> urls = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM urls")) {
            while (rs.next()) {

                List<String> tags = new ArrayList<>();
                String tagStr = rs.getString("tags");
                if (tagStr != null && !tagStr.isEmpty()) {
                    tags = Arrays.asList(tagStr.split("\\|"));
                }

                List<Integer> history = new ArrayList<>();
                String historyStr = rs.getString("history");
                if (historyStr != null && !historyStr.isEmpty()) {
                    for (String s : historyStr.split(",")) {
                        try { history.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
                    }
                }

                urls.add(new MonitoredUrl(
                        rs.getInt("id"),
                        rs.getString("url"),
                        rs.getString("category"),
                        tags,
                        rs.getInt("isUp") == 1,
                        rs.getInt("responseTime"),
                        rs.getInt("sslDays"),
                        rs.getLong("lastChecked"),
                        history
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }

    public void saveUrl(MonitoredUrl u) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO urls (url, category, tags, isUp, responseTime, sslDays, lastChecked, history) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getUrl());
            ps.setString(2, u.getCategory());
            ps.setString(3, String.join("|", u.getTags()));
            ps.setInt(4, u.isUp() ? 1 : 0);
            ps.setInt(5, u.getResponseTime());
            ps.setInt(6, u.getSslDays());
            ps.setLong(7, u.getLastChecked());
            ps.setString(8, "");

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) u.setId(keys.getInt(1));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUrl(int id, MonitoredUrl u) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE urls SET url=?, category=?, tags=?, isUp=?, responseTime=?, sslDays=?, lastChecked=?, history=? WHERE id=?")) {

            ps.setString(1, u.getUrl());
            ps.setString(2, u.getCategory());
            ps.setString(3, String.join("|", u.getTags()));
            ps.setInt(4, u.isUp() ? 1 : 0);
            ps.setInt(5, u.getResponseTime());
            ps.setInt(6, u.getSslDays());
            ps.setLong(7, u.getLastChecked());
            ps.setString(8, String.join(",", u.getHistoryResponseTimes().stream().map(String::valueOf).toList()));
            ps.setInt(9, id);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteUrl(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM urls WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,url,category,tags,isUp,responseTime,sslDays,lastChecked\n");
        for (MonitoredUrl u : getAllUrls()) {
            sb.append(u.getId()).append(",")
              .append(u.getUrl()).append(",")
              .append(u.getCategory() == null ? "" : u.getCategory()).append(",")
              .append(String.join("|", u.getTags())).append(",")
              .append(u.isUp()).append(",")
              .append(u.getResponseTime()).append(",")
              .append(u.getSslDays()).append(",")
              .append(u.getLastChecked()).append("\n");
        }
        return sb.toString();
    }
}
