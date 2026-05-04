package com.deeply.gankura.mixin;

import com.deeply.gankura.render.HudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor; // GuiGraphicsから置き換え
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HudRenderMixin {

    /**
     * Minecraft 26.1.2 における描画の起点メソッド:
     * public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // HudRenderer.render も GuiGraphicsExtractor を受け取るように修正済み
        HudRenderer.render(graphics, deltaTracker);
    }
}