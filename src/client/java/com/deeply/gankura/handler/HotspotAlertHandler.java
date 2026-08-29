package com.deeply.gankura.handler;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 釣っている Hotspot が消えたときに知らせる。
 *
 * Hotspot は中心に "HOTSPOT" という名前のアーマースタンドが立っている。
 * 浮きのそばにあるものを覚えておき、それが消えたら自分の Hotspot が切れたとみなす。
 * チャットの文言に依らないので、文言が変わっても壊れない。
 */
public class HotspotAlertHandler {

    // Hypixel が Hotspot の中心に出しているアーマースタンドの名前
    private static final String HOTSPOT_NAME = "HOTSPOT";
    // 浮きから Hotspot を探す半径(ブロック)。輪の中で釣っているかを見る
    private static final double HOOK_SEARCH_RADIUS = 5.0;
    // 消えたときに知らせる距離(ブロック)。遠くへ移動した後は知らせない
    private static final double ALERT_RANGE = 30.0;
    // 覚えている Hotspot を探し直す間隔(tick)
    private static final int SEARCH_INTERVAL_TICKS = 10;
    // 消えてから知らせるまでの待ち(ミリ秒)。
    // サーバー移動でもアーマースタンドは消えるので、少し置いてから確かめる
    private static final long ALERT_DELAY_MS = 150;

    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 40;
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 0.7f;

    // 今釣っている Hotspot のアーマースタンドと、その位置。
    // 消えた後も距離を見るので、位置は別に控えておく
    private static ArmorStandEntity tracked;
    private static Vec3d trackedPos;
    // 消えたと気づいた時刻。0 ならまだ消えていない
    private static long goneMillis;
    private static int ticks;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HotspotAlertHandler::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            reset();
            return;
        }
        if (!ModConfig.INSTANCE.fishing.showHotspotGoneTitle) {
            reset();
            return;
        }

        if (tracked != null && tracked.isRemoved() && goneMillis == 0) {
            goneMillis = System.currentTimeMillis();
        }

        if (goneMillis != 0) {
            if (System.currentTimeMillis() - goneMillis < ALERT_DELAY_MS) return;

            // 遠くへ移動していたなら、もうその Hotspot で釣っていない
            if (trackedPos != null && client.player.getEntityPos().distanceTo(trackedPos) <= ALERT_RANGE) {
                alert(client);
            }
            reset();
            return;
        }

        if (++ticks < SEARCH_INTERVAL_TICKS) return;
        ticks = 0;

        // 浮きを出している間だけ覚え直す。
        // 釣りをやめて離れた後に消えても知らせない
        FishingBobberEntity bobber = FishingBobberTracker.bobber(client);
        if (bobber == null) {
            tracked = null;
            trackedPos = null;
            return;
        }
        tracked = hotspotNear(client, bobber);
        trackedPos = tracked == null ? null : tracked.getEntityPos();
    }

    // 浮きのそばにある Hotspot のアーマースタンド。なければ null
    private static ArmorStandEntity hotspotNear(MinecraftClient client, FishingBobberEntity bobber) {
        Box box = bobber.getBoundingBox().expand(HOOK_SEARCH_RADIUS);

        ArmorStandEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ArmorStandEntity stand : client.world.getEntitiesByClass(ArmorStandEntity.class, box, e -> true)) {
            if (stand.getCustomName() == null) continue;
            if (!HOTSPOT_NAME.equals(stand.getCustomName().getString().trim())) continue;

            double distance = stand.squaredDistanceTo(bobber);
            if (distance >= bestDistance) continue;

            bestDistance = distance;
            best = stand;
        }
        return best;
    }

    private static void alert(MinecraftClient client) {
        NotificationUtils.showTitle(client, Text.literal("§d§lHotspot §c§lis gone"), null,
                TITLE_FADE, TITLE_STAY, TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SOUND_VOLUME, SOUND_PITCH);
    }

    private static void reset() {
        tracked = null;
        trackedPos = null;
        goneMillis = 0;
    }
}
