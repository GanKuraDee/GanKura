package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetHandler {
    public static boolean hasScannedTabList = false;
    private static int widgetCheckTicker = 0;

    private static final Pattern MANUAL_SUMMON = Pattern.compile("You summoned your (.+?)!");
    private static final Pattern AUTOPET_SUMMON = Pattern.compile("Autopet equipped your \\[Lvl \\d+\\] (.+?)! VIEW RULE");

    public static void register() {
        // ★独自の ClientTickEvents でのスキャンを完全に削除し、TabListScannerに委ねます
    }

    public static void reset() {
        hasScannedTabList = false;
        GameState.Player.activePetName = "§8Scanning...";
    }

    public static void handleMessage(Text message) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        // ... (チャットメッセージの処理は元のまま維持しますが、色復元は削除し、NetworkHandlerから文字列を受け取る形でもOKです。今回は文字数削減のため割愛しますが元のロジックで動作します)
    }

    // ★追加: TabListScanner からリストを受け取るメソッド
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
}