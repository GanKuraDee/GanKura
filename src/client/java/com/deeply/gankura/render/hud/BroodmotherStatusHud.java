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
        super("broodmother", 10, 50, 1.0f, 150, 50,
                () -> ModConfig.showBroodmotherStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String displayStats; String timerText = null;

        if (isPreview) {
            displayStats = "Stage: §f4"; timerText = "Since Imminent: §e0m 45s §7(Max 1m)";
        } else {
            String stage = GameState.Broodmother.stage;

            if ("Alive!".equals(stage)) {
                displayStats = "§cStage: 5 (Spawned)";
            }
            // ★修正: GameState.Golem.isScanning の条件を削除し、純粋にBroodmotherのステージ名だけで判定！
            else if ("Scanning...".equals(stage)) {
                displayStats = "Stage: §8Scanning...";
            }
            else {
                String num = switch (stage) {
                    case "Slain" -> "0";
                    case "Dormant" -> "1";
                    case "Soon" -> "2";
                    case "Awakening" -> "3";
                    case "Imminent" -> "4";
                    default -> "?";
                };
                displayStats = "Stage: §f" + num;

                if ("Imminent".equals(stage) && GameState.Broodmother.stage4StartTime > 0) {
                    long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;
                    String colorCode = seconds >= 45 ? "§c" : (seconds >= 30 ? "§e" : "§f");
                    timerText = String.format("Since S4: %s%dm %ds §7(Max 1m)", colorCode, seconds / 60, seconds % 60);
                }
            }
        }

        context.drawTextWithShadow(tr, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, displayStats, 0, 12, 0xFFFFFFFF);
        if (timerText != null) context.drawTextWithShadow(tr, timerText, 0, 24, 0xFFFFFFFF);
    }
}