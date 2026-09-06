package com.deeply.gankura.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class NotificationUtils {

    // 括弧の色。名前と同じ青系のまま、彩度も明度も一段深く沈めた鋼色。
    // 名前の彩度を落としてあるぶん、枠まで近い色にすると境目が消えてしまうので、
    // 明度差で「枠と名前」が分かれて見えるところに置いてある
    private static final int BRACKET_COLOR = 0x3E4C70;

    private static final String NAME = "GanKura";
    // 名前のグラデーション。青から水色へ流す。
    // 彩度は落としてあるが、チャットは暗い背景に重なるので明度は下げない。
    // 彩度と明度を一緒に落とすと、ただの灰色に見えてしまう
    private static final int NAME_START_COLOR = 0x8290BF;
    private static final int NAME_END_COLOR = 0xA7DAE4;

    public static MutableComponent getGanKuraPrefix() {
        MutableComponent prefix = Component.literal("[").withStyle(Style.EMPTY.withColor(BRACKET_COLOR));

        for (int i = 0; i < NAME.length(); i++) {
            // 色を持てるのは1文字ずつなので、両端がちょうど始点と終点の色になるように割る
            float ratio = NAME.length() == 1 ? 0f : (float) i / (NAME.length() - 1);
            int color = lerpColor(NAME_START_COLOR, NAME_END_COLOR, ratio);
            prefix.append(Component.literal(String.valueOf(NAME.charAt(i))).withStyle(Style.EMPTY.withColor(color)));
        }

        return prefix.append(Component.literal("] ").withStyle(Style.EMPTY.withColor(BRACKET_COLOR)));
    }

    /** 2色の間。赤緑青それぞれを別々に混ぜる */
    private static int lerpColor(int from, int to, float ratio) {
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, ratio);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, ratio);
        int b = lerp(from & 0xFF, to & 0xFF, ratio);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float ratio) {
        return Math.round(from + (to - from) * ratio);
    }

    // ドロップ通知でアイテム名を挟む1文字。中身は見えないので何でもよい
    private static final String SPARKLE = "a";

    /**
     * ドロップ通知の副題。
     *
     * アイテム名と通算数を、まとめてちらつく1文字で挟む
     */
    public static MutableComponent dropSubtitle(Component itemName, int count) {
        TextColor color = rarityColor(itemName);
        return Component.empty()
                .append(sparkle(color))
                .append(Component.literal(" "))
                .append(itemName)
                .append(Component.literal(" #" + count).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(sparkle(color));
    }

    private static Component sparkle(TextColor color) {
        return Component.literal(SPARKLE).withStyle(style -> style.withObfuscated(true).withColor(color));
    }

    /**
     * アイテム名に付いている色。Hypixel ではこれがレアリティの色になる。
     *
     * 名前は色ごとに切れた部品の集まりになっていることがあるので、
     * 先に見つかった色を採る。分からなければ null(色を付けない)
     */
    private static TextColor rarityColor(Component itemName) {
        TextColor color = itemName.getStyle().getColor();
        if (color != null) return color;

        for (Component sibling : itemName.getSiblings()) {
            TextColor found = rarityColor(sibling);
            if (found != null) return found;
        }

        // 色コードを文字列のまま持っている場合。最初の1つを見る
        return legacyColor(itemName.getString());
    }

    private static TextColor legacyColor(String text) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '\u00a7') continue;

            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting == null) continue;

            // 色でない装飾コード(太字など)には色が無いので、その場合は次を見る
            TextColor color = TextColor.fromLegacyFormat(formatting);
            if (color != null) return color;
        }
        return null;
    }

    public static void showTitle(Minecraft client, Component title, Component subtitle) {
        showTitle(client, title, subtitle, 5, 70, 20);
    }

    public static void showTitle(Minecraft client, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (client.player == null) return;
        // 26.2: Gui.setTimes/setSubtitle/setTitle 廃止 -> Hud に移動
        client.gui.hud.setTimes(fadeIn, stay, fadeOut);
        client.gui.hud.setSubtitle(subtitle != null ? subtitle : Component.empty());
        client.gui.hud.setTitle(title);
    }

    public static void sendSystemChat(Minecraft client, Component message) {
        if (client.player == null) return;
        MutableComponent fullMessage = getGanKuraPrefix().append(message);

        /*
         * 26.1.2 における修正ポイント:
         * displayClientMessage -> sendSystemMessage
         * もしくは client.player.chat.addMessage(...)
         *
         * 多くの最新バージョンでは sendSystemMessage(Component) が標準です。
         */
        client.player.sendSystemMessage(fullMessage);
    }

    public static void playSound(Minecraft client, SoundEvent sound, float volume, float pitch) {
        if (client.player != null) {
            client.player.playSound(sound, volume, pitch);
        }
    }
}