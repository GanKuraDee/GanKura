package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * レシピ本の開閉ボタンを置かない。
 *
 * このメソッドはボタンを足すのと同時に、レシピ本そのものを
 * 操作の受け取り先としても登録している。まとめて省くことで、
 * 見えない当たり判定も残らない
 */
@Mixin(AbstractRecipeBookScreen.class)
public class RecipeBookButtonMixin {

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void gankura$hideRecipeBookButton(CallbackInfo ci) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (config.enableInventoryTweaks && config.hideRecipeBook
                && GameState.Server.isSkyblock()) {
            ci.cancel();
        }
    }
}
