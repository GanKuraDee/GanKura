package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetHandler {
    public static boolean hasScannedTabList = false;
    private static int widgetCheckTicker = 0; // ★追加: ウィジェット再確認用のタイマー

    private static final Pattern MANUAL_SUMMON = Pattern.compile("You summoned your (.+?)!");
    private static final Pattern AUTOPET_SUMMON = Pattern.compile("Autopet equipped your \\[Lvl \\d+\\] (.+?)! VIEW RULE");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            if (!"SKYBLOCK".equals(GameState.gametype)) return;

            // 通常の初回スキャン (未完了の場合)
            if (!hasScannedTabList) {
                scanTabListForPet(client);
            }
            // ★追加: もしウィジェットOFF判定になっていた場合、1秒(20tick)に1回だけ再確認を行う
            else if ("§cRequired Enable Pet Tab Widget!".equals(GameState.activePetName)) {
                if (widgetCheckTicker++ >= 20) {
                    widgetCheckTicker = 0;
                    scanTabListForPet(client);
                }
            }
        });
    }

    public static void reset() {
        hasScannedTabList = false;
        // ★変更: null ではなく、スキャン中の仮テキストを入れておく
        GameState.activePetName = "§8Scanning...";
    }

    // ★変更: StringではなくTextを受け取る
    // ★変更: StringではなくTextを受け取る
    public static void handleMessage(Text message) {
        if (!"SKYBLOCK".equals(GameState.gametype)) return;

        // ★新開発: 生データから「色付き文字列(§aなど)」を完璧に復元！
        String formattedMsg = toLegacyString(message);
        String unformattedMsg = formattedMsg.replaceAll("§[0-9a-fk-or]", "");

        // ★変更: ペット名が含まれるデスポーンメッセージにも完全対応
        // "You despawned your " で始まり "!" で終わるメッセージなら全てデスポーンとして処理します
        if (unformattedMsg.startsWith("You despawned your ") && unformattedMsg.endsWith("!")) {
            GameState.activePetName = null;
            return; // ← 変数の代入を削除し、即座にreturnするように修正
        }

        // 手動召喚の検知 (即時反映)
        Matcher m1 = MANUAL_SUMMON.matcher(unformattedMsg);
        if (m1.find()) {
            String petName = m1.group(1); // 記号(✦)も含む
            GameState.activePetName = extractPerfectColor(formattedMsg, petName);
            return; // ← 変数の代入を削除
        }

        // オートペットの検知 (即時反映)
        Matcher m2 = AUTOPET_SUMMON.matcher(unformattedMsg);
        if (m2.find()) {
            String petName = m2.group(1);
            GameState.activePetName = extractPerfectColor(formattedMsg, petName);
            return; // ← 変数の代入を削除
        }
    }

    private static void scanTabListForPet(MinecraftClient client) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) return;

        Collection<PlayerListEntry> entries = networkHandler.getPlayerList();

        // タブリストのデータがまだサーバーから送られてきていない(ロード中)場合は次回に回す
        if (entries.size() < 20) return;

        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            // タブリストの色を完全に復元する
            String line = toLegacyString(displayName);
            String unformatted = line.replaceAll("§[0-9a-fk-or]", "").trim();

            // 1. ペットを装備している場合
            if (unformatted.contains("[Lvl ")) {
                int startIdx = unformatted.indexOf("] ");
                if (startIdx != -1) {
                    String petName = unformatted.substring(startIdx + 2).trim();
                    GameState.activePetName = extractPerfectColor(line, petName);
                    hasScannedTabList = true;
                    return; // 見つけたら即終了
                }
            }

            // 2. ペットを装備していない場合 ("No pet selected" の文字列を検知)
            if (unformatted.contains("No pet selected")) {
                GameState.activePetName = null; // nullにしておくと、HUD描画側で「§7None」に変換されます
                hasScannedTabList = true;
                return;
            }
        }

        // リスト全体を探しても「[Lvl」も「No pet selected」も見つからず、
        // かつリスト自体はちゃんと読み込まれている場合
        // ＝ Skyblockメニュー設定で「Pet Tab Widget」がOFFになっていると判断する
        GameState.activePetName = "§cRequired Enable Pet Tab Widget!";
        hasScannedTabList = true;
    }

    // =========================================================================
    // ★新開発: 現代のMinecraftのTextからレガシーなカラーコード(§)を再構築する魔法
    // =========================================================================
    private static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                Integer rgb = color.getRgb();
                // 色コード(RGB)からマイクラ標準のカラーコード(§)を探し出して付与
                for (Formatting f : Formatting.values()) {
                    if (f.isColor() && f.getColorValue() != null && f.getColorValue().equals(rgb)) {
                        sb.append("§").append(f.getCode());
                        break;
                    }
                }
            }
            if (style.isObfuscated()) sb.append("§k");
            if (style.isBold()) sb.append("§l");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isItalic()) sb.append("§o");

            sb.append(part);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    // =========================================================================
    // 色コードごと指定したペット名だけを完璧にくり抜くメソッド
    // =========================================================================
    private static String extractPerfectColor(String formatted, String targetUnformatted) {
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");
        int targetIdx = unformatted.indexOf(targetUnformatted);

        if (targetIdx == -1) return "§7" + targetUnformatted;

        StringBuilder result = new StringBuilder();
        String currentColor = "§7";
        String startingColor = "§7";
        int unformattedCount = 0;

        for (int i = 0; i < formatted.length(); i++) {
            if (formatted.charAt(i) == '§' && i + 1 < formatted.length()) {
                char code = formatted.charAt(i + 1);
                currentColor = "§" + code;

                // 抽出範囲内の途中にある色コード(✦の色など)はそのまま維持
                if (unformattedCount > targetIdx && unformattedCount < targetIdx + targetUnformatted.length()) {
                    result.append("§").append(code);
                }
                i++;
            } else {
                // 抽出開始位置での色(メインのレアリティ色)を記憶
                if (unformattedCount == targetIdx) {
                    startingColor = currentColor;
                }

                if (unformattedCount >= targetIdx && unformattedCount < targetIdx + targetUnformatted.length()) {
                    result.append(formatted.charAt(i));
                }
                unformattedCount++;
            }
        }

        return startingColor + result.toString();
    }
}