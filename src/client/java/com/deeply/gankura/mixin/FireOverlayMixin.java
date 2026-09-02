package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 燃えているときに一人称の画面下へ出る炎を消す。
 *
 * 炎を描く手前で切り落とすだけなので、燃えていること自体や
 * ダメージ、体力バーの表示には手を入れていない
 */
@Mixin(ScreenEffectRenderer.class)
public class FireOverlayMixin {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void gankura$hideFireOverlay(PoseStack pose, MultiBufferSource second,
                                                TextureAtlasSprite sprite, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.combat.hideFireOverlay) return;
        if (!GameState.Server.isSkyblock()) return;

        ci.cancel();
    }
}
