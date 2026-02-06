package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class HudRenderer {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.world == null) return;

        if (!ModConstants.GAME_TYPE_SKYBLOCK.equals(GameState.gametype)) return;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.map)
                || ModConstants.MODE_COMBAT_3.equals(GameState.mode);
        if (!isTargetMap) return;

        renderStatus(context, client.textRenderer, tickCounter, client);
    }

    private static void renderStatus(DrawContext context, TextRenderer tr, RenderTickCounter tickCounter, MinecraftClient client) {
        String title = "End Stone Protector";
        String stage = GameState.golemStage;
        String displayStats;
        int color = 0xFFFFFFFF;

        if (GameState.isScanning) {
            displayStats = "Stage: Scanning...";
        } else if (ModConstants.STAGE_SUMMONED.equals(stage)) {
            // ★v1.0.1仕様: 単純なサーバー時間計算に戻す (補間なし)
            // world.getTime() はサーバーのTPSに合わせて進むため、ラグがある場合はゆっくりになります
            long currentTime = client.world.getTime();
            long remainingTicks = GameState.stage5TargetTime - currentTime;

            if (remainingTicks < 0) remainingTicks = 0;

            if (remainingTicks > 0) {
                displayStats = String.format("Stage: 5 (%.1fs)", remainingTicks / 20.0f);
                color = 0xFFFF5555;
            } else {
                displayStats = "Stage: 5 (Spawned)";
                color = 0xFFFF5555;
            }
        } else {
            String num = switch (stage) {
                case ModConstants.STAGE_RESTING -> "0";
                case ModConstants.STAGE_DORMANT -> "1";
                case ModConstants.STAGE_AGITATED -> "2";
                case ModConstants.STAGE_DISTURBED -> "3";
                case ModConstants.STAGE_AWAKENING -> "4";
                default -> "?";
            };
            displayStats = "Stage: " + num;
        }

        // 左揃え設定は維持
        int x = 260;
        int y = 50;

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, x, y + 12, color);

        boolean showLoc = ModConstants.STAGE_AWAKENING.equals(stage)
                || ModConstants.STAGE_SUMMONED.equals(stage);

        if (showLoc) {
            String locText = "None".equals(GameState.locationName)
                    ? (ModConstants.STAGE_AWAKENING.equals(stage) ? "Location: Scanning..." : null)
                    : "Location: " + GameState.locationName;

            if (locText != null) {
                context.drawTextWithShadow(tr, locText, x, y + 24, 0xFFFFFFFF);
            }
        }
    }
}