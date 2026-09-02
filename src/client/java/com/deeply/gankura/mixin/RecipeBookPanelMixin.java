package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 開いたままのレシピ本を閉じた扱いにする。
// 画面の位置決めもこの答えを見ているので、開いていた分の横ずれも直る
@Mixin(RecipeBookComponent.class)
public class RecipeBookPanelMixin {

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void gankura$hideRecipeBook(CallbackInfoReturnable<Boolean> cir) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (config.enableInventoryTweaks && config.hideRecipeBook
                && GameState.Server.isSkyblock()) {
            cir.setReturnValue(false);
        }
    }
}
