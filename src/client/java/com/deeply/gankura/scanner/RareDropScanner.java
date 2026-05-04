package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareDropScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("RareDropScanner");

    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 1200; // 60秒間スキャン

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    private static void scan(Minecraft client) {
        if (!GameState.Player.isLootScanning) {
            scanDurationTicks = 0;
            return;
        }

        if (!ModConstants.MAP_THE_END.equals(GameState.Server.map) &&
                !ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode)) return;

        if (client.level == null || client.player == null) return;

        scanDurationTicks++;
        if (scanDurationTicks > MAX_SCAN_DURATION) {
            GameState.Player.isLootScanning = false;
            scanDurationTicks = 0;
            return;
        }

        if (GameState.Player.hasShownDropAlert) return;

        // プレイヤー周囲30ブロック以内のArmorStandのみスキャン
        // Yarn: expand -> Mojang: inflate
        AABB scanBox = client.player.getBoundingBox().inflate(30.0);
        for (ArmorStand armorStand : client.level.getEntitiesOfClass(ArmorStand.class, scanBox, e -> true)) {
            if (armorStand.hasCustomName()) {
                Component customName = armorStand.getCustomName();
                if (customName == null) continue;

                String nameString = customName.getString();

                // 1. Tier Boost Core (金)
                if (nameString.contains("Tier Boost Core") && hasColor(customName, ChatFormatting.GOLD)) {
                    LootStats.addTierBoostCore();
                    notifyDrop(client, Component.literal("Tier Boost Core").withStyle(ChatFormatting.GOLD), ModConfig.INSTANCE.golem.enableDropAlerts);
                    break;
                }

                // 2. [Lvl 1] Golem (Legendary / Epic)
                else if (nameString.contains("[Lvl 1] Golem")) {
                    if (hasColor(customName, ChatFormatting.GOLD)) {
                        LootStats.addLegendaryGolemPet();
                        MutableComponent itemText = Component.literal("Golem").withStyle(ChatFormatting.GOLD).append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                        notifyDrop(client, itemText, ModConfig.INSTANCE.golem.enableDropAlerts);
                        break;
                    }
                    else if (hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                        LootStats.addEpicGolemPet();
                        MutableComponent itemText = Component.literal("Golem").withStyle(ChatFormatting.DARK_PURPLE).append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                        notifyDrop(client, itemText, ModConfig.INSTANCE.golem.enableDropAlerts);
                        break;
                    }
                }

                // 3. [Lvl 1] Ender Dragon (Legendary / Epic)
                else if (nameString.contains("[Lvl 1] Ender Dragon")) {
                    if (hasColor(customName, ChatFormatting.GOLD)) {
                        LootStats.addLegendaryDragonPet();
                        MutableComponent dragonText = Component.literal("Ender Dragon").withStyle(ChatFormatting.GOLD).append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                        notifyDrop(client, dragonText, ModConfig.INSTANCE.dragon.enableDragonDropAlerts);
                        break;
                    }
                    else if (hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                        LootStats.addEpicDragonPet();
                        MutableComponent dragonText = Component.literal("Ender Dragon").withStyle(ChatFormatting.DARK_PURPLE).append(Component.literal(" (Pet)").withStyle(ChatFormatting.GRAY));
                        notifyDrop(client, dragonText, ModConfig.INSTANCE.dragon.enableDragonDropAlerts);
                        break;
                    }
                }
            }
        }
    }

    private static void notifyDrop(Minecraft client, Component itemText, boolean isAlertEnabled) {
        if (isAlertEnabled) {
            MutableComponent title = Component.literal("DROP!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            NotificationUtils.showTitle(client, title, itemText, 5, 100, 20);

            Component playerName = client.player.getDisplayName();
            MutableComponent chatMsg = Component.empty()
                    .append(playerName)
                    .append(Component.literal(" has obtained ").withStyle(ChatFormatting.YELLOW))
                    .append(itemText)
                    .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

            NotificationUtils.sendSystemChat(client, chatMsg);
            NotificationUtils.playSound(client, SoundEvents.PLAYER_LEVELUP, 1.0f, 0.5f);
        }
        GameState.Player.hasShownDropAlert = true;
        GameState.Player.isLootScanning = false;
        LOGGER.info("Rare Drop Detected: " + itemText.getString());
    }

    private static boolean hasColor(Component text, ChatFormatting targetFormatting) {
        TextColor targetColor = TextColor.fromLegacyFormat(targetFormatting);
        if (targetColor == null) return false;

        TextColor selfColor = text.getStyle().getColor();
        if (selfColor != null && selfColor.equals(targetColor)) {
            return true;
        }

        for (Component sibling : text.getSiblings()) {
            if (hasColor(sibling, targetFormatting)) {
                return true;
            }
        }

        return false;
    }
}