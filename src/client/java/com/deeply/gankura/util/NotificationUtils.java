package com.deeply.gankura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NotificationUtils {

    // ★修正: [] をNetherite色、GanKuraをグラデーションにする
    public static MutableText getGanKuraPrefix() {
        // Netherite色 (濃い灰色/黒)
        // ※好みに合わせて変更してください (例: 0x313335 など)
        int netheriteColor = 0x443a3b;

        // 1. 左括弧 "["
        MutableText prefix = Text.literal("[").setStyle(Style.EMPTY.withColor(netheriteColor));

        // 2. "GanKura" のグラデーション生成
        String text = "GanKura";
        int startColor = 0xAAAAAA; // 灰色
        int endColor = 0xFFFFFF;   // 白

        int length = text.length();

        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;

        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);

            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            int color = (r << 16) | (g << 8) | b;

            prefix.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }

        // 3. 右括弧 "]"
        prefix.append(Text.literal("]").setStyle(Style.EMPTY.withColor(netheriteColor)));

        // 4. 末尾のスペース (色はデフォルト)
        prefix.append(Text.literal(" ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

        return prefix;
    }

    public static void showAwakeningAlert(MinecraftClient client) {
        if (client.player == null) return;
        client.inGameHud.setTitle(Text.literal("GOLEM STAGE 4").formatted(Formatting.RED, Formatting.BOLD));
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitleTicks(5, 70, 20);
    }

    public static void playAwakeningSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
    }

    public static void showSummonedAlert(MinecraftClient client) {
        if (client.player == null) return;
        client.inGameHud.setTitle(Text.literal("GOLEM STAGE 5").formatted(Formatting.DARK_RED, Formatting.BOLD));
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitleTicks(5, 70, 20);
    }

    public static void playSummonedSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }

    public static void showDropAlert(MinecraftClient client, Text itemText) {
        if (client.player == null) return;

        // メインタイトル: DROP! (赤色・太字)
        client.inGameHud.setTitle(Text.literal("DROP!").formatted(Formatting.RED, Formatting.BOLD));

        // サブタイトル: アイテム名
        // ★修正: タイトル表示用にコピーを作成し、ここで太字(BOLD)を適用する
        // これにより、元の itemText (チャット用) は細字のまま維持される
        MutableText titleText = itemText.copy().formatted(Formatting.BOLD);

        client.inGameHud.setSubtitle(titleText);
        client.inGameHud.setTitleTicks(5, 100, 20);
    }

    public static void sendDropChatMessage(MinecraftClient client, Text itemText) {
        if (client.player == null) return;

        MutableText message = getGanKuraPrefix();

        MutableText rareDrop = Text.literal("RARE DROP! ").formatted(Formatting.GOLD, Formatting.BOLD);
        message.append(rareDrop).append(itemText);

        client.player.sendMessage(message, false);
    }

    public static void playDropSound(MinecraftClient client) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
    }

    // ★追加・修正: ドラゴンスポーンのタイトル表示
    public static void showDragonSpawnAlert(MinecraftClient client, String dragonType) {
        if (client.player == null) return;

        // ドラゴンの種類によって色を変更
        // ★変更: Superior を YELLOW (黄色) に変更
        Formatting color = switch (dragonType) {
            case "Protector" -> Formatting.DARK_GRAY;          // 濃い灰色
            case "Old" -> Formatting.GRAY;                     // 灰色
            case "Unstable" -> Formatting.DARK_PURPLE;         // 濃い紫
            case "Young" -> Formatting.WHITE;                  // 白
            case "Strong" -> Formatting.RED;                   // 赤
            case "Wise" -> Formatting.AQUA;                    // 水色
            case "Superior" -> Formatting.YELLOW;              // 黄色 (大当たり)
            default -> Formatting.LIGHT_PURPLE;
        };

        // まずは色だけを適用したテキストを作成 (デフォルトは細字)
        MutableText title = Text.literal(dragonType.toUpperCase() + " DRAGON!").formatted(color);

        // ★変更: Superior の時だけ追加で太字(BOLD)にし、ウィザーの音を鳴らす
        if ("Superior".equals(dragonType)) {
            title.formatted(Formatting.BOLD);
            // ズゥゥン…というウィザー召喚音で大当たりの特別感を演出
            client.player.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
        // Superior 以外の時のサウンド処理は削除しました

        // 画面にタイトルを表示
        client.inGameHud.setTitle(title);
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitleTicks(5, 70, 20);
    }
}