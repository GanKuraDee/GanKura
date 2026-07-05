package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

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

    private static void scan(MinecraftClient client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!screen.getTitle().getString().contains(EQUIPMENT_TITLE)) return;

        ScreenHandler handler = screen.getScreenHandler();
        List<ItemStack> found = new ArrayList<>();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && hasEquipmentLore(stack)) {
                found.add(stack.copy());
            }
        }

        if (!found.isEmpty()) {
            EquipmentState.items = found;
        }
    }

    private static boolean hasEquipmentLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;

        for (Text line : lore.lines()) {
            if (line.getString().contains(EQUIPMENT_LORE_MARKER)) return true;
        }
        return false;
    }
}
