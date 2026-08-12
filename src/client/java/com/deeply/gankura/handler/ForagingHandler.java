package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;

public class ForagingHandler {

    public static void handleMessage(String unformattedMsg, Minecraft client) {
        // 木を一度に切り倒せた合図。チャットに埋もれると見逃すのでタイトルで知らせる
        if (ModConfig.INSTANCE.foraging.enableTreeFelledTitle
                && ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.TREE_FELLED_MSG)) {
            MutableComponent title = Component.literal("TREE FELLED!")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            client.execute(() -> NotificationUtils.showTitle(client, title, null));
            return;
        }

        // Wumpa のスポーンは足音のメッセージでしか分からないので、見逃さないようタイトルで知らせる。
        // Critter Safari 限定の告知なので、そのエリアにいるときだけ反応させる
        if (GameState.Server.isSafari()
                && ModConfig.INSTANCE.foraging.enableWumpaSpawnTitle
                && ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.WUMPA_SPAWN_MSG)) {
            MutableComponent title = Component.literal("WUMPA SPAWNED!")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            MutableComponent subtitle = Component.literal("Icy Biome")
                    .withStyle(ChatFormatting.YELLOW);
            client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
            return;
        }

        if (!ModConfig.INSTANCE.foraging.enableTreeMobTitle) return;

        // 切り倒した木からモブが降ってくるパターン。何が降ってきたかが重要なので、
        // モブ名をタイトル本体に、状況説明をサブタイトルに出す
        Matcher matcher = ModConstants.TREE_MOB_FELL_PATTERN.matcher(unformattedMsg);
        if (!matcher.find()) return;

        String mobName = matcher.group(1).trim();
        MutableComponent title = Component.literal(mobName)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        MutableComponent subtitle = Component.literal("Fell from the Tree!")
                .withStyle(ChatFormatting.YELLOW);
        client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
    }
}
