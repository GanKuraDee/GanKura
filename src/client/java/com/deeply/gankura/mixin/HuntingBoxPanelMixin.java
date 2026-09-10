package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.HuntingBoxPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hunting Box の横に、持っているシャードを売った額の一覧を描く。
 *
 * 描く場所は Shard Cost Panel と同じ extractRenderState の最後。
 * 中身より後、説明より前なので、箱がアイテムに隠れず、説明は箱の上に出る
 */
@Mixin(AbstractContainerScreen.class)
public class HuntingBoxPanelMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gankura$renderShardValues(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        HuntingBoxPanel.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
    }
}
