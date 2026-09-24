package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Soulflow をタブリストから読む。
//
// タブリストの Profile ウィジェットに "Soulflow: 1,764" の行が出る。
// ウィジェットに Soulflow を出していないと行ごと無いので、そのときは HUD 側で出し方を案内する。
// Soulflow は使うたびに減っていくので、設定した量まで減ったらタイトルと音で知らせる
public class SoulflowScanner {

    // 知らせ方は Low Bait Alert に合わせている
    private static final int ALERT_TITLE_FADE = 0;
    private static final int ALERT_TITLE_STAY = 40;
    private static final float ALERT_SOUND_VOLUME = 1.0f;
    private static final float ALERT_SOUND_PITCH = 0.7f;

    // 一度知らせたか。しきい値より上に戻るまでは知らせ直さない
    private static boolean alerted;

    private static final Pattern SOULFLOW = Pattern.compile("^Soulflow:\\s*(?<value>[\\d,]+)");

    // タブリストが揃うまでの行数の目安。読み込み途中で「ウィジェットが無い」と誤判定しないための待ち
    private static final int MIN_LOADED_LINES = 20;

    public static void processTabList(List<String> unformattedLines) {
        if (unformattedLines.size() < MIN_LOADED_LINES) return;

        for (String line : unformattedLines) {
            Matcher matcher = SOULFLOW.matcher(line.trim());
            if (!matcher.find()) continue;

            try {
                int soulflow = Integer.parseInt(matcher.group("value").replace(",", ""));
                GameState.Player.soulflow = soulflow;
                GameState.Player.soulflowWidgetMissing = false;
                checkLow(soulflow);
                return;
            } catch (NumberFormatException ignored) {
                // 数でない書き方に変わった。読めなかったものとして扱う
            }
        }

        // タブリストは揃っているのに行が無い。ウィジェットで Soulflow を出していない
        GameState.Player.soulflow = -1;
        GameState.Player.soulflowWidgetMissing = true;
    }

    /**
     * しきい値まで減ったときに知らせる。
     *
     * 減るたびに出すとうるさいので、1度出したらそれで終わり。
     * 補充してしきい値より上に戻ると、また出せるようにする。
     * ロビーを移っても量が変わらなければ、知らせ直さない
     */
    private static void checkLow(int soulflow) {
        if (!ModConfig.Combat.showSoulflowLowAlert || soulflow > ModConfig.Combat.soulflowLowThreshold) {
            alerted = false;
            return;
        }
        if (alerted) return;

        alerted = true;
        Minecraft client = Minecraft.getInstance();
        NotificationUtils.showTitle(client,
                Component.literal("§c§lSoulflow Low §3§l" + String.format("%,d", soulflow)), null,
                ALERT_TITLE_FADE, ALERT_TITLE_STAY, ALERT_TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, ALERT_SOUND_VOLUME, ALERT_SOUND_PITCH);
    }
}
