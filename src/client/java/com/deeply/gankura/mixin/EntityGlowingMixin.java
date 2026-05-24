package com.deeply.gankura.mixin;

import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {

    // 1. 強制的に発光(Glowing)をONにする
    // ★変更: Yarnの "isGlowing" は Mojangでは "isCurrentlyGlowing" となります
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceBossGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    // =======================================================
    // ★追加: 発光色（アウトラインの色）を強制的に上書きする
    // マイクラが色を取得しようとした瞬間に割り込み、好きな色を渡します。
    // =======================================================
    // ★変更: Yarnの "getTeamColorValue" は Mojangでは "getTeamColor" となります
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void overrideGlowingColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;

        // 私たちのハイライトリストに入っているエンティティなら...
        if (EntityHighlightManager.highlightedEntities.contains(entity)) {

            // --- ボスごとに色を変更 ---
            if (entity instanceof IronGolem) {
                // ゴーレム: 金色 (Hexコード: FFAA00)
                cir.setReturnValue(0xFFAA00);
            }
            else if (entity instanceof Spider) {
                // ブルードマザー: 赤色 (Hexコード: FF5555)
                cir.setReturnValue(0xFF5555);
            }
            else if (entity instanceof EnderDragon) {
                // ドラゴン: ライトパープル (Hexコード: FF55FF)
                cir.setReturnValue(0xFF55FF);
            }
            else {
                // それ以外（予備）: 白色
                cir.setReturnValue(0xFFFFFF);
            }
        }
    }
}