package com.deeply.gankura.scanner;

import com.deeply.gankura.handler.BroodmotherHandler;
import com.deeply.gankura.handler.DragonHandler;
import com.deeply.gankura.handler.GolemHandler;
import com.deeply.gankura.handler.PetHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TabListScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scanTabList(client));
    }

    private static void scanTabList(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) return;

        Collection<PlayerListEntry> entries = networkHandler.getPlayerList();
        if (entries.isEmpty()) return;

        Scoreboard scoreboard = client.world.getScoreboard();
        List<String> unformattedLines = new ArrayList<>();
        List<String> formattedLines = new ArrayList<>();

        // 1回のループで全員分の「色付き文字列」と「色なし文字列」のリストを作成する
        for (PlayerListEntry entry : entries) {
            String profileName = entry.getProfile() != null ? entry.getProfile().name() : "";
            Text displayName = entry.getDisplayName();
            Text nameText = displayName != null ? displayName : Text.literal(profileName);

            Team team = scoreboard.getScoreHolderTeam(profileName);
            Text decoratedText = team != null ? team.decorateName(nameText) : nameText;

            String legacyStr = toLegacyString(decoratedText);
            formattedLines.add(legacyStr);
            unformattedLines.add(legacyStr.replaceAll("(?i)§[0-9A-FK-OR]", "").trim());
        }

        // ★各専門のハンドラー(担当者)にリストを渡して処理を任せる (単一責任の原則)
        GolemHandler.processTabList(unformattedLines, client);
        BroodmotherHandler.processTabList(unformattedLines);
        DragonHandler.processTabList(unformattedLines, client);
        PetHandler.processTabList(formattedLines, unformattedLines, client);
    }

    // MinecraftのTextからレガシーなカラーコード(§)を再構築するメソッド
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