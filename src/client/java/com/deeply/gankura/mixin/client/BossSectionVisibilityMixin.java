package com.deeply.gankura.mixin.client;

import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * エンティティ描画ループは、対象がいるチャンクセクションが「ビルド済みかつ可視」でないと描画をスキップする。
 *
 * <pre>
 * if (entityRenderManager.shouldRender(entity, frustum, x, y, z) || ...) {
 *     BlockPos pos = entity.getBlockPos();
 *     if ((world.isOutOfHeightLimit(pos.getY()) || isRenderingReady(pos)) &amp;&amp; ...) {
 * </pre>
 *
 * Hypixelはクライアントの描画距離設定に関係なくエンティティを送ってくるため、
 * 「エンティティは存在するがチャンクが未描画」という状態が起こる。
 * この場合 Tracer(座標さえあれば描ける)だけが表示され、Glow(エンティティ描画が前提)が出ない。
 *
 * 追跡中のボスがいる座標に限りこの判定を通し、Tracer と Glow の見え方を揃える。
 */
@Mixin(WorldRenderer.class)
public class BossSectionVisibilityMixin {

    @Inject(method = "isRenderingReady", at = @At("HEAD"), cancellable = true)
    private void gankura$allowTrackedBossSection(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;

        // 呼び出し元は entity.getBlockPos() をそのまま渡すため、完全一致で判定すれば
        // 他の用途(ブロックエンティティ等)の呼び出しに影響しない
        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            if (entity.getBlockPos().equals(pos)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
