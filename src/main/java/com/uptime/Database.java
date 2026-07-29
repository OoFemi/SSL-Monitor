package com.uptime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    private static final String URL = "jdbc:sqlite:data.db";

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
            return null;
        }
    }

    public static void initializeDatabase() {
        String createUrlsTable = "CREATE TABLE IF NOT EXISTS monitored_urls (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT NOT NULL, " +
                "active INTEGER DEFAULT 1)";

        String createStatusTable = "CREATE TABLE IF NOT EXISTS endpoint_status (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url_id INTEGER, " +
                "status TEXT, " +
                "response_time INTEGER, " +
                "ssl_expiry_days INTEGER, " +
                "checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String createAdminTable = "CREATE TABLE IF NOT EXISTS admin_config (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "logo_path TEXT)";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createUrlsTable);
            stmt.execute(createStatusTable);
            stmt.execute(createAdminTable);
            
            System.out.println("Database tables initialized successfully.");
        } catch (Exception e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }
}
