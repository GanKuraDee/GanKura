package com.deeply.gankura.util;

import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.regex.Pattern;

// ダメージ表示(与えたダメージが数字で飛び出すあれ)の判定。
// Hypixel はこれを「名前付きのアーマースタンド」で出しているので、名前の形で見分ける
public final class DamageSplashUtils {

    // 例: 1,234 / ✧5231⚔ / 892✷。先頭の記号とダメージ種別の記号は付いたり付かなかったりする
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("[✧✯]?(\\d+[⚔+✧❤♞☄✷ﬗ✯]*)");

    // 湧いてから時間が経ったものはダメージ表示ではない。
    // ダメージ表示はすぐ消えるので、長く残っているなら別物(モブの名前など)と判断する
    private static final int MAX_AGE_TICKS = 300;

    private DamageSplashUtils() {
    }

    public static boolean isDamageSplash(ArmorStand stand) {
        if (stand.tickCount > MAX_AGE_TICKS) return false;
        if (!stand.hasCustomName()) return false;
        if (!stand.isAlive()) return false;

        String name = ScoreboardUtils.stripColor(stand.getName().getString()).replace(",", "");
        return DAMAGE_PATTERN.matcher(name).matches();
    }
}
