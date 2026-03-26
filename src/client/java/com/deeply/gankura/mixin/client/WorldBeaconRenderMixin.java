package com.deeply.gankura.mixin.client;

import com.deeply.gankura.render.GolemBeaconRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldBeaconRenderMixin {

    // 1.21.11の最新仕様に完全対応
    // ブロックエンティティのステートを収集する "fillBlockEntityRenderStates" メソッドの終わりに割り込む
    @Inject(method = "fillBlockEntityRenderStates", at = @At("RETURN"))
    private void injectCustomRenderStates(Camera camera, float tickProgress, WorldRenderState renderStates, CallbackInfo ci) {

        // マイクラ標準の収集が終わった直後に、私たちの「偽ビーコン」のデータを追加する
        GolemBeaconRenderer.submitBeaconState(renderStates, camera);

    }
}