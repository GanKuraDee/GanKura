package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.AttributeCostPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attribute Menu の横に、シャードの値段の一覧を描く。
 *
 * 描く場所は Inventory Button と同じ extractRenderState の最後。
 * 中身より後、説明より前なので、箱がアイテムに隠れず、説明は箱の上に出る
 */
@Mixin(AbstractContainerScreen.class)
public class AttributeCostPanelMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gankura$renderAttributeCosts(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick, CallbackInfo ci) {
        AttributeCostPanel.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
    }
}
