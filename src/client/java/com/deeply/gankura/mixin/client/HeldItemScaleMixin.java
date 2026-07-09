package com.deeply.gankura.mixin.client;

import com.deeply.gankura.GanKura;
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
// push直後(位置オフセット適用前)にスケールすると、アームの位置オフセット(translate)も
// 同じ係数で縮んでカメラに近づいてしまい、遠近法により見かけ上のサイズ変化が相殺されてしまう。
// そのため、位置決めがすべて完了しアイテム本体を描画するrenderItem呼び出し直前にスケールする
// (NoFrills/DulkirMod-Fabricの実装を参考にした手法)
@Mixin(HeldItemRenderer.class)
public class HeldItemScaleMixin {
    // ★デバッグ用: Mixinが実際に発火し、どのスケール値を読んでいるかをログに残す(値が変化した時のみ)
    private static float lastLoggedScale = Float.NaN;

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            )
    )
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                          float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                          OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        float scale = ModConfig.INSTANCE.misc.heldItemScale;
        if (scale != lastLoggedScale) {
            GanKura.LOGGER.info("[HeldItemScaleMixin] renderFirstPersonItem fired, applying scale={}", scale);
            lastLoggedScale = scale;
        }
        if (scale != 1.0f) {
            matrices.scale(scale, scale, scale);
        }
    }
}
