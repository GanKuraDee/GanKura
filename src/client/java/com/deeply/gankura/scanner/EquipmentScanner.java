package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// これらはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final String EQUIPMENT_LORE_MARKER = "This item can be worn as Equipment!";

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(Minecraft client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!screen.getTitle().getString().contains(EQUIPMENT_TITLE)) return;

        AbstractContainerMenu menu = screen.getMenu();
        List<ItemStack> found = new ArrayList<>();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && hasEquipmentLore(stack)) {
                found.add(stack.copy());
            }
        }

        if (!found.isEmpty()) {
            EquipmentState.items = found;
        }
    }

    private static boolean hasEquipmentLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            if (line.getString().contains(EQUIPMENT_LORE_MARKER)) return true;
        }
        return false;
    }
}
