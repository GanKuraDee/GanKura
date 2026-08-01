package com.deeply.gankura.handler;

import com.deeply.gankura.data.CrimsonRareDrop;
import com.deeply.gankura.data.GameState;
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

import java.util.Comparator;
import java.util.List;

public class CrimsonDropHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("CrimsonDropHandler");
    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 1200;

    // 撃破メッセージ "{BOSS} DOWN!" の検知に使うボス名。
    // リスポーンタイマーはドロップの設定と無関係に動かす必要があるため、ドロップ一覧とは別に持つ
    private static final List<String> BOSS_NAMES = List.of(
        "MAGMA BOSS", "ASHFANG", "BARBARIAN DUKE X", "BLADESOUL", "MAGE OUTLAW"
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    // Magma Boss は 2 分、その他は 2 分のリスポーンタイマー
    private static final long RESPAWN_MAGMA_BOSS_MS = 2 * 60 * 1000L;
    private static final long RESPAWN_DEFAULT_MS    = 2 * 60 * 1000L;

    // NetworkHandler から呼ばれる。"{BOSS} DOWN!" を検知してスキャン開始 & タイマーセット
    public static void handleMessage(String unformattedMsg) {
        for (String boss : BOSS_NAMES) {
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
            if (nameStr.equals("[Lvl 1] Magma Cube")) {
                if (isTracked(CrimsonRareDrop.LEGENDARY_MAGMA_CUBE_PET, killedBoss)
                        && hasColor(customName, ChatFormatting.GOLD)) {
                    CrimsonRareDrop.LEGENDARY_MAGMA_CUBE_PET.increment();
                    Component itemName = Component.literal("Magma Cube").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                    notifyDrop(client, itemName);
                    return;
                } else if (isTracked(CrimsonRareDrop.EPIC_MAGMA_CUBE_PET, killedBoss)
                        && hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                    CrimsonRareDrop.EPIC_MAGMA_CUBE_PET.increment();
                    Component itemName = Component.literal("Magma Cube").withStyle(ChatFormatting.DARK_PURPLE)
                            .append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                    notifyDrop(client, itemName);
                    return;
                }
            }

            // アイテム名による判定。"Hot Kuudra Key" が "Kuudra Key" に埋もれないよう、
            // 長い名前から順に照合する
            for (CrimsonRareDrop drop : nameMatchedDrops(killedBoss)) {
                if (nameStr.contains(drop.itemName())) {
                    drop.increment();
                    notifyDrop(client, customName);
                    return;
                }
            }
        }
    }

    // 設定画面のドラッグリストで有効になっていて、かつ撃破したボスのドロップになりうるものだけを返す
    private static List<CrimsonRareDrop> nameMatchedDrops(String killedBoss) {
        return ModConfig.INSTANCE.crimsonIsle.trackedCrimsonDrops.stream()
                .filter(drop -> drop.itemName() != null)
                .filter(drop -> drop.matchesBoss(killedBoss))
                .sorted(Comparator.comparingInt((CrimsonRareDrop drop) -> drop.itemName().length()).reversed())
                .toList();
    }

    private static boolean isTracked(CrimsonRareDrop drop, String killedBoss) {
        return drop.matchesBoss(killedBoss) && ModConfig.INSTANCE.crimsonIsle.trackedCrimsonDrops.contains(drop);
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
