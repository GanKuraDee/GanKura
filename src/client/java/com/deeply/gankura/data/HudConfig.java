package com.deeply.gankura.data;

import com.deeply.gankura.render.HudElement;
import com.deeply.gankura.render.hud.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class HudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraHudConfig");
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("gankura_hud.properties")
            .toFile();
    private static final Properties properties = new Properties();

    // 座標の保存形式。画面左上からの絶対座標 + 描画時クランプ形式。
    // これが一致しない設定ファイルは互換性がないものとして破棄する
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final String FORMAT_VERSION = "9";

    public static final List<HudElement> ELEMENTS = new ArrayList<>();

    static {
        initElements();
        load();
    }

    // ★神クラスの解体により、追加がたったの1行で可能に！
    private static void initElements() {
        ELEMENTS.add(new GolemStatusHud());
        ELEMENTS.add(new GolemLootTrackerHud());
        ELEMENTS.add(new GolemHealthHud());
        ELEMENTS.add(new DragonStatusHud());
        ELEMENTS.add(new DragonLootTrackerHud()); // ここでバグも修正！
        ELEMENTS.add(new DragonHealthHud());
        ELEMENTS.add(new BroodmotherStatusHud());
        ELEMENTS.add(new BroodmotherHealthHud());
        ELEMENTS.add(new ArachneStatusHud());
        ELEMENTS.add(new ArachneHealthHud());
        ELEMENTS.add(new CrimsonIsleStatusHud());
        ELEMENTS.add(new CrimsonLootTrackerHud());
        ELEMENTS.add(new CrimsonBossesHealthHud());
        ELEMENTS.add(new CritterCaptureHud());
        ELEMENTS.add(new DoomspiralHud());

        ELEMENTS.add(new PetHud());
        ELEMENTS.add(new ArmorStackHud());
        ELEMENTS.add(new DayHud());
        ELEMENTS.add(new WarpCooldownHud());
        ELEMENTS.add(new TpsHud());
        ELEMENTS.add(new EquipmentHud());
        ELEMENTS.add(new GearHud());
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);

            // 保存形式が変わった設定ファイルは読み込まず、既定値から作り直す
            if (!FORMAT_VERSION.equals(properties.getProperty(FORMAT_VERSION_KEY))) {
                LOGGER.info("HUD config format changed; resetting HUD positions to defaults");
                properties.clear();
                resetToDefault();
                save();
                return;
            }

            for (HudElement element : ELEMENTS) {
                element.x = parseInt(properties.getProperty(element.id + "X"), element.defaultX);
                element.y = parseInt(properties.getProperty(element.id + "Y"), element.defaultY);
                element.scale = parseFloat(properties.getProperty(element.id + "Scale"), element.defaultScale);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load HUD config", e);
        }
    }

    public static void save() {
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        properties.setProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
        for (HudElement element : ELEMENTS) {
            properties.setProperty(element.id + "X", String.valueOf(element.x));
            properties.setProperty(element.id + "Y", String.valueOf(element.y));
            properties.setProperty(element.id + "Scale", String.valueOf(element.scale));
        }

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura HUD Positions and Scales");
        } catch (IOException e) {
            LOGGER.error("Failed to save HUD config", e);
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException | NullPointerException e) { return def; }
    }

    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (NumberFormatException | NullPointerException e) { return def; }
    }

    public static void resetToDefault() {
        for (HudElement element : ELEMENTS) {
            element.reset();
        }
    }
}