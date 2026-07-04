package com.deeply.gankura.mixin;

import com.deeply.gankura.render.GolemBeaconRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
// 26.2: 描画状態抽出はLevelRendererからLevelExtractorへ分離された
import net.minecraft.client.renderer.extract.LevelExtractor;
// パッケージが .state.level.LevelRenderState であることをソースL66で確認
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class WorldBeaconRenderMixin {

    // ソースL109にある levelRenderState フィールドを参照するためにShadow化
    @Shadow private LevelRenderState levelRenderState;

    /**
     * 26.2 における描画状態収集メソッド:
     * public void extract(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick)
     * (旧: LevelRenderer#extractLevel)
     */
    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        /*
         * ソース L438 以降でエンティティや天候の抽出（extract）が行われています。
         * その処理がすべて終わった RETURN (最後) のタイミングで、
         * 私たちのビーコン描画データを levelRenderState に追加します。
         */
        GolemBeaconRenderer.submitBeaconState(this.levelRenderState, camera);
    }
}