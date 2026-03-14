package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class GolemHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CombatStatsHandler");

    public static void handleMessage(String msg, MinecraftClient client) {
        if (msg.contains(ModConstants.GOLEM_RISE_MSG)) {
            GameState.Golem.hasRisen = true;
            if (client.world != null) {
                GameState.Golem.fightStartTime = client.world.getTime();
                GameState.Golem.fightEndTime = 0; GameState.Golem.lastFirstPlaceDamage = 0; GameState.Golem.lastZealotKills = 0;
            }
            return;
        }

        if (msg.contains(ModConstants.GOLEM_DOWN_MSG)) {
            if (client.world != null) {
                GameState.Golem.fightEndTime = client.world.getTime();
                GameState.Player.isLootScanning = true; GameState.Player.hasShownDropAlert = false;
            }
            return;
        }

        Matcher topMatcher = ModConstants.TOP_DAMAGER_PATTERN.matcher(msg);
        if (topMatcher.find()) {
            try {
                String rank = topMatcher.group(1); String name = topMatcher.group(2);
                long damage = Long.parseLong(topMatcher.group(3).replace(",", ""));
                if ("1st".equals(rank)) { GameState.Golem.top1Name = name; GameState.Golem.top1Damage = damage; GameState.Golem.lastFirstPlaceDamage = damage; }
                else if ("2nd".equals(rank)) { GameState.Golem.top2Name = name; GameState.Golem.top2Damage = damage; }
                else if ("3rd".equals(rank)) { GameState.Golem.top3Name = name; GameState.Golem.top3Damage = damage; }
            } catch (Exception ignored) {}
            return;
        }

        Matcher zealotMatcher = ModConstants.ZEALOT_PATTERN.matcher(msg);
        if (zealotMatcher.find()) {
            try { GameState.Golem.lastZealotKills = Integer.parseInt(zealotMatcher.group(1).replace(",", "")); } catch (Exception ignored) {}
            return;
        }

        Matcher dmgMatcher = ModConstants.DAMAGE_PATTERN.matcher(msg);
        if (dmgMatcher.find()) processResult(dmgMatcher, client);
    }

    private static void processResult(Matcher matcher, MinecraftClient client) {
        if (client.world == null) return;
        long lastDownTime = GameState.Golem.fightEndTime;
        long currentTime = client.world.getTime();
        if (lastDownTime == 0 || (currentTime - lastDownTime) > 400) return;
        GameState.Golem.fightEndTime = 0;

        try {
            long myDamage = Long.parseLong(matcher.group(1).replace(",", ""));
            int myPosition = Integer.parseInt(matcher.group(2).replace(",", ""));
            String formattedDps = null; String durationStr = null; double durationSeconds = 0;
            if (GameState.Golem.fightStartTime > 0 && lastDownTime > GameState.Golem.fightStartTime) {
                durationSeconds = (lastDownTime - GameState.Golem.fightStartTime) / 20.0;
                if (durationSeconds > 0) {
                    formattedDps = formatDps(myDamage / durationSeconds);
                    durationStr = String.format("%.1fs", durationSeconds);
                }
            }

            String finalDps = formattedDps; String finalDuration = durationStr; double finalDurationSec = durationSeconds;
            new Timer().schedule(new TimerTask() {
                @Override public void run() {
                    int lootQuality = calculateLootQuality(myDamage, myPosition);
                    printResult(client, finalDps, finalDuration, finalDurationSec, lootQuality);
                }
            }, 500);
        } catch (Exception e) {}
    }

    private static void printResult(MinecraftClient client, String dps, String duration, double durationSeconds, int lq) {
        client.execute(() -> {
            if (client.player != null) {
                if (ModConfig.showDpsChat && dps != null && duration != null) {
                    MutableText msg = Text.literal(String.format("§bYour Golem DPS: %s §7(%s) ", dps, duration));
                    if (durationSeconds > 0 && GameState.Golem.top1Damage > 0) {
                        MutableText hoverText = Text.literal("§6§lTop 3 DPS\n");
                        hoverText.append(Text.literal(String.format("§e#1 §f%s §7- §b%s", GameState.Golem.top1Name, formatDps(GameState.Golem.top1Damage / durationSeconds))));
                        if (GameState.Golem.top2Damage > 0) hoverText.append(Text.literal(String.format("\n§6#2 §f%s §7- §b%s", GameState.Golem.top2Name, formatDps(GameState.Golem.top2Damage / durationSeconds))));
                        if (GameState.Golem.top3Damage > 0) hoverText.append(Text.literal(String.format("\n§c#3 §f%s §7- §b%s", GameState.Golem.top3Name, formatDps(GameState.Golem.top3Damage / durationSeconds))));
                        msg.append(Text.literal("§8[§eHOVER§8]").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText))));
                    }
                    NotificationUtils.sendSystemChat(client, msg);
                }
                if (ModConfig.showLootQualityChat) {
                    NotificationUtils.sendSystemChat(client, Text.literal(String.format("§eYour Golem Loot Quality: %d", lq)));
                    String dropsMsg = String.format("§6Tier Boost Core: %s §8| §6Golem §7(Pet): %s §8| §5Golem §7(Pet): %s", (lq >= 250) ? "§a✔" : "§c✘", (lq >= 235) ? "§a✔" : "§c✘", (lq >= 220) ? "§a✔" : "§c✘");
                    NotificationUtils.sendSystemChat(client, Text.literal(dropsMsg));
                }
            }
        });
    }

    private static int calculateLootQuality(long myDamage, int myPosition) {
        int placementQuality;
        if (myPosition == 1) placementQuality = 200; else if (myPosition == 2) placementQuality = 175; else if (myPosition == 3) placementQuality = 150; else if (myPosition == 4) placementQuality = 125; else if (myPosition == 5) placementQuality = 110; else if (myPosition >= 6 && myPosition <= 8) placementQuality = 100; else if (myPosition >= 9 && myPosition <= 10) placementQuality = 90; else if (myPosition >= 11 && myPosition <= 12) placementQuality = 80; else placementQuality = (myDamage >= 1) ? 70 : 10;
        double damageScore = 0;
        long firstDamage = GameState.Golem.lastFirstPlaceDamage;
        if (firstDamage == 0 && myPosition == 1) firstDamage = myDamage;
        if (firstDamage > 0) damageScore = 50.0 * ((double) myDamage / firstDamage);
        int zealotScore = Math.min(GameState.Golem.lastZealotKills, 100);
        return (int) (placementQuality + damageScore + zealotScore);
    }

    private static String formatDps(double dps) { return dps >= 1000 ? String.format("%,.1fk", dps / 1000.0) : String.format("%,.1f", dps); }

    public static void processTabList(List<String> lines, MinecraftClient client) {
        for (String line : lines) {
            Matcher matcher = ModConstants.PROTECTOR_PATTERN.matcher(line);
            if (matcher.find()) {
                boolean wasScanning = GameState.Golem.isScanning;
                if (GameState.Golem.isScanning) GameState.Golem.isScanning = false;
                String rawState = matcher.group(1).trim().split("\\s+")[0];
                String stageName = switch (rawState) { case "Resting" -> ModConstants.STAGE_RESTING; case "Dormant" -> ModConstants.STAGE_DORMANT; case "Agitated" -> ModConstants.STAGE_AGITATED; case "Disturbed" -> ModConstants.STAGE_DISTURBED; case "Awakening" -> ModConstants.STAGE_AWAKENING; default -> rawState; };
                if (wasScanning && ModConstants.STAGE_SUMMONED.equals(stageName)) GameState.Golem.stage5TargetTime = 1;
                if (ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConstants.STAGE_AWAKENING.equals(stageName)) return;
                updateStage(client, stageName);
                return;
            }
        }
    }

    public static void setStageToSummoned(MinecraftClient client) {
        if (GameState.Golem.isScanning) GameState.Golem.isScanning = false;
        if (client.world != null) GameState.Golem.stage5TargetTime = client.world.getTime() + 400;
        updateStage(client, ModConstants.STAGE_SUMMONED);
    }

    private static void updateStage(MinecraftClient client, String newStage) {
        String oldStage = GameState.Golem.stage;
        if (oldStage.equals(newStage)) return;

        GameState.Golem.stage = newStage;

        if (ModConstants.STAGE_AWAKENING.equals(newStage)) {
            GameState.Golem.stage4StartTime = System.currentTimeMillis();
            if (ModConfig.enableStageAlerts) {
                MutableText title = Text.literal("GOLEM STAGE 4").formatted(Formatting.RED, Formatting.BOLD);
                // ★修正: サブタイトルを削除 (nullを渡す)
                NotificationUtils.showTitle(client, title, null);
                NotificationUtils.playSound(client, SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.8f);
            }
        }
        else if (ModConstants.STAGE_SUMMONED.equals(newStage)) {
            if (ModConstants.STAGE_AWAKENING.equals(oldStage) && GameState.Golem.stage4StartTime > 0) {
                long seconds = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                if (ModConfig.showStage4Duration) {
                    new Timer().schedule(new TimerTask() {
                        @Override public void run() {
                            client.execute(() -> NotificationUtils.sendSystemChat(client, Text.literal(String.format("§aStage 4 Duration: %dm %ds", seconds / 60, seconds % 60))));
                        }
                    }, 100);
                }
            }
            GameState.Golem.stage4StartTime = 0;
            if (GameState.Golem.stage5TargetTime == 0 && client.world != null) GameState.Golem.stage5TargetTime = client.world.getTime() + 400;

            if (ModConfig.enableStageAlerts) {
                MutableText title = Text.literal("GOLEM STAGE 5").formatted(Formatting.DARK_RED, Formatting.BOLD);
                // ★修正: サブタイトルを削除 (nullを渡す)
                NotificationUtils.showTitle(client, title, null);
                NotificationUtils.playSound(client, SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            }
        }
        else {
            GameState.Golem.stage4StartTime = 0;
            GameState.Golem.hasRisen = false;
        }
    }
}