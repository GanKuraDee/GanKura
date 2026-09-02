package com.deeply.gankura.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Bestiary の画面を開いているか。
 *
 * 画面の題は "Bestiary ➜ Crimson Isle" のこともあれば
 * "Fishing ➜ Lava" のように分類名だけのこともあるので、題では見分けられない。
 * どの階層にも必ず置かれている2つの項目で判断する
 */
public final class BestiaryMenu {

    // Bestiary のどの画面にも並んでいる項目
    private static final String SEARCH_ITEM = "Search Bestiary";
    private static final String MILESTONE_ITEM = "Bestiary Milestone";

    // 同じ中身を毎フレーム調べ直さないための控え。
    // 中身が入れ替わると番号が変わるので、変わった時だけ調べ直す
    private static AbstractContainerMenu cachedMenu;
    private static int cachedState;
    private static boolean cachedResult;

    private BestiaryMenu() {
    }

    public static boolean isOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        AbstractContainerMenu menu = client.player.containerMenu;
        int state = menu.getStateId();

        if (menu == cachedMenu && state == cachedState) return cachedResult;

        cachedMenu = menu;
        cachedState = state;
        cachedResult = hasBestiaryItems(menu);
        return cachedResult;
    }

    private static boolean hasBestiaryItems(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString();
            if (name.equals(SEARCH_ITEM) || name.startsWith(MILESTONE_ITEM)) return true;
        }
        return false;
    }
}
