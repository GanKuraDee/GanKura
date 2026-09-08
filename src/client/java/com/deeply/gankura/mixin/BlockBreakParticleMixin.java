package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ブロックが壊れたときに飛び散る欠片を消す。
 *
 * 粒を出す入口で切り落とすので、音や壊れる動き自体はそのまま残る。
 * 自分が壊したぶんも、周りの人が壊したぶんも同じ入口を通る
 */
@Mixin(ClientLevel.class)
public class BlockBreakParticleMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void gankura$hideBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.misc.hideBlockBreakParticles) return;
        if (!GameState.Server.isSkyblock()) return;

        ci.cancel();
    }
}
