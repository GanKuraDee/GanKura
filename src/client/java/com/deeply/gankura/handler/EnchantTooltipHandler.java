package com.deeply.gankura.handler;

import com.deeply.gankura.data.EnchantData;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * ツールチップのエンチャントの見た目を整える。
 *
 * Hypixel はロアの見た目でティアの高さを区別していないため、
 * {@link EnchantData} の上限表と照らし合わせて見つける。
 *   最大レベル … 虹色
 *   エンチャントテーブルの上限より上で、最大ではない … 金色
 *   ローマ数字 … 数字に置き換え
 */
public final class EnchantTooltipHandler {

    // 色が一周する時間(ミリ秒)
    private static final long CYCLE_MILLIS = 4000;
    // 文字ごとにずらす色の量。波が流れているように見せる
    private static final float CHAR_STEP = 0.03F;
    private static final float SATURATION = 0.75F;
    private static final float BRIGHTNESS = 1.0F;

    // 文字ごとの塗り分け
    private static final byte PLAIN = 0;
    private static final byte CHROMA = 1;
    private static final byte GOLD = 2;

    private EnchantTooltipHandler() {
    }

    // 行を「文字」と「その文字の書式」に開いたもの。書式を保ったまま組み直すために使う
    private record Line(String text, Style[] styles) {
    }

    // ローマ数字の置き換え。どこからどこまでを、何に差し替えるか
    private record Replacement(int end, String text) {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
            if (!config.enableEnchantTooltipTweaks) return;
            if (!config.enableMaxEnchantChroma && !config.enableBookEnchantGold
                    && !config.enableNumericEnchantTiers) {
                return;
            }
            if (!GameState.Server.isSkyblock()) return;

            // 先頭はアイテム名。Attribute のように、名前がエンチャントと
            // 同じ形になっているものを巻き込まないよう、ロアの行だけを見る
            for (int i = 1; i < lines.size(); i++) {
                Component rewritten = rewrite(lines.get(i));
                if (rewritten != null) lines.set(i, rewritten);
            }
        });
    }

    /**
     * 手を入れるエンチャントが入っている行なら、書き換えた行を返す。
     * 入っていなければ null を返して、元の行をそのまま使わせる
     */
    private static Component rewrite(Component component) {
        Line line = flatten(component);
        boolean numbers = ModConfig.INSTANCE.interfaceSettings.enableNumericEnchantTiers;

        byte[] marks = null;
        Map<Integer, Replacement> replacements = null;

        Matcher matcher = EnchantData.pattern().matcher(line.text());
        while (matcher.find()) {
            // エンチャントは行頭か "," の後から始まる。
            // "Arthropod Fortune IV" のように、別の語に続く同名は数えない
            if (!startsSegment(line.text(), matcher.start())) continue;

            int level = EnchantData.romanToInt(matcher.group(2));
            byte mark = markFor(matcher.group(1), level);

            if (mark != PLAIN) {
                if (marks == null) marks = new byte[line.text().length()];
                for (int i = matcher.start(); i < matcher.end(); i++) marks[i] = mark;
            }

            if (numbers && level > 0) {
                if (replacements == null) replacements = new HashMap<>();
                replacements.put(matcher.start(2), new Replacement(matcher.end(2), String.valueOf(level)));
            }
        }

        if (marks == null && replacements == null) return null;
        return rebuild(line, marks, replacements);
    }

    // 行頭、または区切りの "," の直後から始まっているか
    private static boolean startsSegment(String text, int start) {
        int index = start;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;

        return index == 0 || text.charAt(index - 1) == ',';
    }

    // その名前とレベルに付ける色。付けないときは PLAIN
    private static byte markFor(String name, int level) {
        EnchantData.Levels levels = EnchantData.levels(name);
        if (levels == null || level <= 0) return PLAIN;

        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;

        // Ultimate Enchantment はもともと目立つので、外せるようにしてある
        if (levels.ultimate() && config.ignoreUltimateEnchants) return PLAIN;

        // 表より高いレベルの品も最大として扱う
        if (level >= levels.max()) return config.enableMaxEnchantChroma ? CHROMA : PLAIN;

        // エンチャントテーブルでは届かないレベル。本を重ねないと手に入らない
        if (level > levels.table()) return config.enableBookEnchantGold ? GOLD : PLAIN;

        return PLAIN;
    }

    // 行を1文字ずつに開き、それぞれの書式を控えておく
    private static Line flatten(Component component) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();

        component.visit((FormattedText.StyledContentConsumer<Object>) (style, part) -> {
            text.append(part);
            for (int i = 0; i < part.length(); i++) styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);

        return new Line(text.toString(), styles.toArray(new Style[0]));
    }

    private static Component rebuild(Line line, byte[] marks, Map<Integer, Replacement> replacements) {
        MutableComponent result = Component.empty();
        StringBuilder plain = new StringBuilder();
        Style plainStyle = Style.EMPTY;

        int index = 0;
        while (index < line.text().length()) {
            Style style = line.styles()[index];
            byte mark = marks == null ? PLAIN : marks[index];

            Replacement replacement = replacements == null ? null : replacements.get(index);
            String piece = replacement == null
                    ? String.valueOf(line.text().charAt(index))
                    : replacement.text();

            if (mark == PLAIN) {
                // 書式が変わる境目でだけ切って、あとはまとめて出す
                if (!plain.isEmpty() && !plainStyle.equals(style)) appendPlain(result, plain, plainStyle);
                plainStyle = style;
                plain.append(piece);
            } else {
                appendPlain(result, plain, plainStyle);
                result.append(Component.literal(piece).withStyle(colored(style, mark, index)));
            }

            index = replacement == null ? index + 1 : replacement.end();
        }

        appendPlain(result, plain, plainStyle);
        return result;
    }

    private static Style colored(Style style, byte mark, int index) {
        return mark == GOLD ? style.withColor(ChatFormatting.GOLD)
                : style.withColor(TextColor.fromRgb(chromaColor(index)));
    }

    private static void appendPlain(MutableComponent result, StringBuilder plain, Style style) {
        if (plain.isEmpty()) return;

        result.append(Component.literal(plain.toString()).withStyle(style));
        plain.setLength(0);
    }

    // 時間と文字の位置で決まる色。少しずつずらすことで、文字列を色が流れていく
    private static int chromaColor(int index) {
        float time = (System.currentTimeMillis() % CYCLE_MILLIS) / (float) CYCLE_MILLIS;
        float hue = (time + index * CHAR_STEP) % 1.0F;

        return Mth.hsvToRgb(hue, SATURATION, BRIGHTNESS) & 0xFFFFFF;
    }
}
