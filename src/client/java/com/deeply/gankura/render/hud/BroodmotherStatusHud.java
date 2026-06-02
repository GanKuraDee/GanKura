package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class BroodmotherStatusHud extends HudElement {
    public BroodmotherStatusHud() {
        super("broodmother", 10, 50, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.spidersDen.showBroodmotherStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        // Minecraft.getInstance().font を使用
        Font font = Minecraft.getInstance().font;
        String displayStats; String timerText = null;

        if (isPreview) {
            displayStats = "Stage: §e4"; timerText = "Since S4: §e0m 45s §7(Max 1m)";
        } else {
            String stage = GameState.Broodmother.stage;

            if ("Alive!".equals(stage)) {
                displayStats = "§cStage: 5 (Spawned)";
            }
            else if ("Scanning...".equals(stage)) {
                displayStats = "Stage: §8Scanning...";
            }
            else {
                String num = switch (stage) {
                    case "Slain" -> "§f0";
                    case "Dormant" -> "§f1";
                    case "Soon" -> "§f2";
                    case "Awakening" -> "§f3";
                    case "Imminent" -> "§e4";
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;

                if ("Imminent".equals(stage) && GameState.Broodmother.stage4StartTime > 0) {
                    long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;
                    String colorCode = seconds >= 45 ? "§c" : (seconds >= 30 ? "§e" : "§f");
                    timerText = String.format("Since S4: %s%dm %ds §7(Max 1m)", colorCode, seconds / 60, seconds % 60);
                }
            }
        }

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, displayStats, 0, 12, 0xFFFFFFFF, true);

        if (timerText != null && ModConfig.INSTANCE.spidersDen.showBroodmotherStatusHud_SinceS4) {
            graphics.text(font, timerText, 0, 24, 0xFFFFFFFF, true);
        }
    }
}