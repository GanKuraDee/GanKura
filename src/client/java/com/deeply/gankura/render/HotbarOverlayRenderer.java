package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class HotbarOverlayRenderer {

    public static void render(DrawContext context, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) return;

        // 設定でPoison IndicatorがOFFになっている場合は何もせずに終了する
        if (!ModConfig.INSTANCE.misc.showPoisonIndicator) return;

        // =======================================================
        // 弓(Juju/Terminator) への Poison Indicator 描画
        // =======================================================
        if (stack.isOf(Items.BOW)) {
            renderPoisonIndicator(context, x, y);
        }

        // ★将来、釣り竿のエサなどを追加したい場合はここに足すだけ！
        // if (stack.isOf(Items.FISHING_ROD)) {
        //     renderBaitIndicator(context, x, y);
        // }
    }

    private static void renderPoisonIndicator(DrawContext context, int x, int y) {
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

            // 2. 残り個数の描画
            if (GameState.Player.activePoisonCount > 0) {
                String countText = String.valueOf(GameState.Player.activePoisonCount);
                TextRenderer tr = MinecraftClient.getInstance().textRenderer;

                // バニラ標準の「アイテムの右下に個数を描画する機能」を使うことで、
                // 奥行き(Z軸)の問題や行列(Matrix)のエラーを完全に回避して最前面に表示させます。
                context.drawStackOverlay(tr, dyeStack, x, y - 2, countText);
            }
        }
    }
}