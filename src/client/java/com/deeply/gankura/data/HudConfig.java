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
    public static int healthX = 260;
    public static int healthY = 150;
    public static int petX = 10;
    public static int petY = 10;
    public static int armorStackX = 10;
    public static int armorStackY = 50;

    // ★追加: 各HUDのスケール(倍率)
    public static float statsScale = 1.0f;
    public static float trackerScale = 1.0f;
    public static float healthScale = 1.0f;
    public static float petScale = 1.0f;
    public static float armorStackScale = 1.0f;

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
            healthX = parseInt(properties.getProperty("healthX"), 260);
            healthY = parseInt(properties.getProperty("healthY"), 150);
            petX = parseInt(properties.getProperty("petX"), 10);
            petY = parseInt(properties.getProperty("petY"), 10);
            armorStackX = parseInt(properties.getProperty("armorStackX"), 10);
            armorStackY = parseInt(properties.getProperty("armorStackY"), 50);

            // ★追加: スケールの読み込み
            statsScale = parseFloat(properties.getProperty("statsScale"), 1.0f);
            trackerScale = parseFloat(properties.getProperty("trackerScale"), 1.0f);
            healthScale = parseFloat(properties.getProperty("healthScale"), 1.0f);
            petScale = parseFloat(properties.getProperty("petScale"), 1.0f);
            armorStackScale = parseFloat(properties.getProperty("armorStackScale"), 1.0f);
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
        properties.setProperty("healthX", String.valueOf(healthX));
        properties.setProperty("healthY", String.valueOf(healthY));
        properties.setProperty("petX", String.valueOf(petX));
        properties.setProperty("petY", String.valueOf(petY));
        properties.setProperty("armorStackX", String.valueOf(armorStackX));
        properties.setProperty("armorStackY", String.valueOf(armorStackY));

        // ★追加: スケールの保存
        properties.setProperty("statsScale", String.valueOf(statsScale));
        properties.setProperty("trackerScale", String.valueOf(trackerScale));
        properties.setProperty("healthScale", String.valueOf(healthScale));
        properties.setProperty("petScale", String.valueOf(petScale));
        properties.setProperty("armorStackScale", String.valueOf(armorStackScale));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura HUD Positions and Scales");
        } catch (IOException e) {
            LOGGER.error("Failed to save HUD config", e);
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException | NullPointerException e) { return def; }
    }

    // ★追加: 小数点(スケール)読み込み用のメソッド
    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (NumberFormatException | NullPointerException e) { return def; }
    }

    // ★追加: HUDの座標とスケールを初期値にリセットするメソッド
    public static void resetToDefault() {
        statsX = 260;
        statsY = 50;
        statsScale = 1.0f;

        trackerX = 260;
        trackerY = 100;
        trackerScale = 1.0f;

        healthX = 260;
        healthY = 150;
        healthScale = 1.0f;

        petX = 10;
        petY = 10;
        petScale = 1.0f;

        armorStackX = 10;
        armorStackY = 50;
        armorStackScale = 1.0f;
    }
}