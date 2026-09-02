package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HotbarOverlayRenderer {

    // 引数の型を GuiGraphicsExtractor に変更
    public static void render(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) return;

        if (!ModConfig.INSTANCE.combat.showPoisonIndicator) return;

        // Mojang: is(Item)
        if (stack.is(Items.BOW)) {
            renderPoisonIndicator(graphics, x, y);
        }
    }

    private static void renderPoisonIndicator(GuiGraphicsExtractor graphics, int x, int y) {
        ItemStack dyeStack = null;

        if ("TWILIGHT".equals(GameState.Player.activePoison)) {
            dyeStack = new ItemStack(Items.PURPLE_DYE);
        } else if ("TOXIC".equals(GameState.Player.activePoison)) {
            dyeStack = new ItemStack(Items.LIME_DYE);
        }

        if (dyeStack != null) {
            Font font = Minecraft.getInstance().font;

            // 1. 染料アイコンの描画
            // 26.1.2 では Matrix3x2fStack を使用
            graphics.pose().pushMatrix();

            // translate は float の x, y を指定 (26.1.2 形式)
            graphics.pose().translate(x + 9f, y - 3f);
            // scale も 2D 用の引数 (x, y)
            graphics.pose().scale(0.55f, 0.55f);

            /*
             * GuiGraphicsExtractor のメソッド:
             * public void item(ItemStack itemStack, int x, int y)
             * ※内部で自動的に GuiItemRenderState が生成されます
             */
            graphics.item(dyeStack, 0, 0);

            graphics.pose().popMatrix();

            // 2. 残り個数の描画
            if (GameState.Player.activePoisonCount > 0) {
                String countText = String.valueOf(GameState.Player.activePoisonCount);

                /*
                 * GuiGraphicsExtractor のメソッド:
                 * public void itemDecorations(Font font, ItemStack itemStack, int x, int y, String countText)
                 * ※ vanilla の renderItemDecorations に相当
                 */
                graphics.itemDecorations(font, dyeStack, x, y - 2, countText);
            }
        }
    }
}