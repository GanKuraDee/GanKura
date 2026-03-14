package com.deeply.gankura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class NotificationUtils {

    // GanKuraのプレフィックス生成 (そのまま)
    public static MutableText getGanKuraPrefix() {
        int netheriteColor = 0x443a3b;
        MutableText prefix = Text.literal("[").setStyle(Style.EMPTY.withColor(netheriteColor));

        String text = "GanKura";
        int startColor = 0xAAAAAA; int endColor = 0xFFFFFF;
        int length = text.length();
        int r1 = (startColor >> 16) & 0xFF; int g1 = (startColor >> 8) & 0xFF; int b1 = startColor & 0xFF;
        int r2 = (endColor >> 16) & 0xFF; int g2 = (endColor >> 8) & 0xFF; int b2 = endColor & 0xFF;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            int color = (r << 16) | (g << 8) | b;
            prefix.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(color)));
        }
        prefix.append(Text.literal("] ").setStyle(Style.EMPTY.withColor(netheriteColor)));
        return prefix;
    }

    // =======================================================
    // 汎用ユーティリティメソッド群
    // =======================================================

    /**
     * 画面中央にタイトルとサブタイトルを表示する (デフォルトの時間設定)
     * バニラのデフォルトは フェードイン: 10Tick(0.5秒), 表示: 70Tick(3.5秒), フェードアウト: 20Tick(1.0秒) です。
     */
    public static void showTitle(MinecraftClient client, Text title, Text subtitle) {
        // 既存のコードが壊れないように、引数が3つの場合はデフォルト時間を指定して下のメソッドを呼ぶ
        showTitle(client, title, subtitle, 10, 70, 20);
    }

    /**
     * ★追加: 画面中央にタイトルとサブタイトルを表示する (時間を指定可能)
     * 時間の単位は「Tick」です。(20Tick = 1秒)
     *
     * @param fadeIn  フェードインにかかる時間 (Tick)
     * @param stay    画面に完全に表示され続ける時間 (Tick)
     * @param fadeOut フェードアウトにかかる時間 (Tick)
     */
    public static void showTitle(MinecraftClient client, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        if (client.player == null) return;

        // 1. タイトルの表示時間を設定する
        client.inGameHud.setTitleTicks(fadeIn, stay, fadeOut);

        // 2. サブタイトルとタイトルをセットして表示をトリガーする
        client.inGameHud.setSubtitle(subtitle != null ? subtitle : Text.empty());
        client.inGameHud.setTitle(title);
    }

    // GanKuraのプレフィックス付きでチャット欄にメッセージを送信する
    public static void sendSystemChat(MinecraftClient client, Text message) {
        if (client.player == null) return;
        MutableText fullMessage = getGanKuraPrefix().append(message);
        client.player.sendMessage(fullMessage, false);
    }

    // 指定した音を鳴らす
    public static void playSound(MinecraftClient client, SoundEvent sound, float volume, float pitch) {
        if (client.player != null) {
            client.player.playSound(sound, volume, pitch);
        }
    }
}