package com.uptime;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    private static Properties props = new Properties();

    public static void load() {
        try (FileInputStream fis = new FileInputStream("src/main/resources/config.properties")) {
            props.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
