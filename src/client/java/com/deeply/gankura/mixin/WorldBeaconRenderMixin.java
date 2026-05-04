package com.deeply.gankura.mixin;

import com.deeply.gankura.render.GolemBeaconRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
// パッケージが .state.level.LevelRenderState であることをソースL66で確認
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldBeaconRenderMixin {

    // ソースL109にある levelRenderState フィールドを参照するためにShadow化
    @Shadow private LevelRenderState levelRenderState;

    /**
     * Minecraft 26.1.2 における描画状態収集メソッド:
     * public void extractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick)
     */
    @Inject(method = "extractLevel", at = @At("RETURN"))
    private void onExtractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        /*
         * ソース L438 以降でエンティティや天候の抽出（extract）が行われています。
         * その処理がすべて終わった RETURN (最後) のタイミングで、
         * 私たちのビーコン描画データを levelRenderState に追加します。
         */
        GolemBeaconRenderer.submitBeaconState(this.levelRenderState, camera);
    }
}