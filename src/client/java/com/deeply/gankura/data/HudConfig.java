package com.deeply.gankura.data;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class HudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraHudConfig");
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("gankura_hud.properties")
            .toFile();
    private static final Properties properties = new Properties();

    // デフォルト座標
    public static int statsX = 260;
    public static int statsY = 50;

    public static int trackerX = 260;
    public static int trackerY = 100;

    // ★追加: 体力HUDの座標
    public static int healthX = 260;
    public static int healthY = 150;
    static {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            statsX = parseInt(properties.getProperty("statsX"), 260);
            statsY = parseInt(properties.getProperty("statsY"), 50);
            trackerX = parseInt(properties.getProperty("trackerX"), 260);
            trackerY = parseInt(properties.getProperty("trackerY"), 100);
            // ★追加
            healthX = parseInt(properties.getProperty("healthX"), 260);
            healthY = parseInt(properties.getProperty("healthY"), 150);
        } catch (IOException e) {
            LOGGER.error("Failed to load HUD config", e);
        }
    }

    public static void save() {
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        properties.setProperty("statsX", String.valueOf(statsX));
        properties.setProperty("statsY", String.valueOf(statsY));
        properties.setProperty("trackerX", String.valueOf(trackerX));
        properties.setProperty("trackerY", String.valueOf(trackerY));
        // ★追加
        properties.setProperty("healthX", String.valueOf(healthX));
        properties.setProperty("healthY", String.valueOf(healthY));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura HUD Positions");
        } catch (IOException e) {
            LOGGER.error("Failed to save HUD config", e);
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}