package com.deeply.gankura.util;

import com.deeply.gankura.data.EnchantData;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * エンチャント本の名前から Bazaar の ID を組み立てる。
 *
 * Bazaar の棚や注文の一覧に並ぶ本には中身が書かれておらず、
 * Corpse の戦利品もチャットの文字しか届かないので、名前だけが手掛かりになる。
 * Hypixel が配っている品の一覧にもエンチャント本は載っていないため、こちらで組み立てる
 */
public final class EnchantedBookId {

    // "ENCHANTMENT_ICE_COLD_1" の形で売られている
    private static final String ID_PREFIX = "ENCHANTMENT_";
    // Ultimate Enchantment は、名前に出ていなくても ID には必ず付く
    private static final String ULTIMATE_PREFIX = "ULTIMATE_";

    // Corpse の戦利品は "Enchanted Book (Ice Cold I)" の形で届く
    private static final Pattern WRAPPED = Pattern.compile("^Enchanted Book \\((?<inner>[^()]+)\\)$");
    // 名前の後ろにレベルがローマ数字で付く
    private static final Pattern NAMED_LEVEL = Pattern.compile("^(?<name>.+) (?<level>[IVXLCDM]+)$");

    // 2つ以上入った本は1つの品として売られていないので、この区切りが入っていれば諦める
    private static final String MULTIPLE_MARK = ",";

    /**
     * 名前と ID が食い違うもの。
     *
     * 見た目の名前をそのまま大文字にすれば大抵は合うが、
     * この3つだけは Hypixel が別の綴りで登録している
     */
    private static final Map<String, String> EXCEPTIONS = Map.of(
            "TURBO_CACTI", "TURBO_CACTUS",
            "TURBO_COCOA", "TURBO_COCO",
            "ULTIMATE_DUPLEX", "ULTIMATE_REITERATE");

    private EnchantedBookId() {
    }

    /**
     * エンチャント本の ID。本でなければ null。
     *
     * "Ice Cold I" でも "Enchanted Book (Ice Cold I)" でも引ける。
     * エンチャントの表に無い名前は本として扱わないので、
     * たまたま後ろにローマ数字の付いた別の品を拾うことはない
     */
    public static String of(String displayName) {
        if (displayName == null) return null;

        String text = displayName.trim();
        Matcher wrapped = WRAPPED.matcher(text);
        if (wrapped.matches()) text = wrapped.group("inner").trim();
        if (text.contains(MULTIPLE_MARK)) return null;

        Matcher named = NAMED_LEVEL.matcher(text);
        if (!named.matches()) return null;

        String name = named.group("name");
        EnchantData.Levels levels = EnchantData.levels(name);
        if (levels == null) return null;

        int level = EnchantData.romanToInt(named.group("level"));
        if (level <= 0) return null;

        return ID_PREFIX + key(name, levels.ultimate()) + "_" + level;
    }

    /** 見た目の名前を ID の綴りに直す。飾りの記号は下線に潰す */
    private static String key(String name, boolean ultimate) {
        String key = name.replace("'", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);

        if (ultimate && !key.startsWith(ULTIMATE_PREFIX)) key = ULTIMATE_PREFIX + key;

        return EXCEPTIONS.getOrDefault(key, key);
    }
}
