package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 一人称視点で画面右下に表示される手持ちアイテムの大きさを設定値に応じて縮小する。
// アーム/アイテムの回転・位置オフセットが適用される前(push直後)にスケールすることで、
// カメラ原点を中心にアーム全体(位置+大きさ)を縮小できる。popで自動的に元に戻るため
// メインハンド/オフハンドそれぞれ独立して適用される
@Mixin(HeldItemRenderer.class)
public class HeldItemScaleMixin {

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER)
    )
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                          float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                          OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        float scale = ModConfig.INSTANCE.misc.heldItemScale;
        if (scale != 1.0f) {
            matrices.scale(scale, scale, scale);
        }
    }
}
