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
        client.gui.setTimes(fadeIn, stay, fadeOut);
        client.gui.setSubtitle(subtitle != null ? subtitle : Component.empty());
        client.gui.setTitle(title);
    }

    public static void sendSystemChat(Minecraft client, Component message) {
        if (client == null || client.player == null || client.gui == null) return;
        if (!client.isSameThread()) {
            client.execute(() -> sendSystemChat(client, message));
            return;
        }

        // 接頭辞の括弧の色が本文へ引き継がれないよう、空の親の下に並べる。
        // また、message を再帰的に複製してから渡す。
        // ItemStack#getHoverName() 等の生 Component をそのままチャットへ渡すと、
        // チャットの Component を破壊的に書き換える他 Mod (Click to Translate Chat 等) に
        // 元の ItemStack 側の DataComponents.CUSTOM_NAME まで書き換えられてしまい、
        // クリックパケット生成時の HashedStack ハッシュ化で例外が起きるため
        MutableComponent fullMessage = Component.empty()
                .append(getGanKuraPrefix())
                .append(deepCopy(message));

        // LocalPlayer#sendSystemMessage は ChatListener#handleSystemMessage(..., true) を経由するため、
        // サーバー由来のチャット扱いになり Fabric の ClientReceiveMessageEvents (ALLOW_GAME / MODIFY_GAME 等) が発火する。
        // そのため他 Mod が GanKura の通知をサーバーチャットと誤認して文字列から作り直し、
        // 接頭辞の RGB 色が "<#3E4C70>[" のようなタグ文字列として露出することがある。
        // クライアント自身の通知は ChatComponent#addClientSystemMessage で直接チャット欄に追加し、受信イベントを通さない
        client.gui.getChat().addClientSystemMessage(fullMessage);
    }

    /**
     * Component と子要素を再帰的に複製する。
     * {@link Component#copy()} は子リスト(siblings)の浅いコピーしかしないため、自前で深く複製する
     */
    public static MutableComponent deepCopy(Component component) {
        MutableComponent copy = component.plainCopy().withStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) {
            copy.append(deepCopy(sibling));
        }
        return copy;
    }

    public static void playSound(Minecraft client, SoundEvent sound, float volume, float pitch) {
        if (client.player != null) {
            client.player.playSound(sound, volume, pitch);
        }
    }
}