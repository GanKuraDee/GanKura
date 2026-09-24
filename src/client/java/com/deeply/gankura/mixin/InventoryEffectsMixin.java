package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 持ち物画面の横に並ぶ効果の一覧を消す。
// SkyBlock では常時いくつも効果が付いていて、画面の右側を占領してしまう
@Mixin(EffectsInInventory.class)
public class InventoryEffectsMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void gankura$hideEffects(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (ModConfig.Interface.enableInventoryTweaks && ModConfig.Interface.hideInventoryEffects
                && GameState.Server.isSkyblock()) {
            ci.cancel();
        }
    }
}
