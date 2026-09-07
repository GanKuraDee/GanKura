package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PestVacuumHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vacuum を左クリックした瞬間を拾う。
 *
 * 左クリックは空振り・ブロック・エンティティのどれもここを通るので、
 * 何に向けて振っても読み始められる
 */
@Mixin(Minecraft.class)
public class PestVacuumAttackMixin {

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void gankura$onPestVacuumAttack(CallbackInfoReturnable<Boolean> cir) {
        PestVacuumHandler.onAttack((Minecraft) (Object) this);
    }
}
