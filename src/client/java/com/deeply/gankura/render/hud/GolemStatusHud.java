package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class GolemStatusHud extends HudElement {
    public GolemStatusHud() {
        super("stats", 10, 10, 1.0f, 150, 36,
                () -> ModConfig.INSTANCE.theEnd.showGolemStatusHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String displayStats;

        if (isPreview) {
            displayStats = "Stage: §e4 §f(2m 30s)";
        } else {
            String stage = GameState.Golem.stage;
            if (GameState.Golem.isScanning) {
                displayStats = "Stage: §8Scanning...";
            } else if (ModConstants.STAGE_SUMMONED.equals(stage)) {
                long timeSincePacket = Math.min(System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis, 1000);
                double remainingTicks = Math.max(0, GameState.Golem.stage5TargetTime - (GameState.Server.lastTimePacket + (timeSincePacket / 50.0)));

                if (remainingTicks > 0) displayStats = String.format("§cStage: 5 (%.1fs)", remainingTicks / 20.0);
                else displayStats = (!GameState.Golem.hasRisen && !"None".equals(GameState.Player.locationName)) ? "§cStage: 5 §e(Soon)" : "§cStage: 5 (Spawned)";
            } else {
                String num = switch (stage) {
                    case ModConstants.STAGE_RESTING -> "§f0";
                    case ModConstants.STAGE_DORMANT -> "§f1";
                    case ModConstants.STAGE_AGITATED -> "§f2";
                    case ModConstants.STAGE_DISTURBED -> "§f3";
                    case ModConstants.STAGE_AWAKENING -> "§e4";
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;
                if (ModConstants.STAGE_AWAKENING.equals(stage) && GameState.Golem.stage4StartTime > 0) {
                    long seconds = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                    String col = seconds >= 480 ? "§c" : (seconds >= 240 ? "§e" : "§f");
                    displayStats += String.format(" %s(%dm %ds)", col, seconds / 60, seconds % 60);
                }
            }
        }

        context.drawTextWithShadow(tr, "§lGolem Status", 0, 0, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, 0, 12, 0xFFFFFFFF);

        String locText = null;
        if (isPreview) {
            locText = "Location: §fMiddle Front";
        } else if (ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage) || ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage)) {
            if ("None".equals(GameState.Player.locationName)) {
                locText = "Location: §8Scanning...";
            } else if (ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage)) {
                locText = "§cLocation: " + GameState.Player.locationName;
            } else {
                locText = "Location: §f" + GameState.Player.locationName;
            }
        }

        if (locText != null) context.drawTextWithShadow(tr, locText, 0, 24, 0xFFFFFFFF);
    }
}