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
        // 26.2: Options.hideGui 廃止 -> Hud.isHidden()
        if (client.player == null || client.gui.hud.isHidden()) return;
        if (client.level == null) return;

        // エディタ画面が開いている間は、エディタ側の render が動くため重複を避ける
        // 26.2: Minecraft.screen 廃止 -> Gui.screen()
        if (client.gui.screen() instanceof HudEditorScreen) return;

        // Skyblock 以外では表示しない
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        // --- ボスのネームプレート(ワールド座標をスクリーンへ投影) ---
        // Glow(ポストエフェクト)より確実に手前へ出すため、ワールド内テキストではなくHUDとして描画する
        BossNameplateRenderer.render(graphics, client, deltaTracker.getGameTimeDeltaPartialTick(true));

        // --- 各HUD要素のループ描画 ---
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        for (HudElement element : HudConfig.ELEMENTS) {
            if (element.shouldRender(false)) {
                // 2D行列スタック (Matrix3x2fStack) の操作
                graphics.pose().pushMatrix();

                // 26.1.2 では Z軸指定が不要な translate(x, y) / scale(x, y) を使用
                graphics.pose().translate((float) element.renderX(screenWidth), (float) element.renderY(screenHeight));
                graphics.pose().scale(element.scale, element.scale);

                // 各HUDクラス側の renderElement(GuiGraphicsExtractor, boolean) を呼び出す
                // 実プレイ中も範囲を測っておく。画面内へ寄せる判定を実表示基準で行うため
                element.beginMeasure();
                element.renderElement(graphics, false);
                element.endMeasure();

                graphics.pose().popMatrix();
            }
        }

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