package com.deeply.gankura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NotificationUtils {

    public static void showAwakeningAlert(MinecraftClient client) {
        if (client.player == null) return;
        client.inGameHud.setTitle(Text.literal("GOLEM STAGE 4").formatted(Formatting.RED, Formatting.BOLD));
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitleTicks(0, 70, 20);
    }

    public static void playAwakeningSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
    }

    public static void showSummonedAlert(MinecraftClient client) {
        if (client.player == null) return;
        client.inGameHud.setTitle(Text.literal("GOLEM STAGE 5").formatted(Formatting.DARK_RED, Formatting.BOLD));
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitleTicks(0, 70, 20);
    }

    public static void playSummonedSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }

    public static void showDropAlert(MinecraftClient client, Text itemText) {
        if (client.player == null) return;

        // メインタイトル: DROP! (赤色・太字)
        client.inGameHud.setTitle(Text.literal("DROP!").formatted(Formatting.RED, Formatting.BOLD));

        // サブタイトル: アイテム名
        // ★修正: タイトル表示用にコピーを作成し、ここで太字(BOLD)を適用する
        // これにより、元の itemText (チャット用) は細字のまま維持される
        MutableText titleText = itemText.copy().formatted(Formatting.BOLD);

        client.inGameHud.setSubtitle(titleText);
        client.inGameHud.setTitleTicks(0, 100, 20);
    }

    public static void sendDropChatMessage(MinecraftClient client, Text itemText) {
        if (client.player == null) return;

        // [GanKura] (緑色)
        MutableText message = Text.literal("[GanKura] ").formatted(Formatting.GREEN);

        // RARE DROP! (金色・太字)
        MutableText rareDrop = Text.literal("RARE DROP! ").formatted(Formatting.GOLD, Formatting.BOLD);

        // メッセージ結合: [GanKura] RARE DROP! [アイテム名]
        // itemText は ItemDropScanner で生成された細字の状態
        message.append(rareDrop).append(itemText);

        client.player.sendMessage(message, false);
    }

    public static void playDropSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
    }
}