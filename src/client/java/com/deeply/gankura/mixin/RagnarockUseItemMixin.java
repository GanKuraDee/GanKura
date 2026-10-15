package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.RagnarockHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 右クリックで使う操作をした瞬間を拾う。Ragnarock の詠唱の残り時間を、サーバーの知らせを待たずに数え始めるため。
 * 空中・ブロック・エンティティのどれに向けて押しても、ここを通る
 */
@Mixin(Minecraft.class)
public class RagnarockUseItemMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void gankura$onStartUseItem(CallbackInfo ci) {
        RagnarockHandler.onUseItem((Minecraft) (Object) this);
    }
}
