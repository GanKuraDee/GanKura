package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 死んだエンティティを、倒れて赤くなる動きを見せずにその場で消す。
 *
 * 死んだエンティティは、サーバーが取り除くまでの1秒ほど、倒れながら赤く光る姿で残る。
 * 描画するかどうかを決める入口で、死んだものを描かないことにする。
 * エンティティ自体はそのまま残るので、ほかの機能の判定には影響しない
 */
@Mixin(EntityRenderer.class)
public class HideDeathAnimationMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void gankura$hideDeathAnimation(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                            float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.Combat.hideDeathAnimation) return;
        if (!GameState.Server.isSkyblock()) return;
        if (!(entity instanceof LivingEntity living)) return;
        // 自分が消えると死亡画面まわりで困る。防具立ては名札やダメージ表示に使われていて、倒れる動きも無い
        if (living == Minecraft.getInstance().player || living instanceof ArmorStand) return;

        // 体力が 0 になった時点で消す。倒れ始める (deathTime が増える) のを待つと、その間だけ見えてしまう
        if (living.isDeadOrDying() || living.deathTime > 0) cir.setReturnValue(false);
    }
}
