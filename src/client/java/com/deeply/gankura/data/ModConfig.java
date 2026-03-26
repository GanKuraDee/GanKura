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

    // 設定項目 (Golem)
    public static boolean showGolemStatusHud = true;
    public static boolean showLootTrackerHud = true;
    public static boolean showGolemHealthHud = true;
    public static boolean enableStageAlerts = true;
    public static boolean enableDropAlerts = true;
    public static boolean showStage4Duration = true;
    public static boolean showDpsChat = true;
    public static boolean showLootQualityChat = true;
    public static boolean showGolemWorldText = true;
    public static boolean enableDay30Alert = true;
    public static boolean enableGolemHighlight = true; // ★追加: ゴーレムハイライト

    // Dragon
    public static boolean enableDragonAlerts = true;
    public static boolean enableDragonDropAlerts = true;
    public static boolean showDragonStatusHud = true;
    public static boolean showDragonDpsChat = true;
    public static boolean showDragonLootQualityChat = true;
    public static boolean showDragonTrackerHud = true;

    // Spider's Den
    public static boolean showBroodmotherStatusHud = true;
    public static boolean showBroodmotherHealthHud = true;
    public static boolean enableBroodmotherAlerts = true;
    public static boolean showBroodmotherStage4Duration = true;
    public static boolean enableBroodmotherHighlight = true; // ★追加: ブルードマザーハイライト

    // Misc
    public static boolean showPetHud = false;
    public static boolean showArmorStackHud = false;
    public static boolean enableRebootAlert = true;
    public static boolean showDayHud = false;
    public static boolean showPoisonIndicator = false;

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

            // End Stone Protector
            showGolemStatusHud = parseBool("showGolemStatusHud", true);
            showLootTrackerHud = parseBool("showLootTrackerHud", true);
            showGolemHealthHud = parseBool("showGolemHealthHud", true);
            enableStageAlerts = parseBool("enableStageAlerts", true);
            enableDropAlerts = parseBool("enableDropAlerts", true);
            showStage4Duration = parseBool("showStage4Duration", true);
            showDpsChat = parseBool("showDpsChat", true);
            showLootQualityChat = parseBool("showLootQualityChat", true);
            showGolemWorldText = parseBool("showGolemWorldText", true);
            enableDay30Alert = parseBool("enableDay30Alert", true);
            enableGolemHighlight = parseBool("enableGolemHighlight", true); // ★追加

            // Dragon
            enableDragonAlerts = parseBool("enableDragonAlerts", true);
            enableDragonDropAlerts = parseBool("enableDragonDropAlerts", true);
            showDragonStatusHud = parseBool("showDragonStatusHud", true);
            showDragonDpsChat = parseBool("showDragonDpsChat", true);
            showDragonLootQualityChat = parseBool("showDragonLootQualityChat", true);
            showDragonTrackerHud = parseBool("showDragonTrackerHud", true);

            // Spider's Den
            showBroodmotherStatusHud = parseBool("showBroodmotherStatusHud", true);
            showBroodmotherHealthHud = parseBool("showBroodmotherHealthHud", true);
            enableBroodmotherAlerts = parseBool("enableBroodmotherAlerts", true);
            showBroodmotherStage4Duration = parseBool("showBroodmotherStage4Duration", true);
            enableBroodmotherHighlight = parseBool("enableBroodmotherHighlight", true); // ★追加

            // Misc
            showPetHud = parseBool("showPetHud", false);
            showArmorStackHud = parseBool("showArmorStackHud", false);
            enableRebootAlert = parseBool("enableRebootAlert", true);
            showDayHud = parseBool("showDayHud", false);
            showPoisonIndicator = parseBool("showPoisonIndicator", false);
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        // End Stone Protector
        properties.setProperty("showGolemStatusHud", String.valueOf(showGolemStatusHud));
        properties.setProperty("showLootTrackerHud", String.valueOf(showLootTrackerHud));
        properties.setProperty("showGolemHealthHud", String.valueOf(showGolemHealthHud));
        properties.setProperty("enableStageAlerts", String.valueOf(enableStageAlerts));
        properties.setProperty("enableDropAlerts", String.valueOf(enableDropAlerts));
        properties.setProperty("showStage4Duration", String.valueOf(showStage4Duration));
        properties.setProperty("showDpsChat", String.valueOf(showDpsChat));
        properties.setProperty("showLootQualityChat", String.valueOf(showLootQualityChat));
        properties.setProperty("showGolemWorldText", String.valueOf(showGolemWorldText));
        properties.setProperty("enableDay30Alert", String.valueOf(enableDay30Alert));
        properties.setProperty("enableGolemHighlight", String.valueOf(enableGolemHighlight)); // ★追加

        // Dragon
        properties.setProperty("enableDragonAlerts", String.valueOf(enableDragonAlerts));
        properties.setProperty("enableDragonDropAlerts", String.valueOf(enableDragonDropAlerts));
        properties.setProperty("showDragonStatusHud", String.valueOf(showDragonStatusHud));
        properties.setProperty("showDragonDpsChat", String.valueOf(showDragonDpsChat));
        properties.setProperty("showDragonLootQualityChat", String.valueOf(showDragonLootQualityChat));
        properties.setProperty("showDragonTrackerHud", String.valueOf(showDragonTrackerHud));

        // Spider's Den
        properties.setProperty("showBroodmotherStatusHud", String.valueOf(showBroodmotherStatusHud));
        properties.setProperty("showBroodmotherHealthHud", String.valueOf(showBroodmotherHealthHud));
        properties.setProperty("enableBroodmotherAlerts", String.valueOf(enableBroodmotherAlerts));
        properties.setProperty("showBroodmotherStage4Duration", String.valueOf(showBroodmotherStage4Duration));
        properties.setProperty("enableBroodmotherHighlight", String.valueOf(enableBroodmotherHighlight)); // ★追加

        // Misc
        properties.setProperty("showPetHud", String.valueOf(showPetHud));
        properties.setProperty("showArmorStackHud", String.valueOf(showArmorStackHud));
        properties.setProperty("enableRebootAlert", String.valueOf(enableRebootAlert));
        properties.setProperty("showDayHud", String.valueOf(showDayHud));
        properties.setProperty("showPoisonIndicator", String.valueOf(showPoisonIndicator));

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