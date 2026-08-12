package com.deeply.gankura.scanner;

import com.deeply.gankura.handler.BroodmotherHandler;
import com.deeply.gankura.handler.DragonHandler;
import com.deeply.gankura.handler.GolemHandler;
import com.deeply.gankura.handler.PetHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TabListScanner {

    private static List<String> previousUnformattedLines = null;
    private static List<String> previousFormattedLines = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scanTabList(client));
    }

    private static void scanTabList(Minecraft client) {
        if (client.level == null || client.player == null) return;
        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler == null) return;

        // 26.1.2 における修正ポイント1:
        // getListedPlayers() -> getListedPlayerInfos() への変更
        Collection<PlayerInfo> entries = networkHandler.getListedOnlinePlayers();
        if (entries == null || entries.isEmpty()) return;

        Scoreboard scoreboard = client.level.getScoreboard();
        List<String> unformattedLines = new ArrayList<>();
        List<String> formattedLines = new ArrayList<>();

        for (PlayerInfo entry : entries) {
            // 26.1.2 における修正ポイント2:
            // getName() -> name() (レコード形式のアクセサ)
            String profileName = entry.getProfile().name();
            Component displayName = entry.getTabListDisplayName();
            Component nameText = displayName != null ? displayName : Component.literal(profileName);

            PlayerTeam team = scoreboard.getPlayersTeam(profileName);
            Component decoratedText = team != null ? PlayerTeam.formatNameForTeam(team, nameText) : nameText;

            String legacyStr = toLegacyString(decoratedText);
            formattedLines.add(legacyStr);
            unformattedLines.add(legacyStr.replaceAll("(?i)§[0-9A-FK-OR]", "").trim());
        }

        if (previousUnformattedLines != null && previousFormattedLines != null &&
                previousUnformattedLines.equals(unformattedLines) && previousFormattedLines.equals(formattedLines)) {
            return;
        }

        previousUnformattedLines = new ArrayList<>(unformattedLines);
        previousFormattedLines = new ArrayList<>(formattedLines);

        // エリア/サーバーIDは他のハンドラーの判定条件になるため、必ず先に更新する
        LocationScanner.processTabList(unformattedLines);
        GolemHandler.processTabList(unformattedLines, client);
        BroodmotherHandler.processTabList(unformattedLines);
        DragonHandler.processTabList(unformattedLines, client);
        PetHandler.processTabList(formattedLines, unformattedLines, client);
    }

    private static String toLegacyString(Component text) {
        StringBuilder sb = new StringBuilder();
        text.visit((style, part) -> {
            TextColor color = style.getColor();
            if (color != null) {
                // ここを color.value() に修正 (カッコを追加)
                // もし解決できない場合は color.getRgb() を試してください
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
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}