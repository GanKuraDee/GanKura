package com.deeply.gankura.mixin;

import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
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
@Mixin(ArmorStandRenderer.class)
public class ArmorStandHeadOnlyGlowMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V",
            at = @At("TAIL"))
    private void gankura$hideBodyForHighlight(ArmorStand entity, ArmorStandRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (EntityHighlightManager.headOnlyGlowEntities.contains(entity)) renderState.isMarker = true;
    }
}
