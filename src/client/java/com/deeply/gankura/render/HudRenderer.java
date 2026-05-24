package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.HudConfig;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class HudRenderer {

    // 引数の型を GuiGraphicsExtractor に変更
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        // 基本的なガード句
        if (client.player == null || client.options.hideGui) return;
        if (client.level == null) return;

        // エディタ画面が開いている間は、エディタ側の render が動くため重複を避ける
        if (client.screen instanceof HudEditorScreen) return;

        // Skyblock 以外では表示しない
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        // --- 各HUD要素のループ描画 ---
        for (HudElement element : HudConfig.ELEMENTS) {
            if (element.shouldRender(false)) {
                // 2D行列スタック (Matrix3x2fStack) の操作
                graphics.pose().pushMatrix();

                // 26.1.2 では Z軸指定が不要な translate(x, y) / scale(x, y) を使用
                graphics.pose().translate((float) element.x, (float) element.y);
                graphics.pose().scale(element.scale, element.scale);

                // 各HUDクラス側の renderElement(GuiGraphicsExtractor, boolean) を呼び出す
                element.renderElement(graphics, false);

                graphics.pose().popMatrix();
            }
        }

        // --- エンティティトレーサー（画面中央から対象エンティティへの線） ---
        EntityTracerRenderer.render(graphics, client);

        // --- サーバーリブート警告（画面中央固定） ---
        if (ModConfig.INSTANCE.misc.enableRebootAlert && GameState.Server.isClosing && GameState.Server.closingTime != null) {
            renderServerClosingAlert(graphics, client, client.font);
        }

    }

    private static void renderServerClosingAlert(GuiGraphicsExtractor graphics, Minecraft client, Font font) {
        String text = "Server closing: " + GameState.Server.closingTime;

        graphics.pose().pushMatrix();

        // 画面中央へ移動 (Z軸なし)
        graphics.pose().translate(client.getWindow().getGuiScaledWidth() / 2f, client.getWindow().getGuiScaledHeight() / 2f);
        // 2倍サイズで強調表示
        graphics.pose().scale(2.0f, 2.0f);

        /*
         * GuiGraphicsExtractor のメソッド:
         * text(Font font, String str, int x, int y, int color, boolean dropShadow)
         * 中央揃えのため font.width(text) / 2 を引いています
         */
        graphics.text(font, text, -font.width(text) / 2, -9 / 2, 0xFFFF5555, true);

        graphics.pose().popMatrix();
    }
}