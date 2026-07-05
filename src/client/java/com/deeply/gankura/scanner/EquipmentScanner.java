package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// これらはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
// メニューはラージチェスト(6行9列, スロット0-53)と同じ構造で、Equipmentは上から2行目〜5行目の2列目
// (スロット10, 19, 28, 37) に上から順に並んでいる。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(Minecraft client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;
        if (!screen.getTitle().getString().contains(EQUIPMENT_TITLE)) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (menu.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int slotIndex : EQUIPMENT_SLOTS) {
            ItemStack stack = menu.slots.get(slotIndex).getItem();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        EquipmentState.items = found;
        // CLIENT_STOPPING 時には既にワールドから切断済みでレジストリ情報が失われている場合があるため、
        // スキャンできた時点でその都度保存しておく
        if (client.level != null) {
            EquipmentState.save(client.level.registryAccess());
        }
    }
}
