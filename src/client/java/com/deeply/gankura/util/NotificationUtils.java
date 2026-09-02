package com.deeply.gankura.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class NotificationUtils {

    public static MutableComponent getGanKuraPrefix() {
        int netheriteColor = 0x443a3b;
        MutableComponent prefix = Component.literal("[").withStyle(Style.EMPTY.withColor(netheriteColor));

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
            prefix.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(color)));
        }
        prefix.append(Component.literal("] ").withStyle(Style.EMPTY.withColor(netheriteColor)));
        return prefix;
    }

    // ドロップ通知でアイテム名を挟む1文字。中身は見えないので何でもよい
    private static final String SPARKLE = "a";

    /**
     * ドロップ通知の副題。
     *
     * アイテム名と通算数を、まとめてちらつく1文字で挟む
     */
    public static MutableComponent dropSubtitle(Component itemName, int count) {
        TextColor color = rarityColor(itemName);
        return Component.empty()
                .append(sparkle(color))
                .append(Component.literal(" "))
                .append(itemName)
                .append(Component.literal(" #" + count).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(sparkle(color));
    }

    private static Component sparkle(TextColor color) {
        return Component.literal(SPARKLE).withStyle(style -> style.withObfuscated(true).withColor(color));
    }

    /**
     * アイテム名に付いている色。Hypixel ではこれがレアリティの色になる。
     *
     * 名前は色ごとに切れた部品の集まりになっていることがあるので、
     * 先に見つかった色を採る。分からなければ null(色を付けない)
     */
    private static TextColor rarityColor(Component itemName) {
        TextColor color = itemName.getStyle().getColor();
        if (color != null) return color;

        for (Component sibling : itemName.getSiblings()) {
            TextColor found = rarityColor(sibling);
            if (found != null) return found;
        }

        // 色コードを文字列のまま持っている場合。最初の1つを見る
        return legacyColor(itemName.getString());
    }

    private static TextColor legacyColor(String text) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '\u00a7') continue;

            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting == null) continue;

            // 色でない装飾コード(太字など)には色が無いので、その場合は次を見る
            TextColor color = TextColor.fromLegacyFormat(formatting);
            if (color != null) return color;
        }
        return null;
    }

    public static void showTitle(Minecraft client, Component title, Component subtitle) {
        showTitle(client, title, subtitle, 5, 70, 20);
    }

    public static void showTitle(Minecraft client, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (client.player == null) return;
        // 26.2: Gui.setTimes/setSubtitle/setTitle 廃止 -> Hud に移動
        client.gui.hud.setTimes(fadeIn, stay, fadeOut);
        client.gui.hud.setSubtitle(subtitle != null ? subtitle : Component.empty());
        client.gui.hud.setTitle(title);
    }

    public static void sendSystemChat(Minecraft client, Component message) {
        if (client.player == null) return;
        MutableComponent fullMessage = getGanKuraPrefix().append(message);

        /*
         * 26.1.2 における修正ポイント:
         * displayClientMessage -> sendSystemMessage
         * もしくは client.player.chat.addMessage(...)
         *
         * 多くの最新バージョンでは sendSystemMessage(Component) が標準です。
         */
        client.player.sendSystemMessage(fullMessage);
    }

    public static void playSound(Minecraft client, SoundEvent sound, float volume, float pitch) {
        if (client.player != null) {
            client.player.playSound(sound, volume, pitch);
        }
    }
}