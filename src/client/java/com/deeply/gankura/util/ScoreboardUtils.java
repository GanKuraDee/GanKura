package com.deeply.gankura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Hypixelのサイドバーは、各行を Team の Prefix/Suffix にテキストとして載せる形で表示している。
// Team の集合をそのまま回すと順序が保証されないため、「ある行の1つ下の行」を参照したい機能
// (Magma Bossのプログレスバー等)のために、表示順(スコア降順 = 上から下)で行を組み立てて返す。
public class ScoreboardUtils {

    // 色コード(§x)を保持したままのサイドバー行を、上から順に返す
    public static List<String> getSidebarLines(MinecraftClient client) {
        if (client.world == null) return List.of();

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return List.of();

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        // スコアが大きい行ほど上に表示される
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());

        List<String> lines = new ArrayList<>(entries.size());
        for (ScoreboardEntry entry : entries) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            if (team == null) {
                lines.add("");
                continue;
            }
            // owner はHypixel側が行を識別するためのダミー文字列なので連結しない
            lines.add(toLegacyString(team.getPrefix()) + toLegacyString(team.getSuffix()));
        }
        return lines;
    }

    // サイドバーのタイトル(SkyBlockでは "SKYBLOCK")。各行と違いObjective側に載っているため別途取得する
    public static String getSidebarTitle(MinecraftClient client) {
        if (client.world == null) return "";
        ScoreboardObjective objective = client.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return "";
        return toLegacyString(objective.getDisplayName());
    }

    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    // Style で表現された装飾を § コードに戻す。
    // Hypixelは文字列内に直接 § を埋め込む場合もあるが、その分はそのまま残るので両方に対応できる
    public static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit((Style style, String part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getRgb();
                for (Formatting f : Formatting.values()) {
                    if (f.isColor() && f.getColorValue() != null && f.getColorValue().equals(rgb)) {
                        sb.append('§').append(f.getCode());
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
