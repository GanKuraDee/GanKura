package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.VisitorPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 来客の画面の横に、欲しがっている品の一覧を描く。
 *
 * 描く場所は Attribute の一覧と同じ extractRenderState の最後。
 * 中身より後、説明より前なので、箱がアイテムに隠れず、説明は箱の上に出る
 */
@Mixin(AbstractContainerScreen.class)
public class VisitorPanelMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gankura$renderVisitorItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        VisitorPanel.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
    }
}
