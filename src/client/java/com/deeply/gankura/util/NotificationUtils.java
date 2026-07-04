package com.deeply.gankura.util;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

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