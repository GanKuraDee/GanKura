package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.HudConfig;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
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
        if (ModConfig.showLootTrackerHud) {
            renderTracker(context, client.textRenderer, HudConfig.trackerX, HudConfig.trackerY);
        }
        if (ModConfig.showGolemHealthHud) {
            renderHealth(context, client.textRenderer, HudConfig.healthX, HudConfig.healthY, false);
        }
    }

    public static void renderStats(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§lGolem Status";
        String displayStats = "Stage: Resting";
        int color = 0xFFFFFFFF;

        if (isPreview) {
            displayStats = "Stage: 5 (Spawned)";
            color = 0xFFFF5555;
        } else {
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

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, x, y + 12, color);

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

        if (isPreview || (ModConstants.STAGE_AWAKENING.equals(GameState.golemStage) && GameState.stage4StartTime > 0)) {
            String timerText;
            if (isPreview) {
                timerText = "Since S4: 0m 45s";
            } else {
                long durationMillis = System.currentTimeMillis() - GameState.stage4StartTime;
                long seconds = durationMillis / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;
                timerText = String.format("Since S4: %dm %ds", minutes, remainingSeconds);
            }
            context.drawTextWithShadow(tr, timerText, x, y + 36, 0xFFFFFFFF);
        }
    }

    public static void renderTracker(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, "§6§lGolem Loot Tracker", x, y, 0xFFFFFFFF);

        String epicText = String.format("§5Golem §7(Pet): §f%d", LootStats.epicGolemPets);
        context.drawTextWithShadow(tr, epicText, x, y + 12, 0xFFFFFFFF);

        String legText = String.format("§6Golem §7(Pet): §f%d", LootStats.legendaryGolemPets);
        context.drawTextWithShadow(tr, legText, x, y + 24, 0xFFFFFFFF);

        String tbcText = String.format("§6Tier Boost Core: §f%d", LootStats.tierBoostCores);
        context.drawTextWithShadow(tr, tbcText, x, y + 36, 0xFFFFFFFF);
    }

    // Golem HP 描画メソッド
    public static void renderHealth(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        // ★修正: プレビューでなく、かつ体力データがない場合は即終了（非表示。Waiting...も出さない）
        if (!isPreview && GameState.golemHealth == null) return;

        String title = "§c§lGolem HP";
        String hpText; // 初期値なし

        if (isPreview) {
            hpText = "§e2.4M§f/§a5M";
        } else {
            // ここに来る時点で golemHealth は null ではない
            String raw = GameState.golemHealth;
            String[] parts = raw.split("/");

            if (parts.length == 2) {
                double current = parseHealthValue(parts[0]);
                double max = parseHealthValue(parts[1]);

                String colorCode = "§a"; // デフォルト: 緑

                if (current >= 0 && max > 0) {
                    // 優先順位1: 残り1M未満 -> 赤
                    if (current < 1_000_000) {
                        colorCode = "§c";
                    }
                    // 優先順位2: 最大体力の半分未満 -> 黄
                    else if (current < (max / 2.0)) {
                        colorCode = "§e";
                    }
                }

                hpText = colorCode + parts[0] + "§f/§a" + parts[1];
            } else {
                hpText = "§a" + raw.replace("/", "§f/§a");
            }
        }

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, hpText, x, y + 12, 0xFFFFFFFF);
    }

    private static double parseHealthValue(String s) {
        try {
            s = s.trim();
            if (s.isEmpty()) return 0;

            double multiplier = 1.0;
            char last = s.charAt(s.length() - 1);

            if (last == 'M' || last == 'm') {
                multiplier = 1_000_000.0;
                s = s.substring(0, s.length() - 1);
            } else if (last == 'k' || last == 'K') {
                multiplier = 1_000.0;
                s = s.substring(0, s.length() - 1);
            }

            return Double.parseDouble(s) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}