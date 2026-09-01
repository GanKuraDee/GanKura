package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.InventoryButtonHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 持ち物画面の "Crafting" を消す。Inventory Button が文字に重なると読めなくなるため。
// この画面の extractLabels は "Crafting" しか描いていないので、丸ごと止めてよい
@Mixin(InventoryScreen.class)
public class CraftingLabelMixin {

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void gankura$hideCraftingLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (InventoryButtonHandler.shouldHideCraftingLabel((Screen) (Object) this)) ci.cancel();
    }
}
