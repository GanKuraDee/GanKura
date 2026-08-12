package com.deeply.gankura.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Hypixelのサイドバーは、各行を Team の Prefix/Suffix にテキストとして載せる形で表示している。
// Team の集合をそのまま回すと順序が保証されないため、「ある行の1つ下の行」を参照したい機能
// (Magma Bossのプログレスバー等)のために、表示順(スコア降順 = 上から下)で行を組み立てて返す。
public class ScoreboardUtils {

    // 色コード(§x)を保持したままのサイドバー行を、上から順に返す
    public static List<String> getSidebarLines(Minecraft client) {
        if (client.level == null) return List.of();

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return List.of();

        List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
        // スコアが大きい行ほど上に表示される
        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

        List<String> lines = new ArrayList<>(entries.size());
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            if (team == null) {
                lines.add("");
                continue;
            }
            // owner はHypixel側が行を識別するためのダミー文字列なので連結しない
            lines.add(toLegacyString(team.getPlayerPrefix()) + toLegacyString(team.getPlayerSuffix()));
        }
        return lines;
    }

    // サイドバーのタイトル(SkyBlockでは "SKYBLOCK")。各行と違いObjective側に載っているため別途取得する
    public static String getSidebarTitle(Minecraft client) {
        if (client.level == null) return "";
        Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return "";
        return toLegacyString(objective.getDisplayName());
    }

    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    // Style で表現された装飾を § コードに戻す。
    // Hypixelは文字列内に直接 § を埋め込む場合もあるが、その分はそのまま残るので両方に対応できる
    private static String toLegacyString(Component text) {
        StringBuilder sb = new StringBuilder();
        text.visit((Style style, String part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getValue();
                for (ChatFormatting f : ChatFormatting.values()) {
                    // 26.2: ChatFormatting.isColor()/getColor()/getChar() 廃止。TextColor.fromLegacyFormat() 経由で取得
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
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}
