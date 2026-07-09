package com.deeply.gankura.mixin;

import com.deeply.gankura.GanKura;
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

// 一人称視点で画面右下に表示される手持ちアイテムの大きさを設定値に応じて縮小する。
// アーム/アイテムの回転・位置オフセットが適用される前(pushPose直後)にスケールすることで、
// カメラ原点を中心にアーム全体(位置+大きさ)を縮小できる。popPoseで自動的に元に戻るため
// メインハンド/オフハンドそれぞれ独立して適用される
@Mixin(ItemInHandRenderer.class)
public class HeldItemScaleMixin {
    // ★デバッグ用: Mixinが実際に発火し、どのスケール値を読んでいるかをログに残す(値が変化した時のみ)
    private static float lastLoggedScale = Float.NaN;

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                      float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                      SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        float scale = ModConfig.INSTANCE.misc.heldItemScale;
        if (scale != lastLoggedScale) {
            GanKura.LOGGER.info("[HeldItemScaleMixin] renderArmWithItem fired, applying scale={}", scale);
            lastLoggedScale = scale;
        }
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
