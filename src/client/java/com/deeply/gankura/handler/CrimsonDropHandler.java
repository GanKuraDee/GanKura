package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrimsonDropHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CrimsonDropHandler");
    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 1200;

    // 全ボス共通ドロップ（"Hot Kuudra Key" を "Kuudra Key" より先にチェック）
    private static final List<String> COMMON_DROPS = List.of(
        "Hot Kuudra Key", "Kuudra Key", "Magma Urchin"
    );

    // ボス固有ドロップ
    private static final Map<String, List<String>> BOSS_DROPS = new LinkedHashMap<>();
    static {
        BOSS_DROPS.put("MAGMA BOSS",      List.of("Fire Fury Staff"));
        BOSS_DROPS.put("ASHFANG",         List.of("Fire Veil Wand"));
        BOSS_DROPS.put("BARBARIAN DUKE X",List.of("Flaming Fist"));
        BOSS_DROPS.put("BLADESOUL",       List.of("Ragnarock Axe"));
        BOSS_DROPS.put("MAGE OUTLAW",     List.of("Fire Freeze Staff", "Wand Of Strength"));
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    // Magma Boss は 2 分、その他は 2 分のリスポーンタイマー
    private static final long RESPAWN_MAGMA_BOSS_MS = 2 * 60 * 1000L;
    private static final long RESPAWN_DEFAULT_MS    = 2 * 60 * 1000L;

    // NetworkHandler から呼ばれる。"{BOSS} DOWN!" を検知してスキャン開始 & タイマーセット
    public static void handleMessage(String unformattedMsg) {
        for (String boss : BOSS_DROPS.keySet()) {
            if (unformattedMsg.contains(boss + " DOWN!")) {
                setRespawnTimer(boss);
                if (!GameState.CrimsonDrop.isScanning) {
                    GameState.CrimsonDrop.killedBoss = boss;
                    GameState.CrimsonDrop.isScanning = true;
                    GameState.CrimsonDrop.hasShownAlert = false;
                    scanDurationTicks = 0;
                }
                return;
            }
        }
    }

    private static void setRespawnTimer(String boss) {
        long duration = "MAGMA BOSS".equals(boss) ? RESPAWN_MAGMA_BOSS_MS : RESPAWN_DEFAULT_MS;
        long endTime = System.currentTimeMillis() + duration;
        switch (boss) {
            case "BARBARIAN DUKE X" -> GameState.BarbarianDukeX.respawnEndTime = endTime;
            case "BLADESOUL"        -> GameState.Bladesoul.respawnEndTime       = endTime;
            case "MAGE OUTLAW"      -> GameState.MageOutlaw.respawnEndTime      = endTime;
            case "ASHFANG"          -> GameState.Ashfang.respawnEndTime         = endTime;
            case "MAGMA BOSS"       -> GameState.MagmaBoss.respawnEndTime       = endTime;
        }
    }

    private static void scan(Minecraft client) {
        if (!GameState.CrimsonDrop.isScanning) {
            scanDurationTicks = 0;
            return;
        }

        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        if (!isCrimsonIsle) return;

        if (client.level == null || client.player == null) return;

        scanDurationTicks++;
        if (scanDurationTicks > MAX_SCAN_DURATION) {
            GameState.CrimsonDrop.isScanning = false;
            scanDurationTicks = 0;
            return;
        }

        if (GameState.CrimsonDrop.hasShownAlert) return;

        String killedBoss = GameState.CrimsonDrop.killedBoss;
        AABB scanBox = client.player.getBoundingBox().inflate(30.0);

        for (Entity entity : client.level.getEntitiesOfClass(ArmorStand.class, scanBox, e -> true)) {
            if (!(entity instanceof ArmorStand armorStand)) continue;
            if (!armorStand.hasCustomName()) continue;
            Component customName = armorStand.getCustomName();
            if (customName == null) continue;

            String nameStr = customName.getString();

            // Magma Cube Pet: "[Lvl 1] Magma Cube" に完全一致した場合のみ、色で Epic/Legendary を判別
            if ("MAGMA BOSS".equals(killedBoss) && nameStr.equals("[Lvl 1] Magma Cube")) {
                if (hasColor(customName, ChatFormatting.GOLD)) {
                    LootStats.addLegendaryMagmaCubePet();
                    Component itemName = Component.literal("Magma Cube").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                    notifyDrop(client, itemName);
                    return;
                } else if (hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                    LootStats.addEpicMagmaCubePet();
                    Component itemName = Component.literal("Magma Cube").withStyle(ChatFormatting.DARK_PURPLE)
                            .append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                    notifyDrop(client, itemName);
                    return;
                }
            }

            // 共通ドロップ
            for (String item : COMMON_DROPS) {
                if (nameStr.contains(item)) {
                    switch (item) {
                        case "Hot Kuudra Key" -> LootStats.addHotKuudraKey();
                        case "Kuudra Key"     -> LootStats.addKuudraKey();
                        case "Magma Urchin"   -> LootStats.addMagmaUrchin();
                    }
                    notifyDrop(client, customName);
                    return;
                }
            }

            // ボス固有ドロップ
            List<String> specificDrops = BOSS_DROPS.get(killedBoss);
            if (specificDrops != null) {
                for (String item : specificDrops) {
                    if (nameStr.contains(item)) {
                        switch (item) {
                            case "Fire Fury Staff"   -> LootStats.addFireFuryStaff();
                            case "Fire Veil Wand"    -> LootStats.addFireVeilWand();
                            case "Flaming Fist"      -> LootStats.addFlamingFist();
                            case "Ragnarock Axe"     -> LootStats.addRagnarockAxe();
                            case "Fire Freeze Staff" -> LootStats.addFireFreezeStaff();
                            case "Wand Of Strength"  -> LootStats.addWandOfStrength();
                        }
                        notifyDrop(client, customName);
                        return;
                    }
                }
            }
        }
    }

    private static boolean hasColor(Component text, ChatFormatting targetFormatting) {
        if (targetFormatting.getColor() == null) return false;
        int targetRgb = targetFormatting.getColor();
        TextColor selfColor = text.getStyle().getColor();
        if (selfColor != null && selfColor.getValue() == targetRgb) return true;
        for (Component sibling : text.getSiblings()) {
            if (hasColor(sibling, targetFormatting)) return true;
        }
        return false;
    }

    private static void notifyDrop(Minecraft client, Component itemName) {
        GameState.CrimsonDrop.hasShownAlert = true;
        GameState.CrimsonDrop.isScanning = false;
        LOGGER.info("Crimson Drop Detected: " + itemName.getString());

        if (!ModConfig.INSTANCE.crimsonIsle.enableCrimsonDropAlerts) return;

        MutableComponent title = Component.literal("§c§lDROP!");
        NotificationUtils.showTitle(client, title, itemName, 5, 100, 20);

        Component playerName = client.player.getDisplayName();
        MutableComponent chatMsg = Component.literal("")
                .append(playerName)
                .append(Component.literal(" has obtained ").withStyle(ChatFormatting.YELLOW))
                .append(itemName)
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));
        NotificationUtils.sendSystemChat(client, chatMsg);
        NotificationUtils.playSound(client, SoundEvents.PLAYER_LEVELUP, 1.0f, 0.5f);
    }
}
