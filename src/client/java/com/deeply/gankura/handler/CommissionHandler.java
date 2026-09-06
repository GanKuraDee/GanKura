package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.SlotColorCache;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * 王から受けた依頼のうち、終わっているものの枠を塗る。
 *
 * 受け取りに行けるものが、1つずつ覗かなくても分かるようにする。
 * 判定は説明文の "COMPLETED" の行から取る。
 * まだ途中のものには代わりに "Progress" と進み具合が書かれている
 */
public final class CommissionHandler {

    // 依頼の一覧を出している画面
    private static final String MENU_TITLE = "Commissions";

    // 終わった依頼に書かれる印。行はこれだけで、他の言葉は続かない
    private static final String COMPLETED_MARK = "COMPLETED";

    // 濃さは設定で決まるので、ここでは色味だけを持つ
    private static final int COMPLETED_COLOR = 0x55FF55;

    private static final SlotColorCache COLORS = new SlotColorCache();

    private CommissionHandler() {
    }

    /** 依頼の一覧を出している画面か */
    public static boolean inMenu(String title) {
        return title.equals(MENU_TITLE);
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        return COLORS.get(slot, CommissionHandler::compute);
    }

    private static Integer compute(ItemStack stack) {
        if (!ModConfig.INSTANCE.mining.highlightCompletedCommissions) return null;
        if (!GameState.Server.isSkyblock() || stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        for (Component line : lore.lines()) {
            if (line.getString().trim().equals(COMPLETED_MARK)) return COMPLETED_COLOR;
        }
        return null;
    }
}
