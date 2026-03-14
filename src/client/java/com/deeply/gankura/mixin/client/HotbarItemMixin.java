package com.deeply.gankura.mixin.client;

import com.deeply.gankura.render.HotbarOverlayRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HotbarItemMixin {

    @Inject(method = "renderHotbarItem", at = @At("TAIL"))
    private void onRenderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        // 実際の描画・ロジック処理は専用のレンダラークラスに丸投げする
        HotbarOverlayRenderer.render(context, x, y, stack);
    }
}