package com.deeply.gankura.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * エンチャントごとのレベルの上限。ロアに出る名前で引く。
 *
 * Hypixel はロアの見た目でティアの高さを区別していないため、この表と突き合わせる。
 * 表は SkyHanni-REPO の constants/Enchants.json から起こしたもので、
 * assets/gankura/enchantments.json に置いてある
 */
public final class EnchantData {

    /**
     * @param table    エンチャントテーブルで付けられる上限。テーブルで付かないものは 0
     * @param max      そのエンチャントに存在する最大レベル
     * @param ultimate Ultimate Enchantment か
     */
    public record Levels(int table, int max, boolean ultimate) {
    }

    // ロアに出る名前 -> 上限
    private static Map<String, Levels> levels = null;

    /**
     * ロアの中からエンチャントを見つけるための形。
     *
     * "Sharpness VII" のように、名前の後ろにローマ数字が続く。
     * 名前は長いものから並べて、"Blast Protection" が "Protection" に
     * 食われないようにしてある
     */
    private static Pattern pattern = null;

    private EnchantData() {
    }

    public static Pattern pattern() {
        load();
        return pattern;
    }

    /** 上限。表に無い名前なら null */
    public static Levels levels(String loreName) {
        load();
        return levels.get(loreName);
    }

    private static synchronized void load() {
        if (levels != null) return;

        levels = new HashMap<>();
        try (InputStream stream = EnchantData.class
                .getResourceAsStream("/assets/gankura/enchantments.json")) {
            if (stream != null) {
                JsonObject json = new Gson().fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject entries = json.getAsJsonObject("enchantments");

                for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    // ultimate は Ultimate Enchantment にだけ書いてある
                    levels.put(entry.getKey(), new Levels(
                            value.get("table").getAsInt(),
                            value.get("max").getAsInt(),
                            value.has("ultimate") && value.get("ultimate").getAsBoolean()));
                }
            }
        } catch (Exception ignored) {
            // 表が読めなければ色が付かないだけで、他の表示には影響しない
        }
        pattern = buildPattern();
    }

    private static Pattern buildPattern() {
        List<String> names = levels.keySet().stream()
                .sorted((left, right) -> right.length() - left.length())
                .toList();

        StringBuilder builder = new StringBuilder("(?<![A-Za-z])(");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) builder.append('|');
            builder.append(Pattern.quote(names.get(i)));
        }
        // 後ろに続くローマ数字までを1組として取る
        builder.append(") ([IVXLCDM]+)(?![A-Za-z])");

        return Pattern.compile(builder.toString());
    }

    /** ローマ数字を数に直す。読めない並びなら 0 */
    public static int romanToInt(String roman) {
        int total = 0;
        int previous = 0;

        for (int i = roman.length() - 1; i >= 0; i--) {
            int value = romanDigit(roman.charAt(i));
            if (value == 0) return 0;

            // 小さい数が前に来たら引く(IV = 4)
            total += value < previous ? -value : value;
            previous = Math.max(previous, value);
        }
        return total;
    }

    private static int romanDigit(char digit) {
        return switch (digit) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }
}
