package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

// 矢の残りが少なくなったことを、タイトルと音で知らせる。
// しきい値を下から跨いだ瞬間だけ出すので、残りが少ないまま撃ち続けても鳴り続けない
public class QuiverAlertHandler {

    private static final int LOW_THRESHOLD = 50;
    private static final int CRITICAL_THRESHOLD = 10;

    // 直前の残数と矢の種類。跨いだ瞬間を見るために覚えておく
    private static int lastCount = -1;
    private static String lastArrow = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(QuiverAlertHandler::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) return;

        String arrow = GameState.Player.quiverArrow;
        if (!GameState.Server.isSkyblock() || arrow == null) {
            reset();
            return;
        }

        int count = GameState.Player.quiverArrowCount;
        int previous = lastCount;
        boolean sameArrow = arrow.equals(lastArrow);
        lastCount = count;
        lastArrow = arrow;

        if (!ModConfig.INSTANCE.misc.enableQuiverAlert) return;
        // 読み始めた直後と、矢を持ち替えた直後は比べる相手がいない。
        // 種類を変えると残数が別の矢のものへ飛ぶので、それを「減った」と数えないようにする
        if (previous < 0 || !sameArrow) return;
        // 補充したときは何も出さない
        if (count >= previous) return;

        if (crossed(previous, count, CRITICAL_THRESHOLD)) {
            alert(client, "ARROWS ALMOST OUT", Formatting.RED, count, SoundEvents.BLOCK_ANVIL_LAND, 1.4f);
        } else if (crossed(previous, count, LOW_THRESHOLD)) {
            alert(client, "LOW ON ARROWS", Formatting.YELLOW, count, SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0f);
        }
    }

    public static void reset() {
        lastCount = -1;
        lastArrow = null;
    }

    // このtickでしきい値を上から下へ跨いだか
    private static boolean crossed(int previous, int count, int threshold) {
        return previous > threshold && count <= threshold;
    }

    private static void alert(MinecraftClient client, String title, Formatting color, int count, SoundEvent sound, float pitch) {
        MutableText titleText = Text.literal(title).formatted(color, Formatting.BOLD);
        Text subtitle = Text.literal(String.format("%,d left", count));

        NotificationUtils.showTitle(client, titleText, subtitle);
        NotificationUtils.playSound(client, sound, 1.0f, pitch);
    }
}
