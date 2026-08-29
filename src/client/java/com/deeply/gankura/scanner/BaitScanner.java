package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.util.ScoreboardUtils;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 釣り餌とその残数を読む。
 *
 * Hypixel はホットバーの一番右(スロット8)を SkyBlock Menu に差し替えているが、
 * 釣り竿を持っている間だけその枠が餌そのものの見本に変わり、
 * 説明文に "Bait Remaining: <残数>" の行が出る。Quiver の矢と同じ仕組み。
 */
public class BaitScanner {

    // Hypixel が SkyBlock Menu / 餌の見本を置いているホットバーの位置
    private static final int MENU_SLOT = 8;

    private static final Pattern BAIT_REMAINING = Pattern.compile("Bait Remaining: (?<amount>[\\d,]+)");

    private static final int ALERT_TITLE_FADE = 0;
    private static final int ALERT_TITLE_STAY = 40;
    private static final float ALERT_SOUND_VOLUME = 1.0f;
    private static final float ALERT_SOUND_PITCH = 0.7f;

    // すでに知らせたか。減り続ける間に何度も出さないために持つ
    private static boolean alerted;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BaitScanner::scan);
    }

    private static void scan(Minecraft client) {
        if (client.player == null) return;

        if (!GameState.Server.isSkyblock()) {
            clear();
            return;
        }

        ItemStack stack = client.player.getInventory().getItem(MENU_SLOT);
        if (stack.isEmpty()) {
            clear();
            return;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            clear();
            return;
        }

        for (Component line : lore.lines()) {
            Matcher matcher = BAIT_REMAINING.matcher(
                    ScoreboardUtils.stripColor(ScoreboardUtils.toLegacyString(line)));
            if (!matcher.find()) continue;

            // 餌の名前はレアリティの色付きなので、色コードごと持っておく
            apply(client, ScoreboardUtils.toLegacyString(stack.getHoverName()).trim(), matcher.group("amount"));
            return;
        }

        clear();
    }

    private static void apply(Minecraft client, String bait, String amount) {
        if (ScoreboardUtils.stripColor(bait).isBlank()) {
            clear();
            return;
        }

        int count = parseAmount(amount);
        checkLow(client, bait, count);

        GameState.Player.fishingBait = bait;
        GameState.Player.fishingBaitCount = count;
    }

    private static void clear() {
        GameState.Player.fishingBait = null;
        GameState.Player.fishingBaitCount = 0;
        alerted = false;
    }

    /**
     * 残数が閾値を下回ったときに知らせる。
     *
     * 減るたびに出すとうるさいので、1度出したらそれで終わり。
     * 餌を付け直すか、余裕のある数に戻るとまた出せるようにする
     */
    private static void checkLow(Minecraft client, String bait, int count) {
        if (!ModConfig.INSTANCE.fishing.showBaitLowAlert) {
            alerted = false;
            return;
        }

        // 別の餌に付け直したなら、また知らせられるように戻す
        if (!bait.equals(GameState.Player.fishingBait)) alerted = false;

        // まだ余裕があるうちも戻しておく
        if (count > ModConfig.INSTANCE.fishing.baitLowThreshold) {
            alerted = false;
            return;
        }
        if (alerted) return;

        alerted = true;
        NotificationUtils.showTitle(client,
                Component.literal("§c§lBait Low §e§l" + count), null,
                ALERT_TITLE_FADE, ALERT_TITLE_STAY, ALERT_TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, ALERT_SOUND_VOLUME, ALERT_SOUND_PITCH);
    }

    private static int parseAmount(String text) {
        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
