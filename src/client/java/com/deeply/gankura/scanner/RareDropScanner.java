package com.deeply.gankura.scanner;

import com.deeply.gankura.data.DragonRareDrop;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.GolemRareDrop;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareDropScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("RareDropScanner");

    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 1200; // 60秒間スキャン

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    private static void scan(MinecraftClient client) {
        if (!GameState.Player.isLootScanning) {
            scanDurationTicks = 0;
            return;
        }

        if (!ModConstants.MAP_THE_END.equals(GameState.Server.map) &&
                !ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode)) return;

        if (client.world == null || client.player == null) return;

        scanDurationTicks++;
        if (scanDurationTicks > MAX_SCAN_DURATION) {
            GameState.Player.isLootScanning = false;
            scanDurationTicks = 0;
            return;
        }

        if (GameState.Player.hasShownDropAlert) return;

        // プレイヤー周囲30ブロック以内のArmorStandのみスキャン
        Box scanBox = client.player.getBoundingBox().expand(30.0);
        for (Entity entity : client.world.getEntitiesByClass(ArmorStandEntity.class, scanBox, e -> true)) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (armorStand.hasCustomName()) {
                    Text customName = armorStand.getCustomName();
                    if (customName == null) continue;

                    String nameString = customName.getString();

                    // 1. Tier Boost Core (金)
                    if (isTracked(GolemRareDrop.TIER_BOOST_CORE)
                            && ModConstants.containsIgnoreCase(nameString, "Tier Boost Core") && hasColor(customName, Formatting.GOLD)) {
                        LootStats.addTierBoostCore();
                        notifyDrop(client, Text.literal("Tier Boost Core").formatted(Formatting.GOLD), ModConfig.INSTANCE.theEnd.enableDropAlerts);
                        break;
                    }

                    // 2. [Lvl 1] Golem (Legendary / Epic)
                    else if (ModConstants.containsIgnoreCase(nameString, "[Lvl 1] Golem")) {
                        if (isTracked(GolemRareDrop.LEGENDARY_GOLEM_PET) && hasColor(customName, Formatting.GOLD)) {
                            LootStats.addLegendaryGolemPet();
                            MutableText itemText = Text.literal("Golem").formatted(Formatting.GOLD).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            notifyDrop(client, itemText, ModConfig.INSTANCE.theEnd.enableDropAlerts);
                            break;
                        }
                        else if (isTracked(GolemRareDrop.EPIC_GOLEM_PET) && hasColor(customName, Formatting.DARK_PURPLE)) {
                            LootStats.addEpicGolemPet();
                            MutableText itemText = Text.literal("Golem").formatted(Formatting.DARK_PURPLE).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            notifyDrop(client, itemText, ModConfig.INSTANCE.theEnd.enableDropAlerts);
                            break;
                        }
                    }

                    // 3. [Lvl 1] Ender Dragon (Legendary / Epic)
                    else if (ModConstants.containsIgnoreCase(nameString, "[Lvl 1] Ender Dragon")) {
                        if (isTracked(DragonRareDrop.LEGENDARY_DRAGON_PET) && hasColor(customName, Formatting.GOLD)) {
                            LootStats.addLegendaryDragonPet();
                            MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.GOLD).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            notifyDrop(client, dragonText, ModConfig.INSTANCE.theEnd.enableDragonDropAlerts);
                            break;
                        }
                        else if (isTracked(DragonRareDrop.EPIC_DRAGON_PET) && hasColor(customName, Formatting.DARK_PURPLE)) {
                            LootStats.addEpicDragonPet();
                            MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.DARK_PURPLE).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            notifyDrop(client, dragonText, ModConfig.INSTANCE.theEnd.enableDragonDropAlerts);
                            break;
                        }
                    }
                }
            }
        }
    }

    // 設定画面のドラッグリストから外されたドロップはスキャンしない
    private static boolean isTracked(GolemRareDrop drop) {
        return ModConfig.INSTANCE.theEnd.trackedGolemDrops.contains(drop);
    }

    private static boolean isTracked(DragonRareDrop drop) {
        return ModConfig.INSTANCE.theEnd.trackedDragonDrops.contains(drop);
    }

    private static void notifyDrop(MinecraftClient client, Text itemText, boolean isAlertEnabled) {
        if (isAlertEnabled) {
            MutableText title = Text.literal("DROP!").formatted(Formatting.RED, Formatting.BOLD);
            NotificationUtils.showTitle(client, title, itemText, 5, 100, 20);

            Text playerName = client.player.getDisplayName();
            MutableText chatMsg = Text.empty()
                    .append(playerName)
                    .append(Text.literal(" has obtained ").formatted(Formatting.YELLOW))
                    .append(itemText)
                    .append(Text.literal("!").formatted(Formatting.YELLOW));

            NotificationUtils.sendSystemChat(client, chatMsg);
            NotificationUtils.playSound(client, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        }
        GameState.Player.hasShownDropAlert = true;
        GameState.Player.isLootScanning = false;
        LOGGER.info("Rare Drop Detected: " + itemText.getString());
    }

    private static boolean hasColor(Text text, Formatting targetFormatting) {
        TextColor targetColor = TextColor.fromFormatting(targetFormatting);
        if (targetColor == null) return false;

        TextColor selfColor = text.getStyle().getColor();
        if (selfColor != null && selfColor.equals(targetColor)) {
            return true;
        }

        for (Text sibling : text.getSiblings()) {
            if (hasColor(sibling, targetFormatting)) {
                return true;
            }
        }

        return false;
    }
}