package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.DamageSplashUtils;
import com.deeply.gankura.util.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.FishingHook;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.regex.Pattern;

/**
 * 投げている釣り竿の浮きの状態を追う。ここから2つの表示が作られる。
 *
 *   Cast Timer          ... 投げてからの経過秒。浮きの上に出す
 *   Bite Countdown HUD  ... かかるまでの残り時間。Hypixel が浮きのそばに出している数字を読む
 *
 * 浮き自体がエンティティなので、それが消えた時点で投げ直し・釣り上げのどちらでも計測が終わる。
 * チャットを読む必要がないぶん、Sea Creature の湧き方が変わっても壊れない。
 */
public class FishingBobberTracker {

    // 浮きのそばのカウントダウンを探す半径(ブロック)
    private static final double TIMER_SEARCH_RADIUS = 5.0;
    // 魚が来た合図。Hypixel はこの名前のアーマースタンドを浮きの上に出す
    private static final String FISH_ARRIVED_NAME = "!!!";
    // 残り時間。"2.0" のような形。表示名は色コードが剥がれた形で届くので、素の文字で見る
    private static final Pattern COUNTDOWN_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    // 計測を始めた時刻。投げていない間は 0
    private static long startMillis = 0;

    // Hypixel が浮きの上に出しているカウントダウンの秒数。出ていなければ null
    private static String hypixelCountdown = null;
    // 魚が来た合図(!!!)が出ているか
    private static boolean fishArrived = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FishingBobberTracker::tick);
    }

    /** 投げている浮き。出していなければ null */
    public static FishingHook bobber(Minecraft client) {
        if (client.player == null || client.level == null) return null;

        FishingHook hook = client.player.fishing;
        if (hook != null && !hook.isRemoved()) return hook;

        // 浮きは出ているのに Player 側の紐づけが遅れることがある。
        // (手持ちの釣り竿の見た目も投げていない側のままになる)
        // その間もカウントダウンを出せるよう、ワールド側からも自分の浮きを探す
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof FishingHook candidate) || candidate.isRemoved()) continue;
            if (candidate.getPlayerOwner() == client.player) return candidate;
        }
        return null;
    }

    /** Hypixel が浮きの上に出しているカウントダウンの秒数。出ていなければ null */
    public static String hypixelCountdown() {
        return hypixelCountdown;
    }

    /** 魚が来た合図(!!!)が出ているか */
    public static boolean fishArrived() {
        return fishArrived;
    }

    /** 浮きが浮いてからの経過秒。まだ数え始めていなければ -1 */
    public static double elapsedSeconds() {
        if (startMillis == 0) return -1;
        return (System.currentTimeMillis() - startMillis) / 1000.0;
    }

    private static void tick(Minecraft client) {
        // どちらの表示も切っているなら、浮きを探す必要が無い
        if (!ModConfig.INSTANCE.fishing.showCastTimer && !ModConfig.INSTANCE.fishing.showBiteCountdownHud) {
            startMillis = 0;
            hypixelCountdown = null;
            fishArrived = false;
            return;
        }

        FishingHook hook = bobber(client);
        if (hook == null || !GameState.Server.isSkyblock()) {
            startMillis = 0;
            hypixelCountdown = null;
            fishArrived = false;
            return;
        }

        readHypixelTimer(client, hook);

        if (startMillis != 0) return;

        // 着水を待つ設定のときは、水面(溶岩面)に触れてから数え始める。
        // 待たない設定なら、投げた瞬間 = 浮きが出た瞬間から数える
        if (!ModConfig.INSTANCE.fishing.castTimerOnLiquidTouch || hook.isInWater() || hook.isInLava()) {
            startMillis = System.currentTimeMillis();
        }
    }

    /**
     * 浮きのそばに出ている Hypixel のカウントダウンを読む。
     *
     * "2.0" のような名前のアーマースタンドが残り時間、"!!!" が魚の到着。
     * 同じ「数字だけの名前」になるダメージ表示は、Hide Damage Splash と同じ判定で弾く
     */
    private static void readHypixelTimer(Minecraft client, FishingHook hook) {
        hypixelCountdown = null;
        fishArrived = false;
        if (client.level == null) return;

        AABB box = hook.getBoundingBox().inflate(TIMER_SEARCH_RADIUS);
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, box, stand -> stand.hasCustomName())) {
            // 与えたダメージの数字も同じ「数字だけの名前」なので、先に弾く
            if (DamageSplashUtils.isDamageSplash(stand)) continue;

            String name = ScoreboardUtils.stripColor(stand.getName().getString()).trim();

            if (FISH_ARRIVED_NAME.equals(name)) {
                fishArrived = true;
                return;
            }

            if (COUNTDOWN_PATTERN.matcher(name).matches()) hypixelCountdown = name;
        }
    }
}
