package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerGlowingMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void forceBossGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void overrideGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
        if (boss != null) cir.setReturnValue(boss.glowColorRGB());
    }
}
