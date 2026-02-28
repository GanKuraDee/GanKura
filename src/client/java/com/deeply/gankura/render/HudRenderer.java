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
import net.minecraft.util.Identifier;

public class HudRenderer {

    // ★変更: メインのrenderメソッド内でのみ拡大縮小を行うように変更
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.world == null) return;
        if (client.currentScreen instanceof HudEditorScreen) return;

        if (!"SKYBLOCK".equals(GameState.gametype)) return;

        // --- Pet HUD ---
        if (ModConfig.showPetHud) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) HudConfig.petX, (float) HudConfig.petY); // 先に座標へ移動
            context.getMatrices().scale(HudConfig.petScale, HudConfig.petScale); // 拡大縮小
            renderPetHud(context, client.textRenderer, 0, 0, false); // 0,0 を起点に描画
            context.getMatrices().popMatrix();
        }

        // --- Armor Stack HUD ---
        if (ModConfig.showArmorStackHud) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) HudConfig.armorStackX, (float) HudConfig.armorStackY);
            context.getMatrices().scale(HudConfig.armorStackScale, HudConfig.armorStackScale);
            renderArmorStackHud(context, client.textRenderer, 0, 0, false);
            context.getMatrices().popMatrix();
        }

        boolean isTargetMap = "The End".equals(GameState.map) || "Combat 3".equals(GameState.mode);
        if (!isTargetMap) return;

        // --- Golem Status HUD ---
        if (ModConfig.showGolemStatusHud) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) HudConfig.statsX, (float) HudConfig.statsY);
            context.getMatrices().scale(HudConfig.statsScale, HudConfig.statsScale);
            renderStats(context, client.textRenderer, 0, 0, false);
            context.getMatrices().popMatrix();
        }

        // --- Loot Tracker HUD ---
        if (ModConfig.showLootTrackerHud) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) HudConfig.trackerX, (float) HudConfig.trackerY);
            context.getMatrices().scale(HudConfig.trackerScale, HudConfig.trackerScale);
            renderTracker(context, client.textRenderer, 0, 0);
            context.getMatrices().popMatrix();
        }

        // --- Golem HP HUD ---
        if (ModConfig.showGolemHealthHud && GameState.golemHealth != null) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) HudConfig.healthX, (float) HudConfig.healthY);
            context.getMatrices().scale(HudConfig.healthScale, HudConfig.healthScale);
            renderHealth(context, client.textRenderer, 0, 0, false);
            context.getMatrices().popMatrix();
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

                // --- ★Devonian式 厳密同期ロジック ---

                // 1. 最後にパケットが届いてから、現実で何秒経ったか？
                long timeSincePacket = System.currentTimeMillis() - GameState.lastServerPacketArrivalMillis;

                // 2. もし1.5秒以上パケットが来ていないなら、ラグとみなして時間を止める (Clamp)
                //    (通常は1秒ごとに来るはず)
                if (timeSincePacket > 1000) {
                    timeSincePacket = 1000;
                }

                // 3. 現在の推定サーバー時刻 = パケットの時刻 + 経過時間(Tick換算)
                //    (timeSincePacket が 1500 で止まれば、この時刻も止まる)
                double estimatedServerTime = GameState.lastServerTimePacket + (timeSincePacket / 50.0);

                // 4. カウントダウン計算
                double remainingTicks = GameState.stage5TargetTime - estimatedServerTime;

                if (remainingTicks < 0) remainingTicks = 0;

                if (remainingTicks > 0) {
                    displayStats = String.format("Stage: 5 (%.1fs)", remainingTicks / 20.0);
                    color = 0xFFFF5555; // カウントダウン中は赤色
                } else {
                    if (!GameState.hasGolemRisen && !"None".equals(GameState.locationName)) {
                        displayStats = "Stage: 5 (Soon)";
                        // ★変更: (Soon) の時は黄色 (ARGB: 0xFFFFFF55)
                        color = 0xFFFFFF55;
                    } else {
                        displayStats = "Stage: 5 (Spawned)";
                        color = 0xFFFF5555; // Spawned の時は赤色
                    }
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

        // LocationとStage4タイマーの描画 (変更なし)
        renderLocationAndTimer(context, tr, x, y, isPreview);
    }

    private static void renderLocationAndTimer(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
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

    public static void renderHealth(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        if (!isPreview && GameState.golemHealth == null) return;

        String title = "§c§lGolem HP";
        String hpText;

        if (isPreview) {
            hpText = "§e2.4M§f/§a5M";
        } else {
            String raw = GameState.golemHealth;
            String[] parts = raw.split("/");

            if (parts.length == 2) {
                double current = parseHealthValue(parts[0]);
                double max = parseHealthValue(parts[1]);

                String colorCode = "§a";

                if (current >= 0 && max > 0) {
                    if (current < 1_000_000) {
                        colorCode = "§c";
                    } else if (current < (max / 2.0)) {
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

    // クラスの最後に以下のメソッドを追加
    public static void renderPetHud(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§e§lActive Pet";
        String petText;

        // ★変更: プレビュー画面かどうかにかかわらず、常に現在のペット状況をそのまま表示する
        if (GameState.activePetName != null) {
            petText = GameState.activePetName;
        } else {
            petText = "§7None";
        }

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, petText, x, y + 12, 0xFFFFFFFF);
    }

    // アーマースタックHUDの描画メソッド (中央揃え・プレビュー全表示)
    public static void renderArmorStackHud(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        int spacing = 8; // テキスト同士の隙間

        // 描画するテキストのリストを作成
        java.util.List<String> parts = new java.util.ArrayList<>();

        if (isPreview) {
            // プレビュー時は5つすべてを様々なパターンでテスト表示
            parts.add("§6§l10ᝐ"); // Crimson (太字)
            parts.add("§15⁑");     // Terror (通常)
            parts.add("§e§l8⚶");   // Hollow (太字)
            parts.add("§23҉");     // Fervor (通常)
            parts.add("§9§l2Ѫ");   // Aurora (太字)
        } else {
            // 実際のデータに基づいてリストに追加
            if (GameState.crimsonStack > 0) parts.add((GameState.isCrimsonBold ? "§6§l" : "§6") + GameState.crimsonStack + "ᝐ");
            if (GameState.terrorStack > 0) parts.add((GameState.isTerrorBold ? "§1§l" : "§1") + GameState.terrorStack + "⁑");
            if (GameState.hollowStack > 0) parts.add((GameState.isHollowBold ? "§e§l" : "§e") + GameState.hollowStack + "⚶");
            if (GameState.fervorStack > 0) parts.add((GameState.isFervorBold ? "§2§l" : "§2") + GameState.fervorStack + "҉");
            if (GameState.auroraStack > 0) parts.add((GameState.isAuroraBold ? "§9§l" : "§9") + GameState.auroraStack + "Ѫ");
        }

        // 表示するものがなければここで終了
        if (parts.isEmpty()) return;

        // 全体の描画幅を事前に計算
        int totalWidth = 0;
        for (String part : parts) {
            totalWidth += tr.getWidth(part);
        }
        totalWidth += (parts.size() - 1) * spacing;

        // HUDエディターの当たり判定の幅(150px)を基準にして、その中央から描画を開始するロジック
        int boxWidth = 150;
        int currentX = x + (boxWidth / 2) - (totalWidth / 2);

        // リストの順番通りに横へ並べて描画していく
        for (String part : parts) {
            context.drawTextWithShadow(tr, part, currentX, y, 0xFFFFFFFF);
            currentX += tr.getWidth(part) + spacing;
        }
    }
}