package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.LavaTextureHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 溶岩に入っているときの、視界を覆うオレンジのフォグを消す。
 *
 * フォグの色も濃さも、カメラがどの流体に入っているかだけで決まる。
 * そこを外に出ているとき(ATMOSPHERIC)と同じ扱いにすれば、
 * 溶岩の中でも普段どおりの見え方になる。
 *
 * 溶岩を水の見た目にする機能の一部なので、その設定に合わせて働く
 */
@Mixin(FogRenderer.class)
public class LavaFogMixin {

    @Inject(method = "getFogType", at = @At("RETURN"), cancellable = true)
    private void gankura$hideLavaFog(Camera camera, CallbackInfoReturnable<FogType> cir) {
        if (cir.getReturnValue() != FogType.LAVA) return;
        if (!LavaTextureHandler.hidingFog()) return;

        cir.setReturnValue(FogType.ATMOSPHERIC);
    }
}
