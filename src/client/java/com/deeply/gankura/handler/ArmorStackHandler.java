package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorStackHandler {

    private static final Pattern CRIMSON_PATTERN = Pattern.compile("(\\d+)[ ]?ᝐ");
    private static final Pattern TERROR_PATTERN = Pattern.compile("(\\d+)[ ]?⁑");
    private static final Pattern HOLLOW_PATTERN = Pattern.compile("(\\d+)[ ]?⚶");
    private static final Pattern FERVOR_PATTERN = Pattern.compile("(\\d+)[ ]?҉");
    private static final Pattern AURORA_PATTERN = Pattern.compile("(\\d+)[ ]?Ѫ");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (System.currentTimeMillis() - GameState.Player.lastArmorStackUpdateTime > 2000) {
                GameState.Player.crimsonStack = 0; GameState.Player.isCrimsonBold = false;
                GameState.Player.terrorStack = 0;  GameState.Player.isTerrorBold = false;
                GameState.Player.hollowStack = 0;  GameState.Player.isHollowBold = false;
                GameState.Player.fervorStack = 0;  GameState.Player.isFervorBold = false;
                GameState.Player.auroraStack = 0;  GameState.Player.isAuroraBold = false;
            }
        });
    }

    // ★変更: 引数を Component message に変更し、太字検知ロジックを追加
    public static void handleActionBar(Component message) {
        String formattedMsg = toLegacyString(message);
        String unformattedMsg = formattedMsg.replaceAll("§[0-9a-fk-or]", "");

        boolean hasArmorInfo = false;
        if (unformattedMsg.contains("ᝐ") || unformattedMsg.contains("⁑") || unformattedMsg.contains("⚶") || unformattedMsg.contains("҉") || unformattedMsg.contains("Ѫ")) {
            hasArmorInfo = true;
        }

        if (hasArmorInfo) {
            GameState.Player.crimsonStack = 0; GameState.Player.isCrimsonBold = false;
            GameState.Player.terrorStack = 0;  GameState.Player.isTerrorBold = false;
            GameState.Player.hollowStack = 0;  GameState.Player.isHollowBold = false;
            GameState.Player.fervorStack = 0;  GameState.Player.isFervorBold = false;
            GameState.Player.auroraStack = 0;  GameState.Player.isAuroraBold = false;

            Matcher m1 = CRIMSON_PATTERN.matcher(unformattedMsg);
            if (m1.find()) {
                GameState.Player.crimsonStack = Integer.parseInt(m1.group(1));
                GameState.Player.isCrimsonBold = isBoldAt(formattedMsg, m1.group(0));
            }

            Matcher m2 = TERROR_PATTERN.matcher(unformattedMsg);
            if (m2.find()) {
                GameState.Player.terrorStack = Integer.parseInt(m2.group(1));
                GameState.Player.isTerrorBold = isBoldAt(formattedMsg, m2.group(0));
            }

            Matcher m3 = HOLLOW_PATTERN.matcher(unformattedMsg);
            if (m3.find()) {
                GameState.Player.hollowStack = Integer.parseInt(m3.group(1));
                GameState.Player.isHollowBold = isBoldAt(formattedMsg, m3.group(0));
            }

            Matcher m4 = FERVOR_PATTERN.matcher(unformattedMsg);
            if (m4.find()) {
                GameState.Player.fervorStack = Integer.parseInt(m4.group(1));
                GameState.Player.isFervorBold = isBoldAt(formattedMsg, m4.group(0));
            }

            Matcher m5 = AURORA_PATTERN.matcher(unformattedMsg);
            if (m5.find()) {
                GameState.Player.auroraStack = Integer.parseInt(m5.group(1));
                GameState.Player.isAuroraBold = isBoldAt(formattedMsg, m5.group(0));
            }

            GameState.Player.lastArmorStackUpdateTime = System.currentTimeMillis();
        }
    }

    // Componentオブジェクトから色コード(§)を復元するメソッド
    private static String toLegacyString(Component text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                // Yarnの .getRgb() は .getValue() に
                Integer rgb = color.getValue();
                for (ChatFormatting f : ChatFormatting.values()) {
                    // Yarnの .getColorValue() は .getColor()、 .getCode() は .getChar() に
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

    // 指定した文字列の直前が太字(§l)かどうかを判定するメソッド
    private static boolean isBoldAt(String formattedMsg, String targetUnformatted) {
        String unformatted = formattedMsg.replaceAll("§[0-9a-fk-or]", "");
        int targetIdx = unformatted.indexOf(targetUnformatted);
        if (targetIdx == -1) return false;

        boolean isBold = false;
        int unformattedCount = 0;

        for (int i = 0; i < formattedMsg.length(); i++) {
            if (formattedMsg.charAt(i) == '§' && i + 1 < formattedMsg.length()) {
                char code = formattedMsg.charAt(i + 1);
                if (code == 'l' || code == 'L') isBold = true;
                    // 色コード(0-9, a-f)やリセット(r)が来ると太字は解除される仕様
                else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'r' || code == 'R') {
                    isBold = false;
                }
                i++;
            } else {
                if (unformattedCount == targetIdx) {
                    return isBold;
                }
                unformattedCount++;
            }
        }
        return false;
    }
}