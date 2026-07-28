package com.fobsslmonitor;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    private static Properties props = new Properties();

    public static void load() {
        try (FileInputStream fis = new FileInputStream("resources/config.properties")) {
            props.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getPollIntervalHours() {
        return Integer.parseInt(props.getProperty("poll.interval.hours", "6"));
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
