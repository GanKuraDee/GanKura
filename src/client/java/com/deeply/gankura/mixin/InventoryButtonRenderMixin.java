package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.InventoryButtonHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory Button を収納画面に描く。
 *
 * 描く場所は extractRenderState の最後。ここだと
 *   ・中身とカーソルのアイテムより後 → ボタンがアイテムに隠れない
 *   ・説明の描画(extractDeferredElements)より前 → アイテムの説明がボタンの上に出る
 * の両方を満たせる。
 * Fabric の画面イベントは説明まで描き終えた後に呼ばれるため、ここでは使えない。
 *
 * レシピ本を持つ画面(プレイヤーの持ち物など)は extractRenderState を
 * 親を呼ばずに書き直しているので、そちらにも同じ処理を入れる
 */
@Mixin({AbstractContainerScreen.class, AbstractRecipeBookScreen.class})
public class InventoryButtonRenderMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gankura$renderInventoryButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        InventoryButtonHandler.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
    }
}
