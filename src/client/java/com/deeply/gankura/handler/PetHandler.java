package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetHandler {
    public static boolean hasScannedTabList = false;
    private static int widgetCheckTicker = 0;

    private static final Pattern MANUAL_SUMMON = Pattern.compile("You summoned your (.+?)!");

    // ★修正: [Lvl 100] 等のレベル表記をスキップ(除外)し、スキン(♦)と名前だけを抽出する正規表現
    private static final Pattern AUTOPET_SUMMON = Pattern.compile("Autopet equipped your (?:\\[Lvl \\d+\\] )?(.+?)! VIEW RULE");

    public static void register() {
    }

    public static void reset() {
        hasScannedTabList = false;
        GameState.Player.activePetName = "§8Scanning...";
    }

    // ★復活: チャットからのペット名・色・スキンの直接抽出ロジック
    public static void handleMessage(Text message) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        // 1. Textオブジェクトから色情報(§)を保持した文字列を復元
        String formatted = toLegacyString(message);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        // 2. しまった時の判定
        if (unformatted.contains("You despawned your") || unformatted.contains("No pet selected")) {
            GameState.Player.activePetName = null;
            return;
        }

        // 3. 手動召喚時の判定
        Matcher manualMatcher = MANUAL_SUMMON.matcher(unformatted);
        if (manualMatcher.find()) {
            String petName = manualMatcher.group(1).trim();
            GameState.Player.activePetName = extractPerfectColor(formatted, petName);
            return;
        }

        // 4. オートペット装備時の判定 (レベルを除外して抽出)
        Matcher autoMatcher = AUTOPET_SUMMON.matcher(unformatted);
        if (autoMatcher.find()) {
            String petName = autoMatcher.group(1).trim();
            GameState.Player.activePetName = extractPerfectColor(formatted, petName);
            return;
        }
    }

    public static void processTabList(List<String> formattedLines, List<String> unformattedLines, MinecraftClient client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (formattedLines.size() < 20) return; // ロード待ち

        // スキャン完了済みの場合は、エラー状態の時の再確認(1秒に1回)のみ行う
        if (hasScannedTabList) {
            if ("§cRequired Enable Pet Tab Widget!".equals(GameState.Player.activePetName)) {
                if (widgetCheckTicker++ < 20) return;
                widgetCheckTicker = 0;
            } else {
                return;
            }
        }

        for (int i = 0; i < unformattedLines.size(); i++) {
            String unformatted = unformattedLines.get(i);
            String formatted = formattedLines.get(i);

            // タブリストからも [Lvl X] を除外して抽出
            if (unformatted.contains("[Lvl ")) {
                int startIdx = unformatted.indexOf("] ");
                if (startIdx != -1) {
                    String petName = unformatted.substring(startIdx + 2).trim();
                    GameState.Player.activePetName = extractPerfectColor(formatted, petName);
                    hasScannedTabList = true;
                    return;
                }
            }
            if (unformatted.contains("No pet selected")) {
                GameState.Player.activePetName = null;
                hasScannedTabList = true;
                return;
            }
        }

        if (System.currentTimeMillis() - GameState.Server.lastWorldJoinTime > 5000) {
            GameState.Player.activePetName = "§cRequired Enable Pet Tab Widget!";
            hasScannedTabList = true;
        } else {
            GameState.Player.activePetName = "§8Scanning...";
        }
    }

    // 元々の完璧な色抽出ロジック
    private static String extractPerfectColor(String formatted, String targetUnformatted) {
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");
        int targetIdx = unformatted.indexOf(targetUnformatted);
        if (targetIdx == -1) return "§7" + targetUnformatted;

        StringBuilder result = new StringBuilder();
        String currentColor = "§7", startingColor = "§7";
        int unformattedCount = 0;

        for (int i = 0; i < formatted.length(); i++) {
            if (formatted.charAt(i) == '§' && i + 1 < formatted.length()) {
                char code = formatted.charAt(i + 1);
                currentColor = "§" + code;
                if (unformattedCount > targetIdx && unformattedCount < targetIdx + targetUnformatted.length()) {
                    result.append("§").append(code);
                }
                i++;
            } else {
                if (unformattedCount == targetIdx) startingColor = currentColor;
                if (unformattedCount >= targetIdx && unformattedCount < targetIdx + targetUnformatted.length()) {
                    result.append(formatted.charAt(i));
                }
                unformattedCount++;
            }
        }
        return startingColor + result.toString();
    }

    // ★復活: MinecraftのTextオブジェクトからレガシーなカラーコード(§)を再構築するヘルパーメソッド
    private static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                Integer rgb = color.getRgb();
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
}