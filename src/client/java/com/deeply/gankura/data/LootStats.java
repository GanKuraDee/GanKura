package com.deeply.gankura.data;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class LootStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraLootStats");

    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("loot_tracker.properties")
            .toFile();

    private static final Properties properties = new Properties();

    public static int epicGolemPets = 0;
    public static int legendaryGolemPets = 0;
    public static int tierBoostCores = 0;

    // ★追加: ドラゴンペットの取得数
    public static int epicDragonPets = 0;
    public static int legendaryDragonPets = 0;

    static {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // ファイルがない場合は作成へ
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            // 数値変換エラー対策を追加
            try { epicGolemPets = Integer.parseInt(properties.getProperty("epicGolemPets", "0")); } catch (NumberFormatException e) { epicGolemPets = 0; }
            try { legendaryGolemPets = Integer.parseInt(properties.getProperty("legendaryGolemPets", "0")); } catch (NumberFormatException e) { legendaryGolemPets = 0; }
            try { tierBoostCores = Integer.parseInt(properties.getProperty("tierBoostCores", "0")); } catch (NumberFormatException e) { tierBoostCores = 0; }

            // ★追加: ドラゴンペットの読み込み
            try { epicDragonPets = Integer.parseInt(properties.getProperty("epicDragonPets", "0")); } catch (NumberFormatException e) { epicDragonPets = 0; }
            try { legendaryDragonPets = Integer.parseInt(properties.getProperty("legendaryDragonPets", "0")); } catch (NumberFormatException e) { legendaryDragonPets = 0; }
        } catch (IOException e) {
            LOGGER.error("Failed to load loot stats", e);
        }
    }

    public static void save() {
        // 親フォルダ (config/gankura) が存在しない場合は作成する
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOGGER.error("Failed to create config directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        properties.setProperty("epicGolemPets", String.valueOf(epicGolemPets));
        properties.setProperty("legendaryGolemPets", String.valueOf(legendaryGolemPets));
        properties.setProperty("tierBoostCores", String.valueOf(tierBoostCores));

        // ★追加: ドラゴンペットの保存
        properties.setProperty("epicDragonPets", String.valueOf(epicDragonPets));
        properties.setProperty("legendaryDragonPets", String.valueOf(legendaryDragonPets));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura Golem and Dragon Loot Tracker");
        } catch (IOException e) {
            LOGGER.error("Failed to save loot stats", e);
        }
    }

    public static void addEpicGolemPet() {
        epicGolemPets++;
        save();
    }

    public static void addLegendaryGolemPet() {
        legendaryGolemPets++;
        save();
    }

    public static void addTierBoostCore() {
        tierBoostCores++;
        save();
    }

    // ★追加: ドラゴンペット追加用メソッド
    public static void addEpicDragonPet() {
        epicDragonPets++;
        save();
    }

    public static void addLegendaryDragonPet() {
        legendaryDragonPets++;
        save();
    }
}