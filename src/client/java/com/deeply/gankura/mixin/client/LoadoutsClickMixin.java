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

// "Loadouts" メニューのロードアウト切り替えスロットをクリックした瞬間、そのスロット自身のツールチップから
// Pet Hud用の情報を読み取る(こちらはロードアウトに保存された情報のためクリック時点で確定している)。
// Equipment Hudについては、クリック直後はまだサーバー側の切り替えが反映されていないため、
// ここでは全EquipmentスロットをBarrier(Unknown)化するだけにとどめ、実際の読み取りは
// LoadoutsContentSyncMixin(サーバーからコンテナ内容が同期された瞬間)に委譲する。
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
        EquipmentScanner.resetToUnknown();
    }
}
