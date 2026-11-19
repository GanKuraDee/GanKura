package com.deeply.gankura.handler;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 釣っている Hotspot が消えたときに知らせる。
 *
 * Hotspot が消えると、Hypixel がチャットに "The Fishing Hotspot faded away..." と出すので、それを拾う
 */
public class HotspotAlertHandler {

    // Hotspot が消えたときのチャット
    private static final String FADED_MESSAGE = "The Fishing Hotspot faded away...";

    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 40;
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 0.7f;

    public static void handleMessage(String unformattedMsg, Minecraft client) {
        if (!ModConfig.Fishing.showHotspotGoneTitle) return;
        if (!FADED_MESSAGE.equals(unformattedMsg.trim())) return;

        client.execute(() -> alert(client));
    }

    private static void alert(Minecraft client) {
        NotificationUtils.showTitle(client, Component.literal("§d§lHotspot §c§lis gone"), null,
                TITLE_FADE, TITLE_STAY, TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, SOUND_VOLUME, SOUND_PITCH);
    }
}
