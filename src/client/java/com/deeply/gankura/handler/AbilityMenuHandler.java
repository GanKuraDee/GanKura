package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Locale;

/**
 * 能力で開いたメニューが、持ち替えで閉じられるのを防ぐ。
 *
 * クライアントは選んでいる枠が変わると、次の tick でそれをサーバーに伝える。
 * Royal Pigeon のようにメニューを開く能力は、右クリックしてから画面が届くまでに
 * 往復ぶんの間があるので、その隙に持ち替えると、伝える順番がこうなる。
 * <pre>
 * client -> server : この品を使った
 * server -> client : メニューを開け
 * client -> server : 持ち替えた   ← メニューが開いた後に届く
 * </pre>
 * Hypixel はメニューを開いている最中の持ち替えを受け取ると、その場でメニューを閉じる。
 * そこで、メニューが開きそうな間と開いている間は持ち替えを伝えるのを見送り、
 * 閉じてから伝える。
 *
 * 見送るのは tick から出る分だけで、採掘や攻撃や次の右クリックはどれも
 * バニラ側で持ち替えを伝えてから動くので、実際に何かをした時点で遅れは解消される
 */
public final class AbilityMenuHandler {

    // 能力の説明にこの語があれば、メニューを開く品とみなす。
    // Royal Pigeon なら "Reach out to the King and open the Commissions menu."
    private static final String MENU_WORD = "menu";

    // 右クリックしてからメニューが届くまでに見込む間。往復ぶんの余裕を取ってある
    private static final long PENDING_WINDOW_MILLIS = 1500;

    private static long pendingUntil = 0;

    private AbilityMenuHandler() {
    }

    /** 右クリックした品がメニューを開くものなら、届くまでの間を覚える */
    public static void onUseItem(ItemStack stack) {
        if (!isEnabled()) return;
        if (!opensMenu(stack)) return;

        pendingUntil = System.currentTimeMillis() + PENDING_WINDOW_MILLIS;
    }

    /** 持ち替えをサーバーに伝えるのを、いま見送るかどうか */
    public static boolean shouldHoldSlotChange() {
        if (!isEnabled()) return false;

        // 開いている最中に伝えると、その場で閉じられる。閉じるまで待つ
        if (Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>) return true;

        return System.currentTimeMillis() < pendingUntil;
    }

    private static boolean isEnabled() {
        return ModConfig.INSTANCE.misc.keepAbilityMenuOpen && GameState.Server.isSkyblock();
    }

    private static boolean opensMenu(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            if (line.getString().toLowerCase(Locale.US).contains(MENU_WORD)) return true;
        }

        return false;
    }
}
