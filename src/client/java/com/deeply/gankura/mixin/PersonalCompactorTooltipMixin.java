package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PersonalCompactorPreview;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * メニューでアイテムの説明を出すとき、Personal Compactor / Deletor なら
 * 登録してある品の並びを説明の画像として渡す。バンドルの中身と同じく、名前のすぐ下に出る
 */
@Mixin(AbstractContainerScreen.class)
public class PersonalCompactorTooltipMixin {

    @Redirect(
            method = "extractTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getTooltipImage()Ljava/util/Optional;")
    )
    private Optional<TooltipComponent> gankura$compactorPreview(ItemStack stack) {
        return PersonalCompactorPreview.tooltipImage(stack, stack.getTooltipImage());
    }
}
