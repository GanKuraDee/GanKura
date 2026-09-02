package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 収納画面の後ろに敷かれる半透明の黒を描かない。
// 消しても他の画面(ポーズ画面など)の暗転はそのまま残す
@Mixin(Screen.class)
public class ScreenBackgroundMixin {

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void gankura$hideBackgroundDim(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractContainerScreen<?>)) return;

        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (config.enableInventoryTweaks && config.hideInventoryDim
                && GameState.Server.isSkyblock()) {
            ci.cancel();
        }
    }
}
