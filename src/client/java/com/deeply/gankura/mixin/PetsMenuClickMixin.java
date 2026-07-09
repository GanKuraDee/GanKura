package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PetHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// "Pets" メニューはActive Petの表示位置が固定でないため、アイテムを左クリックした
// 瞬間のスロットからツールチップを読み取る必要がある(読み取り自体はPetHandlerに委譲)
@Mixin(AbstractContainerScreen.class)
public class PetsMenuClickMixin {
    private static final String PETS_TITLE = "Pets";

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (buttonNum != 0) return;
        if (slot == null || !slot.hasItem()) return;

        Screen screen = (Screen) (Object) this;
        if (!screen.getTitle().getString().contains(PETS_TITLE)) return;

        PetHandler.processPetsMenuClick(slot.getItem());
    }
}
