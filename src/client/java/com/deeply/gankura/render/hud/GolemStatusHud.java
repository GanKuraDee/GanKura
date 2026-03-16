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
        super("stats", 260, 50, 1.0f, 150, 50,
                () -> ModConfig.showGolemStatusHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String displayStats;

        if (isPreview) {
            displayStats = "§cStage: 5 (Spawned)";
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
                // ★修正1: Stage 4の「4」を黄色(§e)にするように色分けを追加
                String num = switch (stage) {
                    case ModConstants.STAGE_RESTING -> "§f0";
                    case ModConstants.STAGE_DORMANT -> "§f1";
                    case ModConstants.STAGE_AGITATED -> "§f2";
                    case ModConstants.STAGE_DISTURBED -> "§f3";
                    case ModConstants.STAGE_AWAKENING -> "§e4"; // 黄色
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;
            }
        }

        context.drawTextWithShadow(tr, "§lGolem Status", 0, 0, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, 0, 12, 0xFFFFFFFF);

        // ★修正2: Locationテキストの生成。可読性向上のためif文に整理し、Stage 5の時は全体を赤色(§c)に。
        String locText = null;
        if (isPreview) {
            // プレビューもStage 5想定なので、全体のテーマに合わせて赤色で表示
            locText = "§cLocation: Middle Front";
        } else if (ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage) || ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage)) {
            if ("None".equals(GameState.Player.locationName)) {
                locText = "Location: §8Scanning...";
            } else if (ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage)) {
                locText = "§cLocation: " + GameState.Player.locationName; // Stage 5: 全体が赤色
            } else {
                locText = "Location: §f" + GameState.Player.locationName; // Stage 4: 通常色
            }
        }

        if (locText != null) context.drawTextWithShadow(tr, locText, 0, 24, 0xFFFFFFFF);

        if (isPreview || (ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage) && GameState.Golem.stage4StartTime > 0)) {
            String timerText;
            if (isPreview) timerText = "Since S4: §f0m 45s";
            else {
                long seconds = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                String colorCode = seconds >= 480 ? "§c" : (seconds >= 240 ? "§e" : "§f");
                timerText = String.format("Since S4: %s%dm %ds", colorCode, seconds / 60, seconds % 60);
            }
            context.drawTextWithShadow(tr, timerText, 0, 36, 0xFFFFFFFF);
        }
    }
}