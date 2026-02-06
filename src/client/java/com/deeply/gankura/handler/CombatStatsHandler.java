package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class CombatStatsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CombatStatsHandler");

    public static void handleMessage(String msg, MinecraftClient client) {
        // 1. 戦闘開始
        if (msg.contains(ModConstants.GOLEM_RISE_MSG)) {
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

                // ★修正: 遅延なしで即時スキャン開始
                GameState.isLootScanning = true;
                GameState.hasShownDropAlert = false;
                // LOGGER.info("Loot Scanning Started (Instant)");
            }
            return;
        }

        // ... (以下、変更なし) ...

        Matcher firstMatcher = ModConstants.FIRST_PLACE_PATTERN.matcher(msg);
        if (firstMatcher.find()) {
            try {
                String raw = firstMatcher.group(1).replace(",", "");
                GameState.lastFirstPlaceDamage = Long.parseLong(raw);
            } catch (Exception ignored) {}
            return;
        }

        Matcher zealotMatcher = ModConstants.ZEALOT_PATTERN.matcher(msg);
        if (zealotMatcher.find()) {
            try {
                String raw = zealotMatcher.group(1).replace(",", "");
                GameState.lastZealotKills = Integer.parseInt(raw);
            } catch (Exception ignored) {}
            return;
        }

        Matcher dmgMatcher = ModConstants.DAMAGE_PATTERN.matcher(msg);
        if (dmgMatcher.find()) {
            processResult(dmgMatcher, client);
        }
    }

    // ... (既存のメソッド群はそのまま) ...

    private static void processResult(Matcher matcher, MinecraftClient client) {
        if (client.world == null || GameState.fightStartTime <= 0) return;
        long endTime = GameState.fightEndTime;
        if (endTime == 0) endTime = client.world.getTime();

        if (endTime <= GameState.fightStartTime) return;

        try {
            String rawDamage = matcher.group(1).replace(",", "");
            long myDamage = Long.parseLong(rawDamage);
            String rawPos = matcher.group(2).replace(",", "");
            int myPosition = Integer.parseInt(rawPos);

            long durationTicks = endTime - GameState.fightStartTime;
            double durationSeconds = durationTicks / 20.0;

            if (durationSeconds > 0) {
                double dps = myDamage / durationSeconds;
                String formattedDps = formatDps(dps);
                String durationStr = String.format("%.1fs", durationSeconds);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        int lootQuality = calculateLootQuality(myDamage, myPosition);
                        printResult(client, formattedDps, durationStr, lootQuality);
                    }
                }, 500);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to calculate stats", e);
        }
    }

    private static void printResult(MinecraftClient client, String dps, String duration, int lq) {
        String tbcMark = (lq >= 250) ? "§a✔" : "§c✘";
        String legMark = (lq >= 235) ? "§a✔" : "§c✘";
        String epicMark = (lq >= 220) ? "§a✔" : "§c✘";

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(String.format("§a[GanKura] §bYour Golem DPS: %s §7(%s)", dps, duration)), false);
                client.player.sendMessage(Text.literal(String.format("§a[GanKura] §eGolem Loot Quality: %d", lq)), false);
                String dropsMsg = String.format("§a[GanKura] §6Tier Boost Core: %s §8| §6Golem Pet: %s §8| §5Golem Pet: %s", tbcMark, legMark, epicMark);
                client.player.sendMessage(Text.literal(dropsMsg), false);
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