package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.SlotColorCache;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Jacob のコンテストの一覧で、褒美を受け取ったかどうかで枠を塗る。
 *
 * 一覧は 20 ページあり、1つずつ覗いて回るには数が多い。
 * 受け取ったものには "Reward claimed!"、
 * まだのものには "Click to claim reward!" と説明の最後に書かれている。
 * どちらも書かれていないもの(開催中や参加していない回)は塗らない
 */
public final class FarmingContestHandler {

    // 一覧の題。"(1/20) Your Contests" のように、頭にページ数が付く
    private static final String MENU_TITLE = "Your Contests";

    private static final String CLAIMED_MARK = "Reward claimed!";
    private static final String UNCLAIMED_MARK = "Click to claim reward!";

    // 濃さは設定で決まるので、ここでは色味だけを持つ
    private static final int CLAIMED_COLOR = 0x55FF55;
    private static final int UNCLAIMED_COLOR = 0xFFFF55;

    private static final SlotColorCache COLORS = new SlotColorCache();

    private FarmingContestHandler() {
    }

    /** コンテストの一覧を出している画面か */
    public static boolean inMenu(String title) {
        return title.contains(MENU_TITLE);
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        if (!ModConfig.INSTANCE.farming.garden.highlightContestRewards) return null;
        if (!GameState.Server.isSkyblock()) return null;

        return COLORS.get(slot, FarmingContestHandler::compute);
    }

    private static Integer compute(ItemStack stack) {
        if (stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        for (Component line : lore.lines()) {
            String text = ChatFormatting.stripFormatting(line.getString());
            if (text == null) continue;

            text = text.trim();
            if (text.equals(CLAIMED_MARK)) return CLAIMED_COLOR;
            if (text.equals(UNCLAIMED_MARK)) return UNCLAIMED_COLOR;
        }
        return null;
    }
}
