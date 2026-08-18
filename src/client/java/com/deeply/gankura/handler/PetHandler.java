package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

    // タブリストのペットウィジェットが無いと読めないので、出し方を案内する。
    // 改行で区切り、HUD側で行ごとに描く
    public static final String MISSING_WIDGET_MESSAGE =
            "§cMissing Pet Widget!\n§c(/widget -> Enable Pet Widget)";
    public static boolean hasScannedTabList = false;
    private static int widgetCheckTicker = 0;

    // [Lvl 100] 等のレベル表記があれば名前と合わせて抽出する(HUDにレベルまで表示するため)
    private static final Pattern AUTOPET_SUMMON = Pattern.compile("Autopet equipped your ((?:\\[Lvl \\d+\\] )?.+?)! VIEW RULE", Pattern.CASE_INSENSITIVE);

    private static final Pattern LEVEL_UP = Pattern.compile("Your (.+?) leveled up to level (\\d+)!", Pattern.CASE_INSENSITIVE);

    public static void register() {
    }

    public static void reset() {
        hasScannedTabList = false;
        GameState.Player.activePetName = "§8Scanning...";
    }

    // ★復活: チャットからのペット名・色・スキンの直接抽出ロジック
    public static void handleMessage(Component message) {
        if (!GameState.Server.isSkyblock()) return;

        // 1. Componentオブジェクトから色情報(§)を保持した文字列を復元
        String formatted = toLegacyString(message);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        // 2. しまった時の判定
        if (ModConstants.containsIgnoreCase(unformatted, "You despawned your") || ModConstants.containsIgnoreCase(unformatted, "No pet selected")) {
            GameState.Player.activePetName = null;
            return;
        }

        // 3. オートペット装備時の判定 (レベル表記も含めて抽出)
        Matcher autoMatcher = AUTOPET_SUMMON.matcher(unformatted);
        if (autoMatcher.find()) {
            String levelAndName = autoMatcher.group(1).trim();
            GameState.Player.activePetName = extractPerfectColor(formatted, levelAndName);
            return;
        }

        // 4. レベルアップ時の判定 (HUD表示中のペットと名前・色が一致する場合のみレベル数値を更新)
        Matcher levelUpMatcher = LEVEL_UP.matcher(unformatted);
        if (levelUpMatcher.find()) {
            String petName = levelUpMatcher.group(1).trim();
            String newLevel = levelUpMatcher.group(2).trim();
            updateLevelIfSamePet(formatted, petName, newLevel);
        }
    }

    // チャットのレベルアップメッセージに含まれるペット名(色込み)がHUD表示中のペット名と
    // 完全一致する場合に限り、"[Lvl X]" のレベル数値だけを新しい値に置き換える
    private static void updateLevelIfSamePet(String chatFormatted, String petName, String newLevel) {
        String current = GameState.Player.activePetName;
        if (current == null) return;

        String currentUnformatted = current.replaceAll("§[0-9a-fk-or]", "");
        int bracketEnd = currentUnformatted.indexOf("] ");
        if (bracketEnd == -1) return;

        String currentNamePart = currentUnformatted.substring(bracketEnd + 2).trim();
        String currentNameColored = extractPerfectColor(current, currentNamePart);
        String chatNameColored = extractPerfectColor(chatFormatted, petName);
        if (!currentNameColored.equals(chatNameColored)) return;

        GameState.Player.activePetName = current.replaceFirst("\\[Lvl \\d+\\]", "[Lvl " + newLevel + "]");
        GameState.Player.petLevelUpTime = System.currentTimeMillis();
    }

    public static void processTabList(List<String> formattedLines, List<String> unformattedLines, Minecraft client) {
        if (!GameState.Server.isSkyblock()) return;
        if (formattedLines.size() < 20) return; // ロード待ち

        // スキャン完了済みの場合は、エラー状態の時の再確認(1秒に1回)のみ行う
        if (hasScannedTabList) {
            if (MISSING_WIDGET_MESSAGE.equals(GameState.Player.activePetName)) {
                if (widgetCheckTicker++ < 20) return;
                widgetCheckTicker = 0;
            } else {
                return;
            }
        }

        for (int i = 0; i < unformattedLines.size(); i++) {
            String unformatted = unformattedLines.get(i);
            String formatted = formattedLines.get(i);

            // タブリストの [Lvl X] {ペット名} をレベル表記ごと抽出
            int bracketIdx = unformatted.indexOf("[Lvl ");
            if (bracketIdx != -1 && unformatted.indexOf("] ", bracketIdx) != -1) {
                String levelAndName = unformatted.substring(bracketIdx).trim();
                GameState.Player.activePetName = extractPerfectColor(formatted, levelAndName);
                hasScannedTabList = true;
                return;
            }
            if (ModConstants.containsIgnoreCase(unformatted, "No pet selected")) {
                GameState.Player.activePetName = null;
                hasScannedTabList = true;
                return;
            }
        }

        if (System.currentTimeMillis() - GameState.Server.lastWorldJoinTime > 5000) {
            GameState.Player.activePetName = MISSING_WIDGET_MESSAGE;
            hasScannedTabList = true;
        } else {
            GameState.Player.activePetName = "§8Scanning...";
        }
    }

    // Loadoutsメニューのロードアウト切り替えスロットをクリックした瞬間のツールチップから
    // "Pet: [Lvl X] {ペット名}" の行を探し、"Pet: " より後ろの部分だけをレベル表記ごとHUD表示に反映する
    // (ホバーだけでは切り替え先の情報を継続的に読み取れないため、クリックをトリガーとして読み取る)
    public static void processLoadoutsSlotClick(ItemStack stack) {
        if (!GameState.Server.isSkyblock()) return;
        if (stack.isEmpty()) return;

        List<Component> tooltip = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
        for (Component line : tooltip) {
            if (tryExtractPetLineFromTooltip(line)) return;
        }
    }

    // Petsメニューでアイテムを左クリックした瞬間のスロットのツールチップから
    // "[Lvl X] {ペット名}" の行を読み取り、レベル表記まで含めてそのままHUD表示に反映する
    // (Loadoutsと違い名前だけに絞らず、行全体の色付き文字列をそのまま使うことでレベル部分の色も維持する)
    public static void processPetsMenuClick(ItemStack stack) {
        if (!GameState.Server.isSkyblock()) return;
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

    // "Pet: [Lvl X] {ペット名}" 行から "Pet: " より後ろの部分だけを色付きのまま抽出する
    private static boolean tryExtractPetLineFromTooltip(Component line) {
        String formatted = toLegacyString(line);
        String unformatted = formatted.replaceAll("§[0-9a-fk-or]", "");

        int petIdx = unformatted.indexOf("Pet:");
        if (petIdx == -1) return false;

        String afterPet = unformatted.substring(petIdx + "Pet:".length()).trim();
        if (afterPet.isEmpty()) return false;

        GameState.Player.activePetName = extractPerfectColor(formatted, afterPet).trim();
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
                    TextColor legacy = TextColor.fromLegacyFormat(f);
                    if (legacy != null && legacy.getValue() == rgb) {
                        sb.append(f);
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