package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class DragonHandler {

    public static boolean handleMessage(String msg, MinecraftClient client) {
        String cleanMsg = msg.replaceAll("§[0-9a-fk-or]", "");

        // 1. Summoning Eye 設置 (通常: X/8)
        Matcher m1 = ModConstants.EYE_PLACED_CHAT_PATTERN.matcher(cleanMsg);
        if (m1.find()) {
            GameState.dragonEyes = Integer.parseInt(m1.group(1));
            GameState.dragonEggState = "Ready";
            if (cleanMsg.contains("You placed a Summoning Eye!")) {
                GameState.playerDragonEyes++;
            }
            GameState.lastDragonChatTime = System.currentTimeMillis();
            return true;
        }

        // 2. Summoning Eye 設置 (8個目・9秒カウントダウン開始)
        Matcher m2 = ModConstants.EYE_PLACED_8_CHAT_PATTERN.matcher(cleanMsg);
        if (m2.find()) {
            GameState.dragonEyes = 8;
            GameState.dragonEggState = "Hatching";
            if (cleanMsg.contains("You placed a Summoning Eye! Brace yourselves!")) {
                GameState.playerDragonEyes++;
            }
            if (client.world != null) {
                GameState.dragonSpawnTargetTime = client.world.getTime() + 180;
            }
            GameState.lastDragonChatTime = System.currentTimeMillis();
            return true;
        }

        // 3. 自分がSummoning Eyeを回収(Remove)した時
        if (cleanMsg.contains("You recovered a Summoning Eye!")) {
            if (GameState.playerDragonEyes > 0) GameState.playerDragonEyes--;
            if (GameState.dragonEyes > 0) GameState.dragonEyes--;
            GameState.dragonEggState = "Ready";
            GameState.lastDragonChatTime = System.currentTimeMillis();
            return true;
        }

        // 4. ドラゴンのスポーンメッセージ (Egg: Hatched)
        Matcher m3 = ModConstants.DRAGON_SPAWN_PATTERN.matcher(cleanMsg);
        if (m3.find()) {
            String dragonType = m3.group(1);
            GameState.dragonEggState = "Hatched";
            GameState.dragonType = dragonType;
            GameState.dragonEyes = 8;
            GameState.dragonSpawnTargetTime = 0;

            if (client.world != null) {
                GameState.dragonFightStartTime = client.world.getTime();
                GameState.dragonFightEndTime = 0;
                GameState.dragonTop1Name = null; GameState.dragonTop1Damage = 0;
                GameState.dragonTop2Name = null; GameState.dragonTop2Damage = 0;
                GameState.dragonTop3Name = null; GameState.dragonTop3Damage = 0;
            }

            if (ModConfig.enableDragonAlerts) {
                client.execute(() -> NotificationUtils.showDragonSpawnAlert(client, dragonType));
            }
            GameState.lastDragonChatTime = System.currentTimeMillis();
            return true;
        }

        // 5. ドラゴン討伐メッセージ (Egg: Respawning)
        Matcher m4 = ModConstants.DRAGON_DOWN_PATTERN.matcher(cleanMsg);
        if (m4.find()) {
            GameState.dragonEggState = "Respawning";
            GameState.dragonSpawnTargetTime = 0;
            GameState.dragonType = null;
            GameState.dragonEyes = 0;

            if (client.world != null) {
                GameState.dragonFightEndTime = client.world.getTime();
            }
            GameState.lastDragonChatTime = System.currentTimeMillis();

            // ★追加: ゴーレム同様、討伐されたら周囲のエンティティスキャンを60秒間開始する！
            GameState.isLootScanning = true;
            GameState.hasShownDropAlert = false;

            return true;
        }

        // 討伐直後のダメージスキャン
        boolean isRecentKill = GameState.dragonFightEndTime > 0 && client.world != null && (client.world.getTime() - GameState.dragonFightEndTime < 400);

        if (isRecentKill) {
            Matcher topMatcher = ModConstants.TOP_DAMAGER_PATTERN.matcher(cleanMsg);
            if (topMatcher.find()) {
                try {
                    String rank = topMatcher.group(1);
                    String name = topMatcher.group(2);
                    long damage = Long.parseLong(topMatcher.group(3).replace(",", ""));

                    if ("1st".equals(rank)) {
                        GameState.dragonTop1Name = name;
                        GameState.dragonTop1Damage = damage;
                    } else if ("2nd".equals(rank)) {
                        GameState.dragonTop2Name = name;
                        GameState.dragonTop2Damage = damage;
                    } else if ("3rd".equals(rank)) {
                        GameState.dragonTop3Name = name;
                        GameState.dragonTop3Damage = damage;
                    }
                } catch (Exception ignored) {}
                return true;
            }

            Matcher dmgMatcher = ModConstants.DAMAGE_PATTERN.matcher(cleanMsg);
            if (dmgMatcher.find()) {
                processDragonResult(dmgMatcher, client);
                return true;
            }
        }

        // 6. 卵リスポーンメッセージ (Egg: Ready)
        if (cleanMsg.contains(ModConstants.DRAGON_EGG_SPAWNED_MSG)) {
            GameState.dragonEggState = "Ready";
            GameState.dragonType = null;
            GameState.dragonEyes = 0;
            GameState.playerDragonEyes = 0;
            GameState.dragonSpawnTargetTime = 0;
            GameState.lastDragonChatTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private static void processDragonResult(Matcher matcher, MinecraftClient client) {
        if (client.world == null) return;

        long lastDownTime = GameState.dragonFightEndTime;
        GameState.dragonFightEndTime = 0;

        try {
            long myDamage = Long.parseLong(matcher.group(1).replace(",", ""));
            int myPosition = Integer.parseInt(matcher.group(2).replace(",", ""));

            double durationSeconds = 0;
            String formattedDps = null;
            String durationStr = null;

            if (GameState.dragonFightStartTime > 0 && lastDownTime > GameState.dragonFightStartTime) {
                long durationTicks = lastDownTime - GameState.dragonFightStartTime;
                durationSeconds = durationTicks / 20.0;

                if (durationSeconds > 0) {
                    double dps = myDamage / durationSeconds;
                    formattedDps = formatDps(dps);
                    durationStr = String.format("%.1fs", durationSeconds);
                }
            }

            String finalDps = formattedDps;
            String finalDuration = durationStr;
            double finalDurationSec = durationSeconds;

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    int lootQuality = calculateDragonLootQuality(myDamage, myPosition);
                    printDragonResult(client, finalDps, finalDuration, finalDurationSec, lootQuality);
                }
            }, 500);

        } catch (Exception e) {}
    }

    private static int calculateDragonLootQuality(long myDamage, int myPosition) {
        int placementQuality = 10;
        if (myDamage >= 1) {
            if (myPosition == 1) placementQuality = 200;
            else if (myPosition == 2) placementQuality = 175;
            else if (myPosition == 3) placementQuality = 150;
            else if (myPosition == 4) placementQuality = 125;
            else if (myPosition == 5) placementQuality = 110;
            else if (myPosition >= 6 && myPosition <= 8) placementQuality = 100;
            else if (myPosition >= 9 && myPosition <= 10) placementQuality = 90;
            else if (myPosition >= 11 && myPosition <= 12) placementQuality = 80;
            else if (myPosition >= 13 && myPosition <= 25) placementQuality = 70;
        }

        double damageScore = 0;
        long firstDamage = GameState.dragonTop1Damage;
        if (firstDamage == 0 && myPosition == 1) firstDamage = myDamage;
        if (firstDamage > 0) {
            damageScore = 100.0 * ((double) myDamage / firstDamage);
        }

        int eyesScore = 100 * GameState.playerDragonEyes;

        return (int) (placementQuality + eyesScore + damageScore);
    }

    private static void printDragonResult(MinecraftClient client, String dps, String duration, double durationSeconds, int lq) {
        String legMark = (lq >= 450) ? "§a✔" : "§c✘";
        String epicMark = (lq >= 449) ? "§a✔" : "§c✘";

        client.execute(() -> {
            if (client.player != null) {
                if (ModConfig.showDragonDpsChat && dps != null && duration != null) {
                    MutableText msg = NotificationUtils.getGanKuraPrefix();
                    msg.append(Text.literal(String.format("§bYour Dragon DPS: %s §7(%s) ", dps, duration)));

                    if (durationSeconds > 0 && GameState.dragonTop1Damage > 0) {
                        MutableText hoverText = Text.literal("§6§lTop 3 DPS\n");
                        hoverText.append(Text.literal(String.format("§e#1 §f%s §7- §b%s", GameState.dragonTop1Name, formatDps(GameState.dragonTop1Damage / durationSeconds))));

                        if (GameState.dragonTop2Damage > 0) {
                            hoverText.append(Text.literal(String.format("\n§6#2 §f%s §7- §b%s", GameState.dragonTop2Name, formatDps(GameState.dragonTop2Damage / durationSeconds))));
                        }
                        if (GameState.dragonTop3Damage > 0) {
                            hoverText.append(Text.literal(String.format("\n§c#3 §f%s §7- §b%s", GameState.dragonTop3Name, formatDps(GameState.dragonTop3Damage / durationSeconds))));
                        }

                        MutableText hoverButton = Text.literal("§8[§eHOVER§8]")
                                .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText)));
                        msg.append(hoverButton);
                    }
                    client.player.sendMessage(msg, false);
                }

                if (ModConfig.showDragonLootQualityChat) {
                    MutableText msg1 = NotificationUtils.getGanKuraPrefix();
                    msg1.append(Text.literal(String.format("§eYour Dragon Loot Quality: %d", lq)));
                    client.player.sendMessage(msg1, false);

                    MutableText msg2 = NotificationUtils.getGanKuraPrefix();
                    String dropsMsg = String.format("§6Ender Dragon §7(Pet): %s §8| §5Ender Dragon §7(Pet): %s", legMark, epicMark);
                    msg2.append(Text.literal(dropsMsg));
                    client.player.sendMessage(msg2, false);
                }
            }
        });
    }

    private static String formatDps(double dps) {
        if (dps >= 1000) return String.format("%,.1fk", dps / 1000.0);
        return String.format("%,.1f", dps);
    }
}