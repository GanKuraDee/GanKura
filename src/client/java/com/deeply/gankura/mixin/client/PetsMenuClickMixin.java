package com.deeply.gankura.mixin.client;

import com.deeply.gankura.handler.PetHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// "Pets" メニューはActive Petの表示位置が固定でないため、アイテムを左クリックした
// 瞬間のスロットからツールチップを読み取る必要がある(読み取り自体はPetHandlerに委譲)
// onMouseClickはオーバーロードされているため、Mixinの対象メソッドをディスクリプタで明示する
@Mixin(HandledScreen.class)
public class PetsMenuClickMixin {
    private static final String PETS_TITLE = "Pets";

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (button != 0) return;
        if (slot == null || !slot.hasStack()) return;

        Screen screen = (Screen) (Object) this;
        if (!screen.getTitle().getString().contains(PETS_TITLE)) return;

        PetHandler.processPetsMenuClick(slot.getStack());
    }
}
