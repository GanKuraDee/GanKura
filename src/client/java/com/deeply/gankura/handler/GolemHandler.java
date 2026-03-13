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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class GolemHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CombatStatsHandler");

    public static void handleMessage(String msg, MinecraftClient client) {
        // 1. 戦闘開始
        if (msg.contains(ModConstants.GOLEM_RISE_MSG)) {
            // ゴーレムが飛び出したフラグをオンにする
            GameState.hasGolemRisen = true;

            if (client.world != null) {
                GameState.fightStartTime = client.world.getTime();
                GameState.fightEndTime = 0;
                GameState.lastFirstPlaceDamage = 0;
                GameState.lastZealotKills = 0;
            }
            return;
        }

        // 2. 戦闘終了 (DOWN!)
        if (msg.contains(ModConstants.GOLEM_DOWN_MSG)) {
            if (client.world != null) {
                GameState.fightEndTime = client.world.getTime();

                // Lootスキャン開始
                GameState.isLootScanning = true;
                GameState.hasShownDropAlert = false;
            }
            return;
        }

        // 3. Top 3 ダメージ収集
        Matcher topMatcher = ModConstants.TOP_DAMAGER_PATTERN.matcher(msg);
        if (topMatcher.find()) {
            try {
                String rank = topMatcher.group(1);
                String name = topMatcher.group(2); // [MVP+] などを除いた純粋なプレイヤーID
                long damage = Long.parseLong(topMatcher.group(3).replace(",", ""));

                if ("1st".equals(rank)) {
                    GameState.top1Name = name;
                    GameState.top1Damage = damage;
                    GameState.lastFirstPlaceDamage = damage; // Loot Quality計算用に維持
                } else if ("2nd".equals(rank)) {
                    GameState.top2Name = name;
                    GameState.top2Damage = damage;
                } else if ("3rd".equals(rank)) {
                    GameState.top3Name = name;
                    GameState.top3Damage = damage;
                }
            } catch (Exception ignored) {}
            return;
        }

        // 4. Zealot数収集
        Matcher zealotMatcher = ModConstants.ZEALOT_PATTERN.matcher(msg);
        if (zealotMatcher.find()) {
            try {
                String raw = zealotMatcher.group(1).replace(",", "");
                GameState.lastZealotKills = Integer.parseInt(raw);
            } catch (Exception ignored) {}
            return;
        }

        // 5. 結果表示 (Your Damage)
        Matcher dmgMatcher = ModConstants.DAMAGE_PATTERN.matcher(msg);
        if (dmgMatcher.find()) {
            processResult(dmgMatcher, client);
        }
    }

    private static void processResult(Matcher matcher, MinecraftClient client) {
        if (client.world == null) return;

        long lastDownTime = GameState.fightEndTime;
        long currentTime = client.world.getTime();

        if (lastDownTime == 0 || (currentTime - lastDownTime) > 400) {
            return;
        }

        GameState.fightEndTime = 0;

        try {
            String rawDamage = matcher.group(1).replace(",", "");
            long myDamage = Long.parseLong(rawDamage);
            String rawPos = matcher.group(2).replace(",", "");
            int myPosition = Integer.parseInt(rawPos);

            String formattedDps = null;
            String durationStr = null;
            double durationSeconds = 0; // ★ここに追加

            if (GameState.fightStartTime > 0 && lastDownTime > GameState.fightStartTime) {
                long durationTicks = lastDownTime - GameState.fightStartTime;
                durationSeconds = durationTicks / 20.0; // ★ここで代入

                if (durationSeconds > 0) {
                    double dps = myDamage / durationSeconds;
                    formattedDps = formatDps(dps);
                    durationStr = String.format("%.1fs", durationSeconds);
                }
            }

            String finalDps = formattedDps;
            String finalDuration = durationStr;
            double finalDurationSec = durationSeconds; // ★これを使って計算値を固定化

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    int lootQuality = calculateLootQuality(myDamage, myPosition);
                    // ★修正: durationSeconds を printResult に渡す
                    printResult(client, finalDps, finalDuration, finalDurationSec, lootQuality);
                }
            }, 500);

        } catch (Exception e) {
            LOGGER.error("Failed to calculate stats", e);
        }
    }

    // ★修正: 引数に durationSeconds (double) を追加
    private static void printResult(MinecraftClient client, String dps, String duration, double durationSeconds, int lq) {
        String tbcMark = (lq >= 250) ? "§a✔" : "§c✘";
        String legMark = (lq >= 235) ? "§a✔" : "§c✘";
        String epicMark = (lq >= 220) ? "§a✔" : "§c✘";

        client.execute(() -> {
            if (client.player != null) {
                if (ModConfig.showDpsChat && dps != null && duration != null) {
                    MutableText msg = NotificationUtils.getGanKuraPrefix();
                    msg.append(Text.literal(String.format("§bYour Golem DPS: %s §7(%s) ", dps, duration)));

                    // =======================================================
                    // ★追加: Top 3 ホバーの生成
                    // =======================================================
                    if (durationSeconds > 0 && GameState.top1Damage > 0) {
                        MutableText hoverText = Text.literal("§6§lTop 3 DPS\n");
                        hoverText.append(Text.literal(String.format("§e#1 §f%s §7- §b%s", GameState.top1Name, formatDps(GameState.top1Damage / durationSeconds))));

                        if (GameState.top2Damage > 0) {
                            hoverText.append(Text.literal(String.format("\n§6#2 §f%s §7- §b%s", GameState.top2Name, formatDps(GameState.top2Damage / durationSeconds))));
                        }
                        if (GameState.top3Damage > 0) {
                            hoverText.append(Text.literal(String.format("\n§c#3 §f%s §7- §b%s", GameState.top3Name, formatDps(GameState.top3Damage / durationSeconds))));
                        }

                        // [HOVER] というテキストに、マウスを乗せた時(SHOW_TEXT)のイベントを付与
                        MutableText hoverButton = Text.literal("§8[§eHOVER§8]")
                                .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText)));

                        msg.append(hoverButton);
                    }
                    // =======================================================

                    client.player.sendMessage(msg, false);
                }

                if (ModConfig.showLootQualityChat) {
                    MutableText msg1 = NotificationUtils.getGanKuraPrefix();
                    msg1.append(Text.literal(String.format("§eYour Golem Loot Quality: %d", lq)));
                    client.player.sendMessage(msg1, false);

                    MutableText msg2 = NotificationUtils.getGanKuraPrefix();
                    String dropsMsg = String.format("§6Tier Boost Core: %s §8| §6Golem §7(Pet): %s §8| §5Golem §7(Pet): %s", tbcMark, legMark, epicMark);
                    msg2.append(Text.literal(dropsMsg));
                    client.player.sendMessage(msg2, false);
                }
            }
        });
    }

    private static int calculateLootQuality(long myDamage, int myPosition) {
        int placementQuality;
        if (myPosition == 1) placementQuality = 200;
        else if (myPosition == 2) placementQuality = 175;
        else if (myPosition == 3) placementQuality = 150;
        else if (myPosition == 4) placementQuality = 125;
        else if (myPosition == 5) placementQuality = 110;
        else if (myPosition >= 6 && myPosition <= 8) placementQuality = 100;
        else if (myPosition >= 9 && myPosition <= 10) placementQuality = 90;
        else if (myPosition >= 11 && myPosition <= 12) placementQuality = 80;
        else placementQuality = (myDamage >= 1) ? 70 : 10;

        double damageScore = 0;
        long firstDamage = GameState.lastFirstPlaceDamage;
        if (firstDamage == 0 && myPosition == 1) firstDamage = myDamage;
        if (firstDamage > 0) damageScore = 50.0 * ((double) myDamage / firstDamage);

        int zealotScore = Math.min(GameState.lastZealotKills, 100);
        return (int) (placementQuality + damageScore + zealotScore);
    }

    private static String formatDps(double dps) {
        if (dps >= 1000) return String.format("%,.1fk", dps / 1000.0);
        return String.format("%,.1f", dps);
    }
}