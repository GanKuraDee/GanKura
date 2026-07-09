package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// "/loadouts" (Loadouts) メニューでも同じスロット構成でEquipmentが表示されるため、こちらも読み取り対象とする。
// これらはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
// メニューはラージチェスト(6行9列, スロット0-53)と同じ構造で、Equipmentは上から2行目〜5行目の2列目
// (スロット10, 19, 28, 37) に上から順に並んでいる。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final String EQUIPMENT_TITLE_LOADOUTS = "Loadouts";
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.contains(EQUIPMENT_TITLE) && !title.contains(EQUIPMENT_TITLE_LOADOUTS)) return;

        ScreenHandler handler = screen.getScreenHandler();
        if (handler.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int slotIndex : EQUIPMENT_SLOTS) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        EquipmentState.items = found;
        // CLIENT_STOPPING 時には既にワールドから切断済みでレジストリ情報が失われている場合があるため、
        // スキャンできた時点でその都度保存しておく
        if (client.world != null) {
            EquipmentState.save(client.world.getRegistryManager());
        }
    }
}
