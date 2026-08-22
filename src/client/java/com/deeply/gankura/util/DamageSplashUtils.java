package com.deeply.gankura.util;

import net.minecraft.entity.decoration.ArmorStandEntity;

import java.util.regex.Pattern;

// ダメージ表示(与えたダメージが数字で飛び出すあれ)の判定。
// Hypixel はこれを「名前付きのアーマースタンド」で出しているので、名前の形で見分ける
public final class DamageSplashUtils {

    // 例: 1,234 / ✧5231⚔ / 892✷。先頭の記号とダメージ種別の記号は付いたり付かなかったりする
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("[✧✯]?(\\d+[⚔+✧❤♞☄✷ﬗ✯]*)");

    private DamageSplashUtils() {
    }

    public static boolean isDamageSplash(ArmorStandEntity stand) {
        if (!stand.hasCustomName()) return false;
        if (!stand.isAlive()) return false;

        String name = ScoreboardUtils.stripColor(stand.getName().getString()).replace(",", "");
        return DAMAGE_PATTERN.matcher(name).matches();
    }
}
