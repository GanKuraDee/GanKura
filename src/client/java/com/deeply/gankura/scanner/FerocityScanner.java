package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Ferocity をタブリストから読む。
//
// タブリストの Stats ウィジェットに "Ferocity: ⫽123" の行が出る(記号は Hypixel のリソースパック用の文字)。
// ウィジェットに Ferocity を出していないと読めないので、その場合は未取得へ戻して HUD 側で案内を出す
public class FerocityScanner {

    // 記号は環境によって変わりうるので、数字の手前は「数字以外」としてまとめて読み飛ばす
    private static final Pattern FEROCITY = Pattern.compile("^Ferocity:\\s*\\D*(?<value>[\\d,.]+)");

    // タブリストが揃うまでの行数の目安。読み込み途中で「ウィジェットが無い」と誤判定しないための待ち
    private static final int MIN_LOADED_LINES = 20;

    public static void processTabList(List<String> unformattedLines) {
        if (unformattedLines.size() < MIN_LOADED_LINES) return;

        for (String line : unformattedLines) {
            Matcher matcher = FEROCITY.matcher(line.trim());
            if (!matcher.find()) continue;

            try {
                GameState.Player.ferocity = (int) Math.round(Double.parseDouble(matcher.group("value").replace(",", "")));
            } catch (NumberFormatException ignored) {
                GameState.Player.ferocity = -1;
            }
            return;
        }

        // ウィジェットから消えたら、古い値を出し続けずに未取得へ戻す
        GameState.Player.ferocity = -1;
    }
}
