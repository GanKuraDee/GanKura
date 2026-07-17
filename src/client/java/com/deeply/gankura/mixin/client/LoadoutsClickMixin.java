package com.deeply.gankura.mixin.client;

import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// "Loadouts" メニューはロードアウト切り替えスロットをクリックした瞬間にしか最新のPet/Equipment情報が
// 確定しないため(ホバーだけの継続スキャンでは切り替え前の古い情報が残り続ける)、クリックされた瞬間の
// スロットのツールチップからPet Hud用の情報を読み取り、合わせてEquipment Hudの再スキャンも行う
// onMouseClickはオーバーロードされているため、Mixinの対象メソッドをディスクリプタで明示する
@Mixin(HandledScreen.class)
public class LoadoutsClickMixin {
    private static final String LOADOUTS_TITLE = "Loadouts";

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (button != 0) return;
        if (slot == null || !slot.hasStack()) return;

        Screen screen = (Screen) (Object) this;
        if (!screen.getTitle().getString().contains(LOADOUTS_TITLE)) return;

        PetHandler.processLoadoutsSlotClick(slot.getStack());
        EquipmentScanner.onLoadoutsSlotClicked(((HandledScreen<?>) screen).getScreenHandler());
    }
}
