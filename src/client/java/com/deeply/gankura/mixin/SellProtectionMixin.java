package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.SellProtection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * メニューのクリックをサーバーへ送る前に、レアなアイテムを NPC に売ろうとしていないかを見る。
 * 止めるときはここで打ち切るので、売却の操作はサーバーへ届かない
 */
@Mixin(AbstractContainerScreen.class)
public class SellProtectionMixin {

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"), cancellable = true)
    private void gankura$preventRareSell(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (SellProtection.shouldBlock(screen.getMenu(), slot, input)) ci.cancel();
    }
}
