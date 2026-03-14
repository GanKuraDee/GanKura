package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HotbarItemMixin {

    @Inject(method = "renderHotbarItem", at = @At("TAIL"))
    private void onRenderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (stack.isEmpty()) return;

        // ★追加: 設定でPoison IndicatorがOFFになっている場合は何もせずに終了する
        if (!ModConfig.showPoisonIndicator) return;

        // JujuやTerminatorはすべてバニラの弓(BOW)として判定される
        if (stack.isOf(Items.BOW)) {
            ItemStack dyeStack = null;

            if ("TWILIGHT".equals(GameState.Player.activePoison)) {
                dyeStack = new ItemStack(Items.PURPLE_DYE);
            } else if ("TOXIC".equals(GameState.Player.activePoison)) {
                dyeStack = new ItemStack(Items.LIME_DYE);
            }

            if (dyeStack != null) {
                // 1. 染料アイコンの描画
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(x + 9, y - 3);
                context.getMatrices().scale(0.55f, 0.55f);
                context.drawItem(dyeStack, 0, 0);
                context.getMatrices().popMatrix();

                // =======================================================
                // ★修正: 2. 残り個数の描画 (最新バージョンの仕様に対応)
                // =======================================================
                if (GameState.Player.activePoisonCount > 0) {
                    String countText = String.valueOf(GameState.Player.activePoisonCount);
                    net.minecraft.client.font.TextRenderer tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

                    // バニラ標準の「アイテムの右下に個数を描画する機能」を使うことで、
                    // 奥行き(Z軸)の問題や行列(Matrix)のエラーを完全に回避して最前面に表示させます。
                    // 座標は「弓の右上に描画した染料のすぐ下」になるように調整しています。
                    context.drawStackOverlay(tr, dyeStack, x, y - 2, countText);
                }
                // =======================================================
            }
        }
    }
}