package com.deeply.gankura.mixin.client;

import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hypixel のモブは、頭に skull を被せたアーマースタンドで見た目を作っていることが多い。
 * このアーマースタンドが marker でない場合、Glowing の輪郭が本体(腕・胴・脚)にも出てしまい、
 * 見た目より大きな輪郭になる。
 *
 * marker のアーマースタンドは本体モデルが描画されず装備だけが描かれるため、
 * ハイライト中のものに限り描画時だけ marker 扱いにして、輪郭をヘッドだけに絞る。
 * 書き換えるのは描画状態だけなので、当たり判定やクリック判定には影響しない。
 */
@Mixin(ArmorStandEntityRenderer.class)
public class ArmorStandHeadOnlyGlowMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/ArmorStandEntity;Lnet/minecraft/client/render/entity/state/ArmorStandEntityRenderState;F)V",
            at = @At("TAIL"))
    private void gankura$hideBodyForHighlight(ArmorStandEntity entity, ArmorStandEntityRenderState renderState, float tickProgress, CallbackInfo ci) {
        if (EntityHighlightManager.headOnlyGlowEntities.contains(entity)) renderState.marker = true;
    }
}
