package com.deeply.gankura.mixin;

import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * エンティティ描画ループは、対象がいるチャンクセクションが「ビルド済みかつ可視」でないと描画をスキップする。
 *
 * <pre>
 * if (dispatcher.shouldRender(entity, frustum, x, y, z) || ...) {
 *     BlockPos pos = entity.blockPosition();
 *     if ((level.isOutsideBuildHeight(pos.getY()) || isSectionCompiledAndVisible(pos)) &amp;&amp; ...) {
 * </pre>
 *
 * Hypixelはクライアントの描画距離設定に関係なくエンティティを送ってくるため、
 * 「エンティティは存在するがチャンクが未描画」という状態が起こる。
 * この場合 Tracer(座標さえあれば描ける)だけが表示され、Glow(エンティティ描画が前提)が出ない。
 *
 * 追跡中のボスがいる座標に限りこの判定を通し、Tracer と Glow の見え方を揃える。
 */
@Mixin(LevelRenderer.class)
public class BossSectionVisibilityMixin {

    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void gankura$allowTrackedBossSection(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;

        // 呼び出し元は entity.blockPosition() をそのまま渡すため、完全一致で判定すれば
        // 他の用途(ブロックエンティティ等)の呼び出しに影響しない
        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            if (entity.blockPosition().equals(pos)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
