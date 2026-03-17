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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class DragonHandler {

    public static boolean handleMessage(String msg, MinecraftClient client) {
        String cleanMsg = msg.replaceAll("§[0-9a-fk-or]", "");

        Matcher m1 = ModConstants.EYE_PLACED_CHAT_PATTERN.matcher(cleanMsg);
        if (m1.find()) {
            GameState.Dragon.eyes = Integer.parseInt(m1.group(1)); GameState.Dragon.eggState = "Ready";
            if (cleanMsg.contains("You placed a Summoning Eye!")) GameState.Dragon.playerEyes++;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m2 = ModConstants.EYE_PLACED_8_CHAT_PATTERN.matcher(cleanMsg);
        if (m2.find()) {
            GameState.Dragon.eyes = 8; GameState.Dragon.eggState = "Hatching";
            if (cleanMsg.contains("You placed a Summoning Eye! Brace yourselves!")) GameState.Dragon.playerEyes++;
            if (client.world != null) GameState.Dragon.spawnTargetTime = client.world.getTime() + 180;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        if (cleanMsg.contains("You recovered a Summoning Eye!")) {
            if (GameState.Dragon.playerEyes > 0) GameState.Dragon.playerEyes--;
            if (GameState.Dragon.eyes > 0) GameState.Dragon.eyes--;
            GameState.Dragon.eggState = "Ready"; GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m3 = ModConstants.DRAGON_SPAWN_PATTERN.matcher(cleanMsg);
        if (m3.find()) {
            String dragonType = m3.group(1);
            GameState.Dragon.eggState = "Hatched"; GameState.Dragon.type = dragonType; GameState.Dragon.eyes = 8; GameState.Dragon.spawnTargetTime = 0;
            if (client.world != null) {
                GameState.Dragon.fightStartTime = client.world.getTime(); GameState.Dragon.fightEndTime = 0;
                GameState.Dragon.top1Name = null; GameState.Dragon.top1Damage = 0; GameState.Dragon.top2Name = null; GameState.Dragon.top2Damage = 0; GameState.Dragon.top3Name = null; GameState.Dragon.top3Damage = 0;
            }
            if (ModConfig.enableDragonAlerts) {
                client.execute(() -> showDragonSpawnAlert(client, dragonType));
            }
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m4 = ModConstants.DRAGON_DOWN_PATTERN.matcher(cleanMsg);
        if (m4.find()) {
            GameState.Dragon.eggState = "Respawning"; GameState.Dragon.spawnTargetTime = 0; GameState.Dragon.type = null; GameState.Dragon.eyes = 0;
            if (client.world != null) GameState.Dragon.fightEndTime = client.world.getTime();
            GameState.Dragon.lastChatTime = System.currentTimeMillis();
            GameState.Player.isLootScanning = true; GameState.Player.hasShownDropAlert = false;
            return true;
        }

        boolean isRecentKill = GameState.Dragon.fightEndTime > 0 && client.world != null && (client.world.getTime() - GameState.Dragon.fightEndTime < 400);
        if (isRecentKill) {
            Matcher topMatcher = ModConstants.TOP_DAMAGER_PATTERN.matcher(cleanMsg);
            if (topMatcher.find()) {
                try {
                    String rank = topMatcher.group(1); String name = topMatcher.group(2); long damage = Long.parseLong(topMatcher.group(3).replace(",", ""));
                    if ("1st".equals(rank)) { GameState.Dragon.top1Name = name; GameState.Dragon.top1Damage = damage; }
                    else if ("2nd".equals(rank)) { GameState.Dragon.top2Name = name; GameState.Dragon.top2Damage = damage; }
                    else if ("3rd".equals(rank)) { GameState.Dragon.top3Name = name; GameState.Dragon.top3Damage = damage; }
                } catch (Exception ignored) {}
                return true;
            }
            Matcher dmgMatcher = ModConstants.DAMAGE_PATTERN.matcher(cleanMsg);
            if (dmgMatcher.find()) {
                processDragonResult(dmgMatcher, client); return true;
            }
        }

        if (cleanMsg.contains(ModConstants.DRAGON_EGG_SPAWNED_MSG)) {
            GameState.Dragon.eggState = "Ready"; GameState.Dragon.type = null; GameState.Dragon.eyes = 0; GameState.Dragon.playerEyes = 0; GameState.Dragon.spawnTargetTime = 0;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }
        return false;
    }

    private static void processDragonResult(Matcher matcher, MinecraftClient client) {
        if (client.world == null) return;
        long lastDownTime = GameState.Dragon.fightEndTime; GameState.Dragon.fightEndTime = 0;
        try {
            long myDamage = Long.parseLong(matcher.group(1).replace(",", ""));
            int myPosition = Integer.parseInt(matcher.group(2).replace(",", ""));
            double durationSeconds = 0;
            if (GameState.Dragon.fightStartTime > 0 && lastDownTime > GameState.Dragon.fightStartTime) durationSeconds = (lastDownTime - GameState.Dragon.fightStartTime) / 20.0;

            final double finalDurationSec = durationSeconds;
            final String finalDps = (durationSeconds > 0) ? formatDps(myDamage / durationSeconds) : null;
            final String finalDurationStr = (durationSeconds > 0) ? String.format("%.1fs", durationSeconds) : null;

            new Timer().schedule(new TimerTask() {
                @Override public void run() {
                    // ★修正: myDamage(自分のダメージ)も引数として渡す
                    int lootQuality = calculateDragonLootQuality(myPosition, myDamage);
                    printDragonResult(client, finalDps, finalDurationStr, finalDurationSec, lootQuality);
                }
            }, 500);
        } catch (Exception ignored) {}
    }

    // ★修正: 指定された公式に完全準拠したLoot Qualityの計算ロジック
    private static int calculateDragonLootQuality(int myPosition, long myDamage) {
        int placementQuality = 10; // ダメージ1未満のデフォルト値

        if (myDamage >= 1) {
            if (myPosition == 1) placementQuality = 200;
            else if (myPosition == 2) placementQuality = 175;
            else if (myPosition == 3) placementQuality = 150;
            else if (myPosition == 4) placementQuality = 125;
            else if (myPosition == 5) placementQuality = 110;
            else if (myPosition >= 6 && myPosition <= 8) placementQuality = 100;
            else if (myPosition >= 9 && myPosition <= 10) placementQuality = 90;
            else if (myPosition >= 11 && myPosition <= 12) placementQuality = 80;
            else placementQuality = 70; // 13位以降でダメージ1以上
        }

        // ダメージスコアの計算: (100 * DamageDealt) / FirstPlaceDamageDealt
        double damageRatio = 0;
        long firstDamage = GameState.Dragon.top1Damage;

        // もし自分が1位で、Top Damagerメッセージの取得が遅れた場合のフェイルセーフ
        if (firstDamage == 0 && myPosition == 1) {
            firstDamage = myDamage;
        }

        if (firstDamage > 0) {
            damageRatio = (100.0 * myDamage) / firstDamage;
        }

        int placedEyes = GameState.Dragon.playerEyes;

        // LootQuality = PlacementQuality + (100 * SummoningEyePlaced) + DamageRatio
        return (int) (placementQuality + (100 * placedEyes) + damageRatio);
    }

    private static void printDragonResult(MinecraftClient client, String dps, String duration, double durationSeconds, int lq) {
        client.execute(() -> {
            if (client.player != null) {
                if (ModConfig.showDragonDpsChat && dps != null && duration != null) {
                    MutableText msg = Text.literal(String.format("§dYour Dragon DPS: %s §7(%s) ", dps, duration));
                    if (durationSeconds > 0 && GameState.Dragon.top1Damage > 0) {
                        MutableText hoverText = Text.literal("§6§lTop 3 DPS\n");
                        hoverText.append(Text.literal(String.format("§e#1 §f%s §7- §b%s", GameState.Dragon.top1Name, formatDps(GameState.Dragon.top1Damage / durationSeconds))));
                        if (GameState.Dragon.top2Damage > 0) hoverText.append(Text.literal(String.format("\n§6#2 §f%s §7- §b%s", GameState.Dragon.top2Name, formatDps(GameState.Dragon.top2Damage / durationSeconds))));
                        if (GameState.Dragon.top3Damage > 0) hoverText.append(Text.literal(String.format("\n§c#3 §f%s §7- §b%s", GameState.Dragon.top3Name, formatDps(GameState.Dragon.top3Damage / durationSeconds))));
                        msg.append(Text.literal("§8§l[§e§lHOVER§8§l]").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hoverText))));
                    }
                    NotificationUtils.sendSystemChat(client, msg);
                }
                if (ModConfig.showDragonLootQualityChat) {
                    NotificationUtils.sendSystemChat(client, Text.literal(String.format("§eYour Dragon Loot Quality: %d", lq)));
                    String dropsMsg = String.format("§6Ender Dragon §7(Pet): %s §8| §5Ender Dragon §7(Pet): %s", (lq >= 450) ? "§a✔" : "§c✘", (lq >= 350) ? "§a✔" : "§c✘");
                    NotificationUtils.sendSystemChat(client, Text.literal(dropsMsg));
                }
            }
        });
    }

    private static String formatDps(double dps) { return dps >= 1000 ? String.format("%,.1fk", dps / 1000.0) : String.format("%,.1f", dps); }

    // ★Utilsから引き継いだドラゴンスポーンの表示処理
    private static void showDragonSpawnAlert(MinecraftClient client, String dragonType) {
        Formatting color = switch (dragonType) {
            case "Protector" -> Formatting.DARK_GRAY; case "Old" -> Formatting.GRAY; case "Unstable" -> Formatting.DARK_PURPLE; case "Young" -> Formatting.WHITE; case "Strong" -> Formatting.RED; case "Wise" -> Formatting.AQUA; case "Superior" -> Formatting.YELLOW; default -> Formatting.LIGHT_PURPLE;
        };
        MutableText title = Text.literal(dragonType.toUpperCase() + " DRAGON!").formatted(color);
        if ("Superior".equals(dragonType)) {
            title.formatted(Formatting.BOLD);
            NotificationUtils.playSound(client, SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
        NotificationUtils.showTitle(client, title, null);
    }

    public static void processTabList(List<String> lines, MinecraftClient client) {
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        if (!isTargetMap) { GameState.Dragon.eggState = "Scanning..."; return; }

        boolean foundEyePlaced = false, foundDragonSpawned = false, foundEggRespawning = false;
        int scannedEyes = 0; String scannedType = null;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("egg respawning")) foundEggRespawning = true;
            if (lowerLine.contains("dragon spawned")) foundDragonSpawned = true;
            Matcher eyeMatcher = ModConstants.EYE_PLACED_TAB_PATTERN.matcher(line);
            if (eyeMatcher.find()) { foundEyePlaced = true; try { scannedEyes = Integer.parseInt(eyeMatcher.group(1)); } catch (Exception ignored) {} }
            Matcher typeMatcher = ModConstants.DRAGON_TYPE_TAB_PATTERN.matcher(line);
            if (typeMatcher.find()) scannedType = typeMatcher.group(1);
        }

        if ("Scanning...".equals(GameState.Dragon.eggState)) {
            if (foundEggRespawning) { GameState.Dragon.eggState = "Respawning"; GameState.Dragon.eyes = 0; GameState.Dragon.playerEyes = 0; GameState.Dragon.type = null; GameState.Dragon.spawnTargetTime = 0;
            } else if (foundDragonSpawned) {
                if (scannedType != null) {
                    GameState.Dragon.eggState = "Hatched"; GameState.Dragon.type = scannedType; GameState.Dragon.eyes = 8;
                    if (ModConfig.enableDragonAlerts) {
                        final String finalType = scannedType;
                        client.execute(() -> showDragonSpawnAlert(client, finalType));
                    }
                } else { GameState.Dragon.eggState = "Hatching"; GameState.Dragon.eyes = 8; GameState.Dragon.type = null; }
            } else if (foundEyePlaced) { GameState.Dragon.eggState = "Ready"; GameState.Dragon.eyes = scannedEyes; GameState.Dragon.type = null; GameState.Dragon.spawnTargetTime = 0; }
        } else {
            if ("Ready".equals(GameState.Dragon.eggState) && System.currentTimeMillis() - GameState.Dragon.lastChatTime > 3000) {
                GameState.Dragon.eyes = scannedEyes;
                if (scannedEyes == 0) GameState.Dragon.playerEyes = 0;
            }
        }
    }
}