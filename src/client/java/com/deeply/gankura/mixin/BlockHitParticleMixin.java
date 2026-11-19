package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ブロックを掘っている間、叩いた面から飛び散り続ける粒を消す。
 *
 * 壊れた瞬間に飛び散る欠片 (BlockBreakParticleMixin) とは出どころが別で、こちらは掘っている間ずっと出る。
 * 粒を出すところだけを切り落とすので、叩く音や掘る速さはそのまま残る
 */
@Mixin(ClientLevel.class)
public class BlockHitParticleMixin {

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void gankura$hideMiningParticles(CallbackInfo ci) {
        if (!ModConfig.Mining.hideMiningParticles) return;
        if (!GameState.Server.isSkyblock()) return;

        ci.cancel();
    }
}
