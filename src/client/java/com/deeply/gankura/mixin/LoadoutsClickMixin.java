package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// "Loadouts" メニューはロードアウト切り替えスロットをクリックした瞬間にしか最新のPet/Equipment情報が
// 確定しないため(ホバーだけの継続スキャンでは切り替え前の古い情報が残り続ける)、クリックされた瞬間の
// スロットのツールチップからPet Hud用の情報を読み取り、合わせてEquipment Hudの再スキャンも行う
@Mixin(AbstractContainerScreen.class)
public class LoadoutsClickMixin {
    private static final String LOADOUTS_TITLE = "Loadouts";

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (buttonNum != 0) return;
        if (slot == null || !slot.hasItem()) return;

        Screen screen = (Screen) (Object) this;
        if (!screen.getTitle().getString().contains(LOADOUTS_TITLE)) return;

        PetHandler.processLoadoutsSlotClick(slot.getItem());
        EquipmentScanner.onLoadoutsSlotClicked(((AbstractContainerScreen<?>) screen).getMenu());
    }
}
