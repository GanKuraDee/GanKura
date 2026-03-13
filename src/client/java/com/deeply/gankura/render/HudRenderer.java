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

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.world == null) return;
        if (client.currentScreen instanceof HudEditorScreen) return;

        if (!"SKYBLOCK".equals(GameState.gametype)) return;

        for (HudElement element : com.deeply.gankura.data.HudConfig.ELEMENTS) {
            if (element.shouldRender(false)) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) element.x, (float) element.y);
                context.getMatrices().scale(element.scale, element.scale);
                element.renderElement(context, false);
                context.getMatrices().popMatrix();
            }
        }

        if (ModConfig.enableRebootAlert && GameState.isServerClosing && GameState.serverClosingTime != null) {
            renderServerClosingAlert(context, client, client.textRenderer);
        }
    }

    public static void renderStats(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§lGolem Status";
        String displayStats;

        if (isPreview) {
            // ★変更: Stage 5は「Stage: 」まで含めて全体を赤色(§c)にする
            displayStats = "§cStage: 5 (Spawned)";
        } else {
            String stage = GameState.golemStage;

            if (GameState.isScanning) {
                displayStats = "Stage: §8Scanning...";
            } else if (ModConstants.STAGE_SUMMONED.equals(stage)) {
                long timeSincePacket = System.currentTimeMillis() - GameState.lastServerPacketArrivalMillis;
                if (timeSincePacket > 1000) timeSincePacket = 1000;

                double estimatedServerTime = GameState.lastServerTimePacket + (timeSincePacket / 50.0);
                double remainingTicks = GameState.stage5TargetTime - estimatedServerTime;

                if (remainingTicks < 0) remainingTicks = 0;

                if (remainingTicks > 0) {
                    // ★変更: カウントダウン中は全体を赤(§c)
                    displayStats = String.format("§cStage: 5 (%.1fs)", remainingTicks / 20.0);
                } else {
                    if (!GameState.hasGolemRisen && !"None".equals(GameState.locationName)) {
                        // ★変更: Soon(間もなく)は全体を黄色(§e)
                        displayStats = "§eStage: 5 (Soon)";
                    } else {
                        // ★変更: Spawned(出現済み)は全体を赤(§c)
                        displayStats = "§cStage: 5 (Spawned)";
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
                displayStats = "Stage: §f" + num;
            }
        }

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFAA00);
        context.drawTextWithShadow(tr, displayStats, x, y + 12, 0xFFFFFFFF);

        renderLocationAndTimer(context, tr, x, y, isPreview);
    }

    private static void renderLocationAndTimer(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String locText = null;

        if (isPreview) {
            locText = "Location: §fMiddle Front";
        } else {
            boolean showLoc = ModConstants.STAGE_AWAKENING.equals(GameState.golemStage)
                    || ModConstants.STAGE_SUMMONED.equals(GameState.golemStage);
            if (showLoc) {
                if ("None".equals(GameState.locationName)) {
                    if (ModConstants.STAGE_AWAKENING.equals(GameState.golemStage)) {
                        locText = "Location: §8Scanning...";
                    }
                } else {
                    locText = "Location: §f" + GameState.locationName;
                }
            }
        }

        if (locText != null) {
            context.drawTextWithShadow(tr, locText, x, y + 24, 0xFFFFFFFF);
        }

        if (isPreview || (ModConstants.STAGE_AWAKENING.equals(GameState.golemStage) && GameState.stage4StartTime > 0)) {
            String timerText;
            if (isPreview) {
                timerText = "Since S4: §f0m 45s";
            } else {
                long durationMillis = System.currentTimeMillis() - GameState.stage4StartTime;
                long seconds = durationMillis / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;

                // ★追加: 経過時間に応じた色の切り替えロジック
                String colorCode = "§f"; // デフォルトは白
                if (seconds >= 480) { // 8分(480秒)以上で赤
                    colorCode = "§c";
                } else if (seconds >= 240) { // 4分(240秒)以上で黄色
                    colorCode = "§e";
                }

                // ★変更: 算出した色コード(colorCode)を数字部分に適用する
                timerText = String.format("Since S4: %s%dm %ds", colorCode, minutes, remainingSeconds);
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

    public static void renderPetHud(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§e§lActive Pet";
        String petText;

        if (GameState.activePetName != null) {
            petText = GameState.activePetName;
        } else {
            petText = "§7None";
        }

        context.drawTextWithShadow(tr, title, x, y, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, petText, x, y + 12, 0xFFFFFFFF);
    }

    public static void renderArmorStackHud(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        int spacing = 8;

        java.util.List<String> parts = new java.util.ArrayList<>();

        if (isPreview) {
            parts.add("§6§l10ᝐ");
            parts.add("§15⁑");
            parts.add("§e§l8⚶");
            parts.add("§23҉");
            parts.add("§9§l2Ѫ");
        } else {
            if (GameState.crimsonStack > 0) parts.add((GameState.isCrimsonBold ? "§6§l" : "§6") + GameState.crimsonStack + "ᝐ");
            if (GameState.terrorStack > 0) parts.add((GameState.isTerrorBold ? "§1§l" : "§1") + GameState.terrorStack + "⁑");
            if (GameState.hollowStack > 0) parts.add((GameState.isHollowBold ? "§e§l" : "§e") + GameState.hollowStack + "⚶");
            if (GameState.fervorStack > 0) parts.add((GameState.isFervorBold ? "§2§l" : "§2") + GameState.fervorStack + "҉");
            if (GameState.auroraStack > 0) parts.add((GameState.isAuroraBold ? "§9§l" : "§9") + GameState.auroraStack + "Ѫ");
        }

        if (parts.isEmpty()) return;

        int totalWidth = 0;
        for (String part : parts) {
            totalWidth += tr.getWidth(part);
        }
        totalWidth += (parts.size() - 1) * spacing;

        int boxWidth = 150;
        int currentX = x + (boxWidth / 2) - (totalWidth / 2);

        for (String part : parts) {
            context.drawTextWithShadow(tr, part, currentX, y, 0xFFFFFFFF);
            currentX += tr.getWidth(part) + spacing;
        }
    }

    private static void renderServerClosingAlert(DrawContext context, MinecraftClient client, TextRenderer tr) {
        String text = "Server closing: " + GameState.serverClosingTime;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int textWidth = tr.getWidth(text);

        context.getMatrices().pushMatrix();

        context.getMatrices().translate(screenWidth / 2f, screenHeight / 2f);
        context.getMatrices().scale(2.0f, 2.0f);

        context.drawTextWithShadow(tr, text, -textWidth / 2, -tr.fontHeight / 2, 0xFFFF5555);

        context.getMatrices().popMatrix();
    }

    public static void renderDayHud(DrawContext context, MinecraftClient client, net.minecraft.client.font.TextRenderer tr, int x, int y) {
        long day = 0;
        if (client.world != null) {
            day = client.world.getTimeOfDay() / 24000L;
        }
        String text = "Day: " + String.format("%,d", day);

        int color = 0xFFFFFFFF;

        boolean isTargetMap = "The End".equals(GameState.map) || "Combat 3".equals(GameState.mode);

        if (ModConfig.enableDay30Alert && isTargetMap && day >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.golemStage)) {
            color = 0xFFFF5555;
        }

        context.drawTextWithShadow(tr, text, x, y, color);
    }

    public static void renderDragonStatus(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        String title = "§d§lDragon Status";
        String eggState;
        String eyePlaced = null;
        String dragonType = null;

        if (isPreview) {
            eggState = "Egg: §cHatched §c(Spawned)";
            eyePlaced = "§cEyes placed: 8/8 §a(2)";
            dragonType = "Type: §eSuperior";
        } else {
            String state = GameState.dragonEggState;
            int eyes = GameState.dragonEyes;

            if ("Hatching".equals(state)) {
                long timeSincePacket = System.currentTimeMillis() - GameState.lastServerPacketArrivalMillis;
                if (timeSincePacket > 1000) timeSincePacket = 1000;

                double estimatedServerTime = GameState.lastServerTimePacket + (timeSincePacket / 50.0);
                double remainingTicks = GameState.dragonSpawnTargetTime - estimatedServerTime;

                if (GameState.dragonSpawnTargetTime == 0) {
                    remainingTicks = 0;
                }
                if (remainingTicks < 0) remainingTicks = 0;

                if (remainingTicks > 0) {
                    eggState = String.format("Egg: §eHatching §c(%.1fs)", remainingTicks / 20.0);
                } else {
                    eggState = "Egg: §eHatching §e(Soon)";
                }
            } else {
                String colorCode = switch (state) {
                    case "Ready" -> "§a";
                    case "Hatched" -> "§c";
                    case "Respawning" -> "§7";
                    case "Scanning..." -> "§8";
                    default -> "§f";
                };

                if ("Hatched".equals(state)) {
                    eggState = "Egg: " + colorCode + "Hatched §c(Spawned)";
                } else {
                    eggState = "Egg: " + colorCode + state;
                }
            }

            if (!"Respawning".equals(state) && !"Scanning...".equals(state)) {
                if (eyes == 8) {
                    eyePlaced = "§cEyes placed: 8/8";
                } else {
                    eyePlaced = "Eyes placed: §e" + eyes + "§7/§a8";
                }

                if (GameState.playerDragonEyes > 0) {
                    eyePlaced += " §a(" + GameState.playerDragonEyes + ")";
                }
            }

            if (GameState.dragonType != null) {
                String typeColorCode = switch (GameState.dragonType) {
                    case "Protector" -> "§8";
                    case "Old" -> "§7";
                    case "Unstable" -> "§5";
                    case "Young" -> "§f";
                    case "Strong" -> "§c";
                    case "Wise" -> "§b";
                    case "Superior" -> "§e";
                    default -> "§d";
                };
                dragonType = "Type: " + typeColorCode + GameState.dragonType;
            }
        }

        context.drawTextWithShadow(tr, title, x, y, 0xFFFF55FF);

        context.drawTextWithShadow(tr, eggState, x, y + 12, 0xFFFFFFFF);

        int nextY = y + 24;
        if (eyePlaced != null) {
            context.drawTextWithShadow(tr, eyePlaced, x, nextY, 0xFFFFFFFF);
            nextY += 12;
        }

        if (dragonType != null) {
            context.drawTextWithShadow(tr, dragonType, x, nextY, 0xFFFFFFFF);
        }
    }

    public static void renderDragonTracker(DrawContext context, TextRenderer tr, int x, int y, boolean isPreview) {
        context.drawTextWithShadow(tr, "§d§lDragon Loot Tracker", x, y, 0xFFFFFFFF);

        int epicCount = isPreview ? 1 : LootStats.epicDragonPets;
        int legCount = isPreview ? 2 : LootStats.legendaryDragonPets;

        String epicText = String.format("§5Ender Dragon §7(Pet): §f%d", epicCount);
        context.drawTextWithShadow(tr, epicText, x, y + 12, 0xFFFFFFFF);

        String legText = String.format("§6Ender Dragon §7(Pet): §f%d", legCount);
        context.drawTextWithShadow(tr, legText, x, y + 24, 0xFFFFFFFF);
    }
}