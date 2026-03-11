package com.deeply.gankura.data;

import com.deeply.gankura.render.HudElement;
import com.deeply.gankura.render.HudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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

    // ★すべてのHUDをここで一括管理するリスト
    public static final List<HudElement> ELEMENTS = new ArrayList<>();

    static {
        initElements();
        load();
    }

    // The Endにいるかどうかの共通判定ロジック
    private static boolean isTheEnd() {
        return "The End".equals(GameState.map) || "Combat 3".equals(GameState.mode);
    }

    // ★今後新しいHUDを追加する時は、ここのリストに1つ追加するだけで完結します！
    private static void initElements() {
        ELEMENTS.add(new HudElement("stats", 260, 50, 1.0f, 150, 50,
                () -> ModConfig.showGolemStatusHud, HudConfig::isTheEnd) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderStats(context, MinecraftClient.getInstance().textRenderer, 0, 0, isPreview);
            }
        });

        ELEMENTS.add(new HudElement("tracker", 260, 100, 1.0f, 150, 50,
                () -> ModConfig.showLootTrackerHud, HudConfig::isTheEnd) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderTracker(context, MinecraftClient.getInstance().textRenderer, 0, 0);
            }
        });

        ELEMENTS.add(new HudElement("health", 260, 150, 1.0f, 100, 30,
                () -> ModConfig.showGolemHealthHud, () -> isTheEnd() && GameState.golemHealth != null) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderHealth(context, MinecraftClient.getInstance().textRenderer, 0, 0, isPreview);
            }
        });

        ELEMENTS.add(new HudElement("pet", 10, 10, 1.0f, 120, 30,
                () -> ModConfig.showPetHud, () -> true) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderPetHud(context, MinecraftClient.getInstance().textRenderer, 0, 0, isPreview);
            }
        });

        ELEMENTS.add(new HudElement("armorStack", 10, 50, 1.0f, 150, 15,
                () -> ModConfig.showArmorStackHud, () -> true) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderArmorStackHud(context, MinecraftClient.getInstance().textRenderer, 0, 0, isPreview);
            }
        });

        ELEMENTS.add(new HudElement("day", 10, 90, 1.0f, 60, 15,
                () -> ModConfig.showDayHud, () -> true) {
            @Override public void renderElement(DrawContext context, boolean isPreview) {
                HudRenderer.renderDayHud(context, MinecraftClient.getInstance(), MinecraftClient.getInstance().textRenderer, 0, 0);
            }
        });
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
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