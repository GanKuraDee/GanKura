package com.deeply.gankura.mixin;

import com.deeply.gankura.data.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 一人称視点で画面右下に表示される手持ちアイテムの大きさ・位置を設定値に応じて調整する。
// pushPose直後(位置決め前)にスケールすると、アームの位置オフセット(translate)も同じ係数で
// 縮んでカメラに近づいてしまい、遠近法により見かけ上のサイズ変化が相殺されてしまう。
// そのため、位置決めがすべて完了しアイテム本体を描画するrenderItem呼び出し直前で
// 先にオフセットをtranslateし、その後にスケールする(NoFrills/DulkirMod-Fabricの実装を参考)
@Mixin(ItemInHandRenderer.class)
public class HeldItemScaleMixin {

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
    )
    private void onSubmitArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                      float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                      SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        float offsetX = ModConfig.INSTANCE.misc.heldItemOffsetX;
        float offsetY = ModConfig.INSTANCE.misc.heldItemOffsetY;
        if (offsetX != 0.0f || offsetY != 0.0f) {
            poseStack.translate(offsetX, offsetY, 0.0f);
        }

        float scale = ModConfig.INSTANCE.misc.heldItemScale;
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
