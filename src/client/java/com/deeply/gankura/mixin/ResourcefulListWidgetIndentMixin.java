package com.deeply.gankura.mixin;

import com.deeply.gankura.config.ConfigAccordions;
import com.teamresourceful.resourcefulconfig.client.components.base.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 設定画面の一覧で、折りたたみ見出しの中にある行を見出しの深さぶん右へずらす。
 * 行の位置と幅はリストが毎フレーム決め直しているので、その指定をここで書き換える。
 * 字下げの量が登録されていない行（GanKura 以外の設定など）は元のまま
 */
@Mixin(ListWidget.class)
public class ResourcefulListWidgetIndentMixin {

    @Redirect(
            method = {"extractWidgetRenderState", "updateLastHeight"},
            at = @At(value = "INVOKE",
                    target = "Lcom/teamresourceful/resourcefulconfig/client/components/base/ListWidget$Item;setItemWidth(I)V")
    )
    private void gankura$indentWidth(ListWidget.Item item, int width) {
        item.setItemWidth(width - ConfigAccordions.indentOf(item));
    }

    @Redirect(
            method = {"extractWidgetRenderState", "updateLastHeight"},
            at = @At(value = "INVOKE",
                    target = "Lcom/teamresourceful/resourcefulconfig/client/components/base/ListWidget$Item;setX(I)V")
    )
    private void gankura$indentX(ListWidget.Item item, int x) {
        item.setX(x + ConfigAccordions.indentOf(item));
    }
}
