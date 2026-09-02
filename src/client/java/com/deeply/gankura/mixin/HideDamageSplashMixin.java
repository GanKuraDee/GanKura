package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.DamageSplashUtils;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ダメージ表示の数字を消す。
 *
 * 描画するかどうかを決める入口で切り落とすので、描画の中身には手を入れずに済む。
 * 当たり判定やエンティティ自体はそのまま残るため、他の機能の判定には影響しない。
 */
@Mixin(EntityRenderer.class)
public class HideDamageSplashMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void gankura$hideDamageSplash(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.INSTANCE.combat.hideDamageSplash) return;
        if (!GameState.Server.isSkyblock()) return;
        if (!(entity instanceof ArmorStand stand)) return;

        if (DamageSplashUtils.isDamageSplash(stand)) cir.setReturnValue(false);
    }
}
