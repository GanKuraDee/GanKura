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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareDropScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("RareDropScanner");

    private static int scanDurationTicks = 0;
    private static final int MAX_SCAN_DURATION = 600; // 30秒間スキャン

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

        // ボスごとのルートプールの分離
        long currentTime = client.world.getTime();
        boolean scanGolemPool = GameState.Golem.fightEndTime > 0 && (currentTime - GameState.Golem.fightEndTime) <= MAX_SCAN_DURATION;
        boolean scanDragonPool = GameState.Dragon.fightEndTime > 0 && (currentTime - GameState.Dragon.fightEndTime) <= MAX_SCAN_DURATION;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand) {
                Text customName = armorStand.getCustomName();
                if (customName != null) {
                    // 色情報(§)を含んだ完全な文字列を復元
                    String legacyName = toLegacyString(customName);
                    // 色情報を抜いた純粋な文字列
                    String plainName = legacyName.replaceAll("§[0-9a-fk-or]", "");

                    // =======================================================
                    // ★修正: 柔軟かつ強固な判定ロジック
                    // "[Lvl 1] " (最後にスペースあり) で検索することで、[Lvl 100]などを
                    // 確実に弾きつつ、見えないカラーコードのズレによる検知漏れを防ぎます。
                    // =======================================================

                    // --- ゴーレム専用ドロップ ---
                    if (scanGolemPool) {
                        if (plainName.contains("Tier Boost Core") && legacyName.contains("§6")) {
                            LootStats.addTierBoostCore();
                            notifyDrop(client, Text.literal("Tier Boost Core").formatted(Formatting.GOLD), ModConfig.enableDropAlerts);
                            break;
                        }
                        // "[Lvl 1] " と "Golem" が両方含まれているか
                        else if (plainName.contains("[Lvl 1] ") && plainName.contains("Golem")) {
                            if (legacyName.contains("§6")) {
                                LootStats.addLegendaryGolemPet();
                                MutableText itemText = Text.literal("Golem").formatted(Formatting.GOLD).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                                notifyDrop(client, itemText, ModConfig.enableDropAlerts);
                                break;
                            } else if (legacyName.contains("§5")) {
                                LootStats.addEpicGolemPet();
                                MutableText itemText = Text.literal("Golem").formatted(Formatting.DARK_PURPLE).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                                notifyDrop(client, itemText, ModConfig.enableDropAlerts);
                                break;
                            }
                        }
                    }

                    // --- ドラゴン専用ドロップ ---
                    if (scanDragonPool) {
                        // "[Lvl 1] " と "Ender Dragon" が両方含まれているか
                        if (plainName.contains("[Lvl 1] ") && plainName.contains("Ender Dragon")) {
                            if (legacyName.contains("§6")) {
                                LootStats.addLegendaryDragonPet();
                                MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.GOLD).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                                notifyDrop(client, dragonText, ModConfig.enableDragonDropAlerts);
                                break;
                            } else if (legacyName.contains("§5")) {
                                LootStats.addEpicDragonPet();
                                MutableText dragonText = Text.literal("Ender Dragon").formatted(Formatting.DARK_PURPLE).append(Text.literal(" (Pet)").formatted(Formatting.GRAY));
                                notifyDrop(client, dragonText, ModConfig.enableDragonDropAlerts);
                                break;
                            }
                        }
                    }
                }
            }
        }
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

    private static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                Integer rgb = color.getRgb();
                for (Formatting f : Formatting.values()) {
                    if (f.isColor() && f.getColorValue() != null && f.getColorValue().equals(rgb)) {
                        sb.append("§").append(f.getCode());
                        break;
                    }
                }
            }
            if (style.isObfuscated()) sb.append("§k");
            if (style.isBold()) sb.append("§l");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isItalic()) sb.append("§o");

            sb.append(part);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}