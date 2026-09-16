package com.deeply.gankura.util;

import com.deeply.gankura.data.EnchantData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * エンチャント本に入っているエンチャントを読む。
 *
 * 本はどれも名前が "Enchanted Book" で絵柄も同じなので、
 * 並んでいる状態では中身が分からない。中身はロアにしか書かれていない
 */
public final class EnchantedBookText {

    private static final String BOOK_NAME = "Enchanted Book";

    // "Legion I" のように、名前の後ろにローマ数字が続く
    private static final Pattern NAMED_LEVEL = Pattern.compile("^(?<name>.+) (?<level>[IVXLCDM]+)$");

    // ロアに色記号が入っていなかったときの代わり。
    // Ultimate は太字の桃、それ以外は青で書かれている
    private static final String ULTIMATE_STYLE = "§d§l";
    private static final String NORMAL_STYLE = "§9";

    /** 本に入っているエンチャント。style はロアの行に付いていた色 */
    public record Book(String name, int level, String style) {
    }

    private EnchantedBookText() {
    }

    /**
     * その品がエンチャント本なら、入っているエンチャント。
     *
     * 2つ以上入っている本は1つに絞れないので null。
     * 元からロアに全部並んでいるので、そちらを読んでもらう
     */
    public static Book of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name == null || !name.trim().startsWith(BOOK_NAME)) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        Book found = null;
        for (Component line : lore.lines()) {
            Book book = read(line.getString());
            if (book == null) continue;
            if (found != null) return null;

            found = book;
        }
        return found;
    }

    /**
     * エンチャントの頭文字。"Legion" なら "L"、"Soul Eater" なら "SE"。
     *
     * 枠は16ドットしかなく、名前をそのまま出すと縮めても読めないので、
     * 語の頭だけを並べる
     */
    public static String initials(String name) {
        StringBuilder text = new StringBuilder();
        boolean wordStart = true;

        for (int i = 0; i < name.length(); i++) {
            char letter = name.charAt(i);

            // 区切りは空白とは限らない。"Turbo-Cacti" のような綴りもある
            if (!Character.isLetterOrDigit(letter)) {
                wordStart = true;
                continue;
            }
            if (wordStart) text.append(Character.toUpperCase(letter));
            wordStart = false;
        }
        return text.toString();
    }

    /** その行が示すエンチャント。エンチャントの行でなければ null */
    private static Book read(String text) {
        String plain = ChatFormatting.stripFormatting(text);
        if (plain == null) return null;

        Matcher matcher = NAMED_LEVEL.matcher(plain.trim());
        if (!matcher.matches()) return null;

        String name = matcher.group("name");
        // 表に無い名前は、後ろにローマ数字が付いているだけの別の行
        EnchantData.Levels levels = EnchantData.levels(name);
        if (levels == null) return null;

        int level = EnchantData.romanToInt(matcher.group("level"));
        return level > 0 ? new Book(name, level, styleOf(text, levels.ultimate())) : null;
    }

    /**
     * その行に付いている色。
     *
     * ロアの色をそのまま使えば、Ultimate かどうかが見た目で分かる。
     * 色を記号ではなく書式で寄越す繋ぎ先もあるので、
     * 記号が拾えなければ Ultimate かどうかから決め打つ
     */
    private static String styleOf(String text, boolean ultimate) {
        StringBuilder codes = new StringBuilder();

        int at = 0;
        while (at + 1 < text.length() && text.charAt(at) == '§') {
            codes.append(text, at, at + 2);
            at += 2;
        }
        if (!codes.isEmpty()) return codes.toString();

        return ultimate ? ULTIMATE_STYLE : NORMAL_STYLE;
    }
}
