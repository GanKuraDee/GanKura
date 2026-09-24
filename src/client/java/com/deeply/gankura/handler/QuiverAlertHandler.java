package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

// 矢筒の矢が設定した本数まで減ったことを、タイトルと音で知らせる。
// 知らせ方は Low Bait Alert / Low Soulflow Alert に合わせている。
// どの矢が減っているのかは、サブタイトルにレアリティの色のまま出す
public class QuiverAlertHandler {

    private static final int ALERT_TITLE_FADE = 0;
    private static final int ALERT_TITLE_STAY = 40;
    private static final float ALERT_SOUND_VOLUME = 1.0f;
    private static final float ALERT_SOUND_PITCH = 0.7f;

    // 一度知らせたか。しきい値より上に戻るか、別の矢に持ち替えるまでは知らせ直さない
    private static boolean alerted;
    // 直前に見た矢の種類。持ち替えを見分けるために覚えておく
    private static String lastArrow = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(QuiverAlertHandler::tick);
    }

    /**
     * 残りがしきい値以下になったときに知らせる。
     *
     * 減るたびに出すとうるさいので、1度出したらそれで終わり。
     * 補充してしきい値より上に戻るか、別の矢に持ち替えると、また出せるようにする。
     * ロビーを移る間などに一時的に読めなくなっても、戻ってきたときに知らせ直さない
     */
    private static void tick(Minecraft client) {
        if (client.player == null) return;

        String arrow = GameState.Player.quiverArrow;
        if (!GameState.Server.isSkyblock() || arrow == null) return;

        if (!arrow.equals(lastArrow)) alerted = false;
        lastArrow = arrow;

        int count = GameState.Player.quiverArrowCount;
        if (!ModConfig.Combat.enableQuiverAlert || count > ModConfig.Combat.quiverLowThreshold) {
            alerted = false;
            return;
        }
        if (alerted) return;

        alerted = true;
        NotificationUtils.showTitle(client,
                Component.literal("§c§lArrows Low §e§l" + String.format("%,d", count)), Component.literal(arrow),
                ALERT_TITLE_FADE, ALERT_TITLE_STAY, ALERT_TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, ALERT_SOUND_VOLUME, ALERT_SOUND_PITCH);
    }
}
