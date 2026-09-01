package com.deeply.gankura.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Inventory Button を画面の角に合わせて置くために、収納画面の位置と大きさを読む。
// どれも protected なので、外から見るにはこの入口が要る
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {

    @Accessor("leftPos")
    int gankura$getLeftPos();

    @Accessor("topPos")
    int gankura$getTopPos();

    @Accessor("imageWidth")
    int gankura$getImageWidth();

    @Accessor("imageHeight")
    int gankura$getImageHeight();
}
