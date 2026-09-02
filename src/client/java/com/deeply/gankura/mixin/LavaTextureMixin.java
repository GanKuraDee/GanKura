package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.LavaTextureHandler;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 溶岩を水の見た目で描く。
 *
 * 流体の見た目は FluidModel にまとまっているので、溶岩を引かれたときに
 * 水のものを返すだけでよい。スプライトだけでなく描画層と色も一緒に入れ替わるため、
 * 溶岩がそのまま水として描かれる。
 *
 * ここはチャンクを組み立てるときに呼ばれる。設定を変えた後の組み直しは
 * LavaTextureHandler が受け持つ
 */
@Mixin(FluidStateModelSet.class)
public class LavaTextureMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void gankura$lavaAsWater(FluidState state, CallbackInfoReturnable<FluidModel> cir) {
        if (!LavaTextureHandler.replacing()) return;

        // 流れている溶岩は流れている水に合わせる
        FluidState water;
        if (state.getType() == Fluids.LAVA) {
            water = Fluids.WATER.defaultFluidState();
        } else if (state.getType() == Fluids.FLOWING_LAVA) {
            water = Fluids.FLOWING_WATER.defaultFluidState();
        } else {
            return;
        }

        cir.setReturnValue(((FluidStateModelSet) (Object) this).get(water));
    }
}
