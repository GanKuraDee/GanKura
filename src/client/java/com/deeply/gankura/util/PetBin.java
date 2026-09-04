package com.deeply.gankura.util;

import com.deeply.gankura.data.ItemPrices;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ペットの最安 BIN を引く。
 *
 * 値段表ではレベルと段まで込みで "LVL_1_LEGENDARY_BLAZE" のように並んでいて、
 * 並んでいるレベルは 1・100・200 の3つしかない。
 * 手元のペットで届く段のうち、一番上のものを使う
 */
public final class PetBin {

    // ペットの段が書かれている行の終わり。"LEGENDARY PET" のように書かれている
    private static final String PET_SUFFIX = " PET";
    // 名前に付いているレベル。"[Lvl 66] Blaze" のように書かれている
    private static final Pattern PET_LEVEL = Pattern.compile("\\[Lvl (\\d+)]");
    // 値段表に並んでいるレベル。高いものから、そのペットで届く段を探す
    private static final int[] PET_LEVELS = {200, 100, 1};

    /**
     * 引けた値段。
     *
     * @param listed 値段表で引いたレベル
     * @param level  手元のペットのレベル
     * @param price  その値段
     */
    public record Result(int listed, int level, double price) {
    }

    private PetBin() {
    }

    /**
     * そのペットの最安 BIN。引けなければ null。
     *
     * @param lines 名前の行から始まる説明。レベルと段をここから読む
     */
    public static Result of(SkyblockItemId.Pet pet, List<Component> lines) {
        String rarity = rarity(pet, lines);
        if (rarity.isEmpty()) return null;

        int level = level(lines);
        for (int listed : PET_LEVELS) {
            if (listed > level) continue;

            Double price = ItemPrices.lowestBin("LVL_" + listed + "_" + rarity + "_" + pet.type());
            if (price != null) return new Result(listed, level, price);
        }

        return null;
    }

    // そのペットのレベル。名前から読めなければ、一番下の 1 として扱う
    private static int level(List<Component> lines) {
        if (lines.isEmpty()) return 1;

        Matcher matcher = PET_LEVEL.matcher(lines.getFirst().getString());
        if (!matcher.find()) return 1;

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            // 桁が多すぎて読めなかった。レベルは分からないものとして扱う
            return 1;
        }
    }

    /**
     * そのペットの段。
     *
     * 説明の一番下に書かれているものを使う。
     * ここには段上げアイテムの分も含まれているので、中身の段より当てになる
     */
    private static String rarity(SkyblockItemId.Pet pet, List<Component> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String text = ChatFormatting.stripFormatting(lines.get(i).getString());
            if (text == null) continue;

            text = text.trim();
            if (!text.endsWith(PET_SUFFIX)) continue;

            String rarity = text.substring(0, text.length() - PET_SUFFIX.length()).trim();
            // "LEGENDARY" や "VERY SPECIAL" のように、大文字だけで書かれている
            if (!rarity.isEmpty() && rarity.equals(rarity.toUpperCase(Locale.ROOT))) {
                return rarity.replace(' ', '_');
            }
        }

        return pet.tier();
    }
}
