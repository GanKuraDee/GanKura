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

    // ★修正: Sodiumに上書きされてしまうメソッドから、絶対に上書きされない「アウトライン収集メソッド」にお引越し！
    @Inject(method = "fillEntityOutlineRenderStates", at = @At("RETURN"))
    private void injectCustomRenderStates(Camera camera, WorldRenderState renderStates, CallbackInfo ci) {

        // マイクラ(またはSodium)の標準収集が終わった直後に、私たちの「偽ビーコン」のデータを追加する
        GolemBeaconRenderer.submitBeaconState(renderStates, camera);

    }
}