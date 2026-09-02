package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class GearHud extends HudElement {
    private static final List<ItemStack> PREVIEW_STACKS = List.of(
            new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.PLAYER_HEAD),
            new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.NETHER_STAR)
    );
    // /equipment を一度も開いておらずスキャン前の場合のプレースホルダー
    private static final List<ItemStack> UNSCANNED_PLACEHOLDER = List.of(
            new ItemStack(Items.BARRIER), new ItemStack(Items.BARRIER),
            new ItemStack(Items.BARRIER), new ItemStack(Items.BARRIER)
    );
    private static final int SLOT_SIZE = 18;
    private static final int DEFAULT_SLOTS = 4;

    public GearHud() {
        super("gear", 10, 131, 1.0f, SLOT_SIZE * DEFAULT_SLOTS, SLOT_SIZE,
                () -> ModConfig.INSTANCE.generalHud.showGearHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        boolean vertical = ModConfig.INSTANCE.generalHud.gearHudOrientation == ModConfig.HudOrientation.VERTICAL;
        List<ItemStack> stacks;
        if (isPreview) {
            stacks = PREVIEW_STACKS;
        } else if (EquipmentState.items.isEmpty()) {
            stacks = UNSCANNED_PLACEHOLDER;
        } else {
            stacks = EquipmentState.items;
        }
        int count = Math.max(1, stacks.size());

        this.width = vertical ? SLOT_SIZE : SLOT_SIZE * count;
        this.height = vertical ? SLOT_SIZE * count : SLOT_SIZE;

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            int x = vertical ? 1 : i * SLOT_SIZE + 1;
            int y = vertical ? i * SLOT_SIZE + 1 : 1;
            graphics.item(stack, x, y);
        }
    }
}
