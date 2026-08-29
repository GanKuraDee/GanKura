package com.deeply.gankura.data;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Hotspot が与える効果。
 *
 * 中心のアーマースタンドのすぐ下に、効果を書いたアーマースタンドが立っている。
 * 色の割り当ては SkyOcean に合わせている。
 */
public enum HotspotPerk {
    SEA_CREATURE("§3Sea Creature Chance", "\\+\\d+. Sea Creature Chance", 0xFF00AAAA),
    FISHING_SPEED("§bFishing Speed", "\\+\\d+. Fishing Speed", 0xFF55FFFF),
    DOUBLE_HOOK("§9Double Hook Chance", "\\+\\d+. Double Hook Chance", 0xFF5555FF),
    TREASURE("§6Treasure Chance", "\\+\\d+. Treasure Chance", 0xFFFFAA00),
    TROPHY_FISH("§6Trophy Chance", "\\+\\d+. Trophy Chance", 0xFFFFAA00),
    SHARD("§eShard Chance", "Chance of .+ Shard", 0xFFFFFF55),
    // どれにも当てはまらないとき。新しい効果が増えても円自体は出る
    UNKNOWN("§dUnknown", "", 0xFFFF55FF);

    private final String label;
    private final Pattern pattern;
    private final int argb;

    HotspotPerk(String label, String regex, int argb) {
        this.label = label;
        this.pattern = Pattern.compile(regex);
        this.argb = argb;
    }

    public int argb() {
        return argb;
    }

    /** チャットへ流すときの呼び名。色コードは付けない */
    public String plainLabel() {
        return label.replaceAll("§[0-9a-fk-or]", "");
    }

    @Override
    public String toString() {
        return label;
    }

    /** 効果の行から引く。当てはまらなければ UNKNOWN */
    public static HotspotPerk of(String text) {
        for (HotspotPerk perk : values()) {
            if (perk != UNKNOWN && perk.pattern.matcher(text).matches()) return perk;
        }
        return UNKNOWN;
    }

    /** 知らせる既定の顔ぶれ。狙って探すことが多いものだけ入れておく */
    public static List<HotspotPerk> defaults() {
        return List.of(SEA_CREATURE, SHARD);
    }
}
