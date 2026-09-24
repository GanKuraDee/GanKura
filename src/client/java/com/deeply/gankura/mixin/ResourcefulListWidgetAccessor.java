package com.deeply.gankura.mixin;

import com.teamresourceful.resourcefulconfig.client.components.base.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** 折りたたみを開閉して一覧を組み直しても、スクロール位置を保つために使う */
@Mixin(ListWidget.class)
public interface ResourcefulListWidgetAccessor {

    @Accessor("scroll")
    double gankura$getScroll();

    @Accessor("scroll")
    void gankura$setScroll(double scroll);

    @Accessor("items")
    List<ListWidget.Item> gankura$getItems();

    @Invoker("updateScrollBar")
    void gankura$updateScrollBar();
}
