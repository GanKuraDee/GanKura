package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Glacite Mineshaft のポータル誘導と再入場クールダウンを扱う。
 *
 *   ・ポータルが湧いたとき、ホログム文字列 ("[MVP+] 名前's Mineshaft Portal") へ Tracer を引く
 *   ・入場チャット ("[MVP+] 名前 entered Glacite Mineshafts!") から30秒のクールダウンを数え、
 *     HUD で残り時間を示し、終わったらタイトルで知らせる
 */
public final class MineshaftHandler {

    // ポータルへの Tracer 色。Glacite / Mineshaft の水色に合わせる
    public static final int PORTAL_TRACER_COLOR = 0xFF55FFFF;

    private static final String PORTAL_SPAWN_SELF = "WOW! You found a Glacite Mineshaft portal!";
    private static final String PORTAL_SPAWN_NEARBY = "MINESHAFT! A Mineshaft portal spawned nearby!";
    private static final String PORTAL_LABEL_KEYWORD = "Mineshaft Portal";

    // 例: "-----------------------------\n[MVP+] deeplygolem entered Glacite Mineshafts!\n-----------------------------"
    // 上下に区切り線の付いた複数行メッセージで届くため、行単位で照合する
    private static final Pattern ENTERED_MINESHAFT_PATTERN = Pattern.compile(
            "^\\s*(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{1,16})\\s+entered Glacite Mineshafts!\\s*$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    // "[MVP+] deeplygolem's Mineshaft Portal" から所有者名を取り出す
    private static final Pattern PORTAL_OWNER_PATTERN = Pattern.compile(
            "(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{1,16})'s?\\s+Mineshaft Portal",
            Pattern.CASE_INSENSITIVE
    );

    // Mineshaft 再入場クールダウン (30秒)
    private static final long COOLDOWN_DURATION_MS = 30_000L;
    // ポータルは約60秒で消えるため、少し余裕を持たせて探索する
    private static final long PORTAL_ALERT_WINDOW_MS = 65_000L;
    // ポータル探索の間隔 (tick)。動かないので2tickに1回で十分
    private static final int SCAN_INTERVAL_TICKS = 2;

    // 描画スレッドからも参照するポータルの位置
    private static volatile Vec3 portalTarget = null;
    private static long portalAlertUntil = 0L;
    private static int scanTickCounter = 0;

    // 30秒クールダウンは Mineshaft へのワープ・帰還をまたぐため、ワールド移動では消さない
    private static volatile long cooldownEndMillis = 0L;
    private static boolean readyTitlePending = false;

    private MineshaftHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MineshaftHandler::tick);
    }

    /** ワールド移動時にポータル追跡だけを消す。クールダウンはワープをまたぐので残す */
    public static void onWorldJoin() {
        portalTarget = null;
        portalAlertUntil = 0L;
        scanTickCounter = 0;
    }

    /** NetworkHandler からチャット受信時に呼ばれる */
    public static void handleMessage(String unformattedMsg, Minecraft client) {
        if (unformattedMsg == null || unformattedMsg.isEmpty()) return;

        if (ModConstants.containsIgnoreCase(unformattedMsg, PORTAL_SPAWN_SELF)
                || ModConstants.containsIgnoreCase(unformattedMsg, PORTAL_SPAWN_NEARBY)) {
            portalAlertUntil = System.currentTimeMillis() + PORTAL_ALERT_WINDOW_MS;
        }

        if (ENTERED_MINESHAFT_PATTERN.matcher(unformattedMsg).find()) {
            cooldownEndMillis = System.currentTimeMillis() + COOLDOWN_DURATION_MS;
            readyTitlePending = true;
            // 入場したので元のロビーのポータル誘導は終わらせる
            portalTarget = null;
            portalAlertUntil = 0L;
        }
    }

    /** Tracer 描画側が参照するポータル文字列の位置。対象が無ければ null */
    public static Vec3 portalTarget() {
        return portalTarget;
    }

    /** クールダウン残り秒数。クールダウン中でなければ null */
    public static Double cooldownSecondsLeft() {
        long end = cooldownEndMillis;
        if (end <= 0L) return null;
        long remainingMs = end - System.currentTimeMillis();
        if (remainingMs <= 0L) return null;
        return remainingMs / 1000.0;
    }

    private static void tick(Minecraft client) {
        checkCooldownReady(client);
        scanPortal(client);
    }

    private static void checkCooldownReady(Minecraft client) {
        if (!readyTitlePending || cooldownEndMillis <= 0L) return;
        if (System.currentTimeMillis() < cooldownEndMillis) return;
        // ちょうどワープ暗転中なら、プレイヤーが戻ってきた tick で出す
        if (client.player == null) return;

        readyTitlePending = false;
        cooldownEndMillis = 0L;

        if (!ModConfig.Mining.showMineshaftCooldownTitle) return;

        Component title = Component.literal("Mineshaft Ready").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        NotificationUtils.showTitle(client, title, null, 0, 50, 0);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private static void scanPortal(Minecraft client) {
        boolean inPortalArea = GameState.Server.isDwarvenMines() || GameState.Server.isGlaciteTunnels();
        if (!ModConfig.Mining.showMineshaftPortalTracer
                || !inPortalArea
                || client.level == null
                || client.player == null) {
            portalTarget = null;
            return;
        }

        if (++scanTickCounter < SCAN_INTERVAL_TICKS) return;
        scanTickCounter = 0;

        String selfName = client.player.getPlainTextName();
        boolean allowOtherNearby = System.currentTimeMillis() <= portalAlertUntil;
        Vec3 playerPos = client.player.position();

        ArmorStand ownPortal = null;
        double ownDistSqr = Double.MAX_VALUE;
        ArmorStand nearbyPortal = null;
        double nearbyDistSqr = Double.MAX_VALUE;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()) continue;
            Component customName = stand.getCustomName();
            if (customName == null) continue;

            String plain = customName.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (!ModConstants.containsIgnoreCase(plain, PORTAL_LABEL_KEYWORD)) continue;

            double distSqr = stand.position().distanceToSqr(playerPos);
            if (isOwnedBy(plain, selfName)) {
                if (distSqr < ownDistSqr) {
                    ownDistSqr = distSqr;
                    ownPortal = stand;
                }
            } else if (allowOtherNearby && distSqr < nearbyDistSqr) {
                nearbyDistSqr = distSqr;
                nearbyPortal = stand;
            }
        }

        ArmorStand chosen = ownPortal != null ? ownPortal : nearbyPortal;
        // マーカーなら bbHeight=0 で文字列の位置そのもの、通常 ArmorStand なら頭上の文字列付近を指す
        portalTarget = chosen != null ? chosen.position().add(0, chosen.getBbHeight(), 0) : null;
    }

    private static boolean isOwnedBy(String plainName, String selfName) {
        if (selfName == null || selfName.isEmpty()) return false;
        Matcher matcher = PORTAL_OWNER_PATTERN.matcher(plainName);
        return matcher.find() && matcher.group(1).equalsIgnoreCase(selfName);
    }
}
