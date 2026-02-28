package com.deeply.gankura.data;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraConfig");
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("gankura_config.properties")
            .toFile();
    private static final Properties properties = new Properties();

    // 設定項目 (デフォルトは全て true)
    public static boolean showGolemStatusHud = true;
    public static boolean showLootTrackerHud = true;
    public static boolean showGolemHealthHud = true;
    public static boolean enableStageAlerts = true;
    public static boolean enableDropAlerts = true;
    public static boolean showStage4Duration = true;
    public static boolean showDpsChat = true;
    public static boolean showLootQualityChat = true;
    public static boolean showGolemWorldText = true;

    // ★追加: ドラゴン出現通知の設定
    public static boolean enableDragonAlerts = true;

    // ★追加: Pet HUDの表示設定
    public static boolean showPetHud = true;

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
            showGolemStatusHud = parseBool("showGolemStatusHud", true);
            showLootTrackerHud = parseBool("showLootTrackerHud", true);
            showGolemHealthHud = parseBool("showGolemHealthHud", true);
            enableStageAlerts = parseBool("enableStageAlerts", true);
            enableDropAlerts = parseBool("enableDropAlerts", true);
            showStage4Duration = parseBool("showStage4Duration", true);
            showDpsChat = parseBool("showDpsChat", true);
            showLootQualityChat = parseBool("showLootQualityChat", true);
            showGolemWorldText = parseBool("showGolemWorldText", true);

            // ★追加: 読み込み処理
            enableDragonAlerts = parseBool("enableDragonAlerts", true);

            showPetHud = parseBool("showPetHud", true);
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        properties.setProperty("showGolemStatusHud", String.valueOf(showGolemStatusHud));
        properties.setProperty("showLootTrackerHud", String.valueOf(showLootTrackerHud));
        properties.setProperty("showGolemHealthHud", String.valueOf(showGolemHealthHud));
        properties.setProperty("enableStageAlerts", String.valueOf(enableStageAlerts));
        properties.setProperty("enableDropAlerts", String.valueOf(enableDropAlerts));
        properties.setProperty("showStage4Duration", String.valueOf(showStage4Duration));
        properties.setProperty("showDpsChat", String.valueOf(showDpsChat));
        properties.setProperty("showLootQualityChat", String.valueOf(showLootQualityChat));
        properties.setProperty("showGolemWorldText", String.valueOf(showGolemWorldText));

        // ★追加: 保存処理
        properties.setProperty("enableDragonAlerts", String.valueOf(enableDragonAlerts));

        properties.setProperty("showPetHud", String.valueOf(showPetHud));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura General Config");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static boolean parseBool(String key, boolean def) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(def)));
    }
}