package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.DnaAnalyzerHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DNA Analyzer の盤の上では、品の説明を出さない。
 *
 * どの枠も同じ説明しか持たないうえ、
 * 塗って示している隣の枠を覆い隠してしまう
 */
@Mixin(AbstractContainerScreen.class)
public class DnaAnalyzerTooltipMixin {

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void gankura$hideDnaAnalyzerTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!DnaAnalyzerHandler.inMenu(screen.getTitle().getString())) return;

        Slot hovered = ((ContainerScreenAccessor) this).gankura$getHoveredSlot();
        if (hovered == null) return;

        // 枠が持つ番号は器ごとの通し番号で、持ち物側とぶつかる。器の並びから引き直す
        int slotId = ((AbstractContainerScreen<?>) screen).getMenu().slots.indexOf(hovered);
        if (!DnaAnalyzerHandler.hidesTooltip(slotId)) return;

        ci.cancel();
    }
}
