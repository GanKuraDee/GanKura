package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareDropScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("ItemDropScanner");

    // スキャン継続時間カウント用
    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 1200; // 60秒 (20 tick * 60)

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            scan(client);
        });
    }

    private static void scan(MinecraftClient client) {
        if (!GameState.isLootScanning) {
            scanDurationTicks = 0;
            return;
        }

        // エリアチェック
        if (!ModConstants.MAP_THE_END.equals(GameState.map) &&
                !ModConstants.MODE_COMBAT_3.equals(GameState.mode)) return;

        scanDurationTicks++;

        if (scanDurationTicks > MAX_SCAN_DURATION) {
            GameState.isLootScanning = false;
            return;
        }

        if (client.world == null) return;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity) {
                if (entity.hasCustomName()) {
                    Text customName = entity.getCustomName();
                    if (customName == null) continue;

                    String nameString = customName.getString();

                    // 1. Tier Boost Core (金)
                    if (nameString.contains("Tier Boost Core")) {
                        LootStats.addTierBoostCore();
                        MutableText tbcText = Text.literal("Tier Boost Core").formatted(Formatting.GOLD);
                        // ★変更: Golemの設定(enableDropAlerts)を渡す
                        notifyDrop(client, tbcText, ModConfig.enableDropAlerts);
                        break;
                    }

                    // 2. [Lvl 1] Golem
                    else if (nameString.contains("[Lvl 1] Golem")) {
                        if (hasColor(customName, Formatting.GOLD)) {
                            LootStats.addLegendaryGolemPet();
                            MutableText golemText = Text.literal("Golem").formatted(Formatting.GOLD);
                            golemText.append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            // ★変更: Golemの設定を渡す
                            notifyDrop(client, golemText, ModConfig.enableDropAlerts);
                            break;
                        }
                        else if (hasColor(customName, Formatting.DARK_PURPLE) || hasColor(customName, Formatting.LIGHT_PURPLE)) {
                            LootStats.addEpicGolemPet();
                            MutableText golemText = Text.literal("Golem").formatted(Formatting.DARK_PURPLE);
                            golemText.append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            // ★変更: Golemの設定を渡す
                            notifyDrop(client, golemText, ModConfig.enableDropAlerts);
                            break;
                        }
                    }

                    // 3. [Lvl 1] Ender Dragon
                    else if (nameString.contains("[Lvl 1] Ender Dragon")) {
                        if (hasColor(customName, Formatting.GOLD)) {
                            LootStats.addLegendaryDragonPet();
                            MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.GOLD);
                            dragonText.append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            // ★変更: Dragon専用の設定(enableDragonDropAlerts)を渡す
                            notifyDrop(client, dragonText, ModConfig.enableDragonDropAlerts);
                            break;
                        }
                        else if (hasColor(customName, Formatting.DARK_PURPLE) || hasColor(customName, Formatting.LIGHT_PURPLE)) {
                            LootStats.addEpicDragonPet();
                            MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.DARK_PURPLE);
                            dragonText.append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                            // ★変更: Dragon専用の設定を渡す
                            notifyDrop(client, dragonText, ModConfig.enableDragonDropAlerts);
                            break;
                        }
                    }
                }
            }
        }
    }

    // ★変更: isAlertEnabled という引数を増やし、呼び出し元でON/OFFを決定させる
    private static void notifyDrop(MinecraftClient client, Text itemText, boolean isAlertEnabled) {
        if (isAlertEnabled) {
            NotificationUtils.showDropAlert(client, itemText);
            NotificationUtils.sendDropChatMessage(client, itemText);
            NotificationUtils.playDropSound(client);
        }
        GameState.hasShownDropAlert = true;
        GameState.isLootScanning = false;
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