package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class BroodmotherStatusHud extends HudElement {
    public BroodmotherStatusHud() {
        super("broodmother", 10, 124, 1.0f, 150, 24,
                () -> ModConfig.INSTANCE.spidersDen.showBroodmotherStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String displayStats;

        if (isPreview) {
            displayStats = "Stage: §e4 §f(0m 45s)";
        } else {
            String stage = GameState.Broodmother.stage;

            if ("Alive!".equals(stage)) {
                displayStats = "§cStage: 5 (Spawned)";
            } else if ("Scanning...".equals(stage)) {
                displayStats = "Stage: §8Scanning...";
            } else {
                String num = switch (stage) {
                    case "Slain" -> "§70";
                    case "Dormant" -> "§71";
                    case "Soon" -> "§72";
                    case "Awakening" -> "§73";
                    case "Imminent" -> "§e4";
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;

                if ("Imminent".equals(stage) && GameState.Broodmother.stage4StartTime > 0) {
                    long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;
                    String col = seconds >= 45 ? "§c" : (seconds >= 30 ? "§e" : "§f");
                    displayStats += String.format(" %s(%dm %ds)", col, seconds / 60, seconds % 60);
                }
            }
        }

        drawTextWithShadow(context, tr, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF);
        drawTextWithShadow(context, tr, displayStats, 0, 12, 0xFFFFFFFF);
    }
}