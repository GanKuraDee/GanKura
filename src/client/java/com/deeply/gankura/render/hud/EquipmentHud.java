package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EquipmentHud extends HudElement {
    private static final EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final ItemStack[] PREVIEW_STACKS = {
            new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)
    };
    private static final int SLOT_SIZE = 18;

    public EquipmentHud() {
        super("equipment", 10, 119, 1.0f, SLOT_SIZE * SLOTS.length, SLOT_SIZE,
                () -> ModConfig.INSTANCE.misc.showEquipmentHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        boolean vertical = ModConfig.INSTANCE.misc.equipmentHudOrientation == ModConfig.HudOrientation.VERTICAL;
        this.width = vertical ? SLOT_SIZE : SLOT_SIZE * SLOTS.length;
        this.height = vertical ? SLOT_SIZE * SLOTS.length : SLOT_SIZE;

        Player player = Minecraft.getInstance().player;

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = isPreview ? PREVIEW_STACKS[i] : (player != null ? player.getItemBySlot(SLOTS[i]) : ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            int x = vertical ? 1 : i * SLOT_SIZE + 1;
            int y = vertical ? i * SLOT_SIZE + 1 : 1;
            graphics.item(stack, x, y);
        }
    }
}
