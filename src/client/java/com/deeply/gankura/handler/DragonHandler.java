package com.deeply.gankura.handler;

import com.deeply.gankura.data.DragonAlertType;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class DragonHandler {

    public static boolean handleMessage(String msg, Minecraft client) {
        String cleanMsg = msg.replaceAll("§[0-9a-fk-or]", "");

        Matcher m1 = ModConstants.EYE_PLACED_CHAT_PATTERN.matcher(cleanMsg);
        if (m1.find()) {
            GameState.Dragon.eyes = Integer.parseInt(m1.group(1)); GameState.Dragon.eggState = "Ready";
            if (ModConstants.containsIgnoreCase(cleanMsg, "You placed a Summoning Eye!")) GameState.Dragon.playerEyes++;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m2 = ModConstants.EYE_PLACED_8_CHAT_PATTERN.matcher(cleanMsg);
        if (m2.find()) {
            GameState.Dragon.eyes = 8; GameState.Dragon.eggState = "Hatching";
            if (ModConstants.containsIgnoreCase(cleanMsg, "You placed a Summoning Eye! Brace yourselves!")) GameState.Dragon.playerEyes++;
            if (client.level != null) GameState.Dragon.spawnTargetTime = client.level.getGameTime() + 180;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        if (ModConstants.containsIgnoreCase(cleanMsg, "You recovered a Summoning Eye!")) {
            if (GameState.Dragon.playerEyes > 0) GameState.Dragon.playerEyes--;
            if (GameState.Dragon.eyes > 0) GameState.Dragon.eyes--;
            GameState.Dragon.eggState = "Ready"; GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m3 = ModConstants.DRAGON_SPAWN_PATTERN.matcher(cleanMsg);
        if (m3.find()) {
            String dragonType = m3.group(1);
            GameState.Dragon.eggState = "Hatched"; GameState.Dragon.type = dragonType; GameState.Dragon.eyes = 8; GameState.Dragon.spawnTargetTime = 0;
            if (client.level != null) {
                GameState.Dragon.fightStartTime = client.level.getGameTime(); GameState.Dragon.fightEndTime = 0;
                GameState.Dragon.top1Name = null; GameState.Dragon.top1Damage = 0; GameState.Dragon.top2Name = null; GameState.Dragon.top2Damage = 0; GameState.Dragon.top3Name = null; GameState.Dragon.top3Damage = 0;
            }
            // ★変更: 古い判定を削除し、新しい isAlertEnabledFor メソッドで判定
            if (isAlertEnabledFor(dragonType)) {
                client.execute(() -> showDragonSpawnAlert(client, dragonType));
            }
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }

        Matcher m4 = ModConstants.DRAGON_DOWN_PATTERN.matcher(cleanMsg);
        if (m4.find()) {
            GameState.Dragon.eggState = "Respawning"; GameState.Dragon.spawnTargetTime = 0; GameState.Dragon.type = null; GameState.Dragon.eyes = 0;
            if (client.level != null) GameState.Dragon.fightEndTime = client.level.getGameTime();
            GameState.Dragon.lastChatTime = System.currentTimeMillis();
            GameState.Player.isLootScanning = true; GameState.Player.hasShownDropAlert = false;
            return true;
        }

        boolean isRecentKill = GameState.Dragon.fightEndTime > 0 && client.level != null && (client.level.getGameTime() - GameState.Dragon.fightEndTime < 400);
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

        if (ModConstants.containsIgnoreCase(cleanMsg, ModConstants.DRAGON_EGG_SPAWNED_MSG)) {
            GameState.Dragon.eggState = "Ready"; GameState.Dragon.type = null; GameState.Dragon.eyes = 0; GameState.Dragon.playerEyes = 0; GameState.Dragon.spawnTargetTime = 0;
            GameState.Dragon.lastChatTime = System.currentTimeMillis(); return true;
        }
        return false;
    }

    private static void processDragonResult(Matcher matcher, Minecraft client) {
        if (client.level == null) return;
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

    private static void printDragonResult(Minecraft client, String dps, String duration, double durationSeconds, int lq) {
        client.execute(() -> {
            if (client.player != null) {
                if (ModConfig.INSTANCE.theEnd.showDragonDpsChat && dps != null && duration != null) {
                    MutableComponent msg = Component.literal(String.format("§dYour Dragon DPS: §l§o%s §r§7(%s) ", dps, duration));
                    if (durationSeconds > 0 && GameState.Dragon.top1Damage > 0) {
                        MutableComponent hoverText = Component.literal("§d§lTop 3 DPS\n");
                        hoverText.append(Component.literal(String.format("§e#1 §f%s §7- §d§l§o%s", GameState.Dragon.top1Name, formatDps(GameState.Dragon.top1Damage / durationSeconds))));
                        if (GameState.Dragon.top2Damage > 0) hoverText.append(Component.literal(String.format("\n§6#2 §f%s §7- §d§l§o%s", GameState.Dragon.top2Name, formatDps(GameState.Dragon.top2Damage / durationSeconds))));
                        if (GameState.Dragon.top3Damage > 0) hoverText.append(Component.literal(String.format("\n§c#3 §f%s §7- §d§l§o%s", GameState.Dragon.top3Name, formatDps(GameState.Dragon.top3Damage / durationSeconds))));
                        // Yarnの HoverEvent.ShowText は HoverEvent.Action.SHOW_TEXT になります。また、.setStyle は .withStyle になります。
                        msg.append(Component.literal("§d§l§o[HOVER]")
                                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverText))));
                    }
                    NotificationUtils.sendSystemChat(client, msg);
                }
                if (ModConfig.INSTANCE.theEnd.showDragonLootQualityChat) {
                    NotificationUtils.sendSystemChat(client, Component.literal(String.format("§dYour Dragon Loot Quality: §l§o%d", lq)));
                    String dropsMsg = String.format("§7[Lvl 1] §6Ender Dragon: %s §8| §7[Lvl 1] §5Ender Dragon: %s", (lq >= 450) ? "§a✔" : "§c✘", (lq >= 350) ? "§a✔" : "§c✘");
                    NotificationUtils.sendSystemChat(client, Component.literal(dropsMsg));
                }
            }
        });
    }

    private static String formatDps(double dps) { return dps >= 1000 ? String.format("%,.1fk", dps / 1000.0) : String.format("%,.1f", dps); }

    // ★追加: ドラゴンの種類に応じてアラート設定がONになっているか確認するメソッド
    private static boolean isAlertEnabledFor(String dragonType) {
        if (!ModConfig.INSTANCE.theEnd.enableDragonSpawnAlert) return false;
        // 未知のドラゴンは fromTypeName が null を返し、リストにも含まれないため表示しない
        DragonAlertType type = DragonAlertType.fromTypeName(dragonType);
        return type != null && ModConfig.INSTANCE.theEnd.dragonSpawnAlerts.contains(type);
    }

    // ★Utilsから引き継いだドラゴンスポーンの表示処理
    private static void showDragonSpawnAlert(Minecraft client, String dragonType) {
        ChatFormatting color = switch (dragonType) {
            case "Protector" -> ChatFormatting.DARK_GRAY; case "Old" -> ChatFormatting.GRAY; case "Unstable" -> ChatFormatting.DARK_PURPLE; case "Young" -> ChatFormatting.WHITE; case "Strong" -> ChatFormatting.RED; case "Wise" -> ChatFormatting.AQUA; case "Superior" -> ChatFormatting.YELLOW; default -> ChatFormatting.LIGHT_PURPLE;
        };
        MutableComponent title = Component.literal(dragonType.toUpperCase() + " DRAGON!").withStyle(color);
        if ("Superior".equals(dragonType)) {
            title.withStyle(ChatFormatting.BOLD);
            NotificationUtils.playSound(client, SoundEvents.WITHER_SPAWN, 1.0f, 1.0f);
        }
        NotificationUtils.showTitle(client, title, null);
    }

    public static void processTabList(List<String> lines, Minecraft client) {
        boolean isTargetMap = GameState.Server.isTheEnd();
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
                    // ★変更: 古い判定を削除し、新しい isAlertEnabledFor メソッドで判定
                    if (isAlertEnabledFor(scannedType)) {
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