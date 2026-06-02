package com.deeply.gankura.mixin;

import com.deeply.gankura.data.CrimsonBossEntry;
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

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceBossGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void overrideGlowingColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;

        if (EntityHighlightManager.highlightedEntities.contains(entity)) {
            // Crimson Isle ボス（Wither Skeleton 等を含む）
            CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
            if (boss != null) {
                cir.setReturnValue(boss.glowColorRGB());
                return;
            }

            // Magma Glare (Magma Boss 配下の MagmaCube): 赤
            if (EntityHighlightManager.magmaGlareEntities.contains(entity)) {
                cir.setReturnValue(0xFF5555);
                return;
            }

            if (entity instanceof IronGolem) {
                cir.setReturnValue(0xFFAA00); // ゴーレム: 金色
            } else if (entity instanceof Spider) {
                cir.setReturnValue(0xFF5555); // ブルードマザー: 赤色
            } else if (entity instanceof EnderDragon) {
                cir.setReturnValue(0xFF55FF); // ドラゴン: ライトパープル
            } else {
                cir.setReturnValue(0xFFFFFF);
            }
        }
    }
}
