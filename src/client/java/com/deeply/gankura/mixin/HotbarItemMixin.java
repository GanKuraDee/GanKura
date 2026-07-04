package com.deeply.gankura.mixin;

import com.deeply.gankura.render.HotbarOverlayRenderer;
import net.minecraft.client.DeltaTracker;
// 26.2: extractSlot は Gui から Hud へ移動
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2の描画クラス
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HotbarItemMixin {

    /**
     * 26.2 におけるスロット描画メソッド (旧: Gui#extractSlot):
     * private void extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed)
     */
    @Inject(
            method = "extractSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onExtractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        // バニラのアイテム装飾（個数など）の描画直後に、独自のオーバーレイ（毒など）を描画します
        HotbarOverlayRenderer.render(graphics, x, y, stack);
    }
}