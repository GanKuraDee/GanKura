package com.deeply.gankura.render;

import com.deeply.gankura.data.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class HudRenderer {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.world == null) return;

        // 設定画面を開いているときは、重複防止のためオーバーレイを非表示にする
        if (client.currentScreen instanceof HudEditorScreen) return;

        if (!ModConstants.GAME_TYPE_SKYBLOCK.equals(GameState.gametype)) return;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.map)
                || ModConstants.MODE_COMBAT_3.equals(GameState.mode);
        if (!isTargetMap) return;

        // Configの座標を使って描画
        if (ModConfig.showGolemStatusHud) {
            renderStats(context, client.textRenderer, HudConfig.statsX, HudConfig.statsY, false);
        }

        // ★変更: 設定がONのときだけ描画
        if (ModConfig.showLootTrackerHud) {
            renderTracker(context, client.textRenderer, HudConfig.trackerX, HudConfig.trackerY);
        }
    }

    // ★ Golem Status の描画メソッド (外部公開)
    // isPreview: trueの場合、設定画面用にダミーデータを表示
    public static void renderStats(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§lGolem Status";
        String displayStats = "Stage: Resting";
        int color = 0xFFFFFFFF;

        if (isPreview) {
            // プレビュー用データ
            displayStats = "Stage: 5 (Spawned)";
            color = 0xFFFF5555;
        } else {
            // 通常ロジック
            String stage = GameState.golemStage;
            if (GameState.isScanning) {
                displayStats = "Stage: Scanning...";
            } else if (ModConstants.STAGE_SUMMONED.equals(stage)) {
                long currentTime = MinecraftClient.getInstance().world.getTime();
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
        }

        // タイトルとステージ描画
        context.drawTextWithShadow(tr, title, x, y, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, x, y + 12, color);

        // Location 描画
        String locText = null;
        if (isPreview) {
            locText = "Location: Middle Front";
        } else {
            boolean showLoc = ModConstants.STAGE_AWAKENING.equals(GameState.golemStage)
                    || ModConstants.STAGE_SUMMONED.equals(GameState.golemStage);
            if (showLoc) {
                locText = "None".equals(GameState.locationName)
                        ? (ModConstants.STAGE_AWAKENING.equals(GameState.golemStage) ? "Location: Scanning..." : null)
                        : "Location: " + GameState.locationName;
            }
        }
        if (locText != null) {
            context.drawTextWithShadow(tr, locText, x, y + 24, 0xFFFFFFFF);
        }

        // Timer 描画 (Since S4)
        if (isPreview || (ModConstants.STAGE_AWAKENING.equals(GameState.golemStage) && GameState.stage4StartTime > 0)) {
            String timerText;
            if (isPreview) {
                timerText = "Since S4: 0m 45s";
            } else {
                long durationMillis = System.currentTimeMillis() - GameState.stage4StartTime;
                long seconds = durationMillis / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;

                // 添付ファイルのフォーマットに合わせました
                timerText = String.format("Since S4: %dm %ds", minutes, remainingSeconds);
            }
            context.drawTextWithShadow(tr, timerText, x, y + 36, 0xFFFFFFFF);
        }
    }

    // ★ Golem Loot Tracker の描画メソッド (外部公開)
    public static void renderTracker(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, "§6§lGolem Loot Tracker", x, y, 0xFFFFFFFF);

        String epicText = String.format("§5Golem §7(Pet): §f%d", LootStats.epicGolemPets);
        context.drawTextWithShadow(tr, epicText, x, y + 12, 0xFFFFFFFF);

        String legText = String.format("§6Golem §7(Pet): §f%d", LootStats.legendaryGolemPets);
        context.drawTextWithShadow(tr, legText, x, y + 24, 0xFFFFFFFF);

        String tbcText = String.format("§6Tier Boost Core: §f%d", LootStats.tierBoostCores);
        context.drawTextWithShadow(tr, tbcText, x, y + 36, 0xFFFFFFFF);
    }
}