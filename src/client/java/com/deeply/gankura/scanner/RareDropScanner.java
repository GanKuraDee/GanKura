package com.deeply.gankura.scanner;

import com.deeply.gankura.data.DragonRareDrop;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.GolemRareDrop;
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

        if (!GameState.Server.isTheEnd()) return;

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
                if (isTracked(GolemRareDrop.TIER_BOOST_CORE)
                        && ModConstants.containsIgnoreCase(nameString, "Tier Boost Core") && hasColor(customName, ChatFormatting.GOLD)) {
                    LootStats.addTierBoostCore();
                    notifyDrop(client, Component.literal("Tier Boost Core").withStyle(ChatFormatting.GOLD),
                            GolemRareDrop.TIER_BOOST_CORE.count(), ModConfig.INSTANCE.theEnd.enableDropAlerts);
                    break;
                }

                // 2. [Lvl 1] Golem (Legendary / Epic)
                else if (ModConstants.containsIgnoreCase(nameString, "[Lvl 1] Golem")) {
                    if (isTracked(GolemRareDrop.LEGENDARY_GOLEM_PET) && hasColor(customName, ChatFormatting.GOLD)) {
                        LootStats.addLegendaryGolemPet();
                        MutableComponent itemText = Component.literal("[Lvl 1] ").withStyle(ChatFormatting.GRAY).append(Component.literal("Golem").withStyle(ChatFormatting.GOLD));
                        notifyDrop(client, itemText, GolemRareDrop.LEGENDARY_GOLEM_PET.count(),
                                ModConfig.INSTANCE.theEnd.enableDropAlerts);
                        break;
                    }
                    else if (isTracked(GolemRareDrop.EPIC_GOLEM_PET) && hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                        LootStats.addEpicGolemPet();
                        MutableComponent itemText = Component.literal("[Lvl 1] ").withStyle(ChatFormatting.GRAY).append(Component.literal("Golem").withStyle(ChatFormatting.DARK_PURPLE));
                        notifyDrop(client, itemText, GolemRareDrop.EPIC_GOLEM_PET.count(),
                                ModConfig.INSTANCE.theEnd.enableDropAlerts);
                        break;
                    }
                }

                // 3. [Lvl 1] Ender Dragon (Legendary / Epic)
                else if (ModConstants.containsIgnoreCase(nameString, "[Lvl 1] Ender Dragon")) {
                    if (isTracked(DragonRareDrop.LEGENDARY_DRAGON_PET) && hasColor(customName, ChatFormatting.GOLD)) {
                        LootStats.addLegendaryDragonPet();
                        MutableComponent dragonText = Component.literal("[Lvl 1] ").withStyle(ChatFormatting.GRAY).append(Component.literal("Ender Dragon").withStyle(ChatFormatting.GOLD));
                        notifyDrop(client, dragonText, DragonRareDrop.LEGENDARY_DRAGON_PET.count(),
                                ModConfig.INSTANCE.theEnd.enableDragonDropAlerts);
                        break;
                    }
                    else if (isTracked(DragonRareDrop.EPIC_DRAGON_PET) && hasColor(customName, ChatFormatting.DARK_PURPLE)) {
                        LootStats.addEpicDragonPet();
                        MutableComponent dragonText = Component.literal("[Lvl 1] ").withStyle(ChatFormatting.GRAY).append(Component.literal("Ender Dragon").withStyle(ChatFormatting.DARK_PURPLE));
                        notifyDrop(client, dragonText, DragonRareDrop.EPIC_DRAGON_PET.count(),
                                ModConfig.INSTANCE.theEnd.enableDragonDropAlerts);
                        break;
                    }
                }
            }
        }
    }

    // アイテム名に通算数を添える。何個目のドロップかがその場で分かるようにする
    private static Component withCount(Component itemText, int count) {
        return Component.empty()
                .append(itemText)
                .append(Component.literal(" #" + count).withStyle(ChatFormatting.GRAY));
    }

    // 設定画面のドラッグリストから外されたドロップはスキャンしない
    private static boolean isTracked(GolemRareDrop drop) {
        return ModConfig.INSTANCE.theEnd.trackedGolemDrops.contains(drop);
    }

    private static boolean isTracked(DragonRareDrop drop) {
        return ModConfig.INSTANCE.theEnd.trackedDragonDrops.contains(drop);
    }

    private static void notifyDrop(Minecraft client, Component itemText, int count, boolean isAlertEnabled) {
        if (isAlertEnabled) {
            MutableComponent title = Component.literal("DROP!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            NotificationUtils.showTitle(client, title, withCount(itemText, count), 5, 100, 20);

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