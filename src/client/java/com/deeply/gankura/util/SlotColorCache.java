package com.deeply.gankura.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 枠に塗る色を控えておく入れ物。
 *
 * 枠は毎フレーム描き直されるので、説明文を読み直すのは中身が変わったときだけにする
 */
public final class SlotColorCache {

    // 塗らないことも答えのうちなので、それ用の目印を控えておく
    private static final int NO_COLOR = 0;

    private final Map<Integer, Integer> colors = new HashMap<>();
    private AbstractContainerMenu menu;
    private int state;

    /** その枠に塗る色。塗らないときは null */
    public Integer get(Slot slot, Function<ItemStack, Integer> compute) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return null;

        AbstractContainerMenu open = client.player.containerMenu;
        int openState = open.getStateId();

        if (open != menu || openState != state) {
            menu = open;
            state = openState;
            colors.clear();
        }

        Integer cached = colors.get(slot.index);
        if (cached != null) return cached == NO_COLOR ? null : cached;

        Integer color = compute.apply(slot.getItem());
        colors.put(slot.index, color == null ? NO_COLOR : color);
        return color;
    }
}
