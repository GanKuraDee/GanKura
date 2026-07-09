package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetHandler {
    public static boolean hasScannedTabList = false;
    private static int widgetCheckTicker = 0;

    // ★修正: [Lvl 100] 等のレベル表記をスキップ(除外)し、スキン(♦)と名前だけを抽出する正規表現
    private static final Pattern AUTOPET_SUMMON = Pattern.compile("Autopet equipped your (?:\\[Lvl \\d+\\] )?(.+?)! VIEW RULE", Pattern.CASE_INSENSITIVE);

    public static void register() {
    }

    public static void reset() {
        hasScannedTabList = false;
        GameState.Player.activePetName = "§8Scanning...";
    }

    // ★復活: チャットからのペット名・色・スキンの直接抽出ロジック
    public static void handleMessage(Component message) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        // 1. Componentオブジェクトから色情報(§)を保持した文字列を復元
        String formatted = toLegacyString(message);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        // 2. しまった時の判定
        if (ModConstants.containsIgnoreCase(unformatted, "You despawned your") || ModConstants.containsIgnoreCase(unformatted, "No pet selected")) {
            GameState.Player.activePetName = null;
            return;
        }

        // 3. オートペット装備時の判定 (レベルを除外して抽出)
        Matcher autoMatcher = AUTOPET_SUMMON.matcher(unformatted);
        if (autoMatcher.find()) {
            String petName = autoMatcher.group(1).trim();
            GameState.Player.activePetName = extractPerfectColor(formatted, petName);
            return;
        }
    }

    public static void processTabList(List<String> formattedLines, List<String> unformattedLines, Minecraft client) {
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
            if (ModConstants.containsIgnoreCase(unformatted, "[Lvl ")) {
                int startIdx = unformatted.indexOf("] ");
                if (startIdx != -1) {
                    String petName = unformatted.substring(startIdx + 2).trim();
                    GameState.Player.activePetName = extractPerfectColor(formatted, petName);
                    hasScannedTabList = true;
                    return;
                }
            }
            if (ModConstants.containsIgnoreCase(unformatted, "No pet selected")) {
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

    // Loadoutsメニューの3行4列目(Active Pet表示スロット)のアイテムのツールチップから
    // "[Lvl X] {ペット名}" の行を読み取り、ペット名だけをHUD表示に反映する
    // ツールチップは「アイテム名(1行目)」+「Lore」で構成されるため両方をチェックする
    // (SkyblockのペットアイテムはLoreではなくアイテム名自体が [Lvl X] 表記になっていることが多い)
    public static void processLoadoutsPetItem(ItemStack stack) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (stack.isEmpty()) return;

        if (tryExtractPetNameLine(stack.getHoverName())) return;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return;

        for (Component line : lore.lines()) {
            if (tryExtractPetNameLine(line)) return;
        }
    }

    private static boolean tryExtractPetNameLine(Component line) {
        String formatted = toLegacyString(line);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        if (!ModConstants.containsIgnoreCase(unformatted, "[Lvl ")) return false;
        int startIdx = unformatted.indexOf("] ");
        if (startIdx == -1) return false;

        String petName = unformatted.substring(startIdx + 2).trim();
        GameState.Player.activePetName = extractPerfectColor(formatted, petName);
        return true;
    }

    // Petsメニューでアイテムを左クリックした瞬間のスロットのツールチップから
    // "[Lvl X] {ペット名}" の行を読み取り、レベル表記まで含めてそのままHUD表示に反映する
    // (Loadoutsと違い名前だけに絞らず、行全体の色付き文字列をそのまま使うことでレベル部分の色も維持する)
    public static void processPetsMenuClick(ItemStack stack) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (stack.isEmpty()) return;

        if (tryExtractPetNameAndLevelLine(stack.getHoverName())) return;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return;

        for (Component line : lore.lines()) {
            if (tryExtractPetNameAndLevelLine(line)) return;
        }
    }

    private static boolean tryExtractPetNameAndLevelLine(Component line) {
        String formatted = toLegacyString(line);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        if (!ModConstants.containsIgnoreCase(unformatted, "[Lvl ")) return false;
        if (unformatted.indexOf("] ") == -1) return false;

        GameState.Player.activePetName = formatted.trim();
        return true;
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

    // ★復活: MinecraftのComponentオブジェクトからレガシーなカラーコード(§)を再構築するヘルパーメソッド
    private static String toLegacyString(Component text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                Integer rgb = color.getValue();
                for (ChatFormatting f : ChatFormatting.values()) {
                    if (f.isColor() && f.getColor() != null && f.getColor().equals(rgb)) {
                        sb.append("§").append(f.getChar());
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