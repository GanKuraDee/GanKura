package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.HudConfig;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class HudRenderer {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.world == null) return;
        if (client.currentScreen instanceof HudEditorScreen) return;

        if (!GameState.Server.isSkyblock()) return;

        // ボスのネームプレート(ワールド座標をスクリーンへ投影)
        // Glow(ポストエフェクト)より確実に手前へ出すため、ワールド内テキストではなくHUDとして描画する
        BossNameplateRenderer.render(context, client, tickCounter.getTickProgress(true));

        // 各HUDオブジェクトが自分自身の描き方を知っているので、ただループで呼ぶだけ！
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        for (HudElement element : HudConfig.ELEMENTS) {
            if (element.shouldRender(false)) {
                context.getMatrices().pushMatrix();
                // ★修正: Z軸の 0.0f を削除し、XとYのみを指定
                context.getMatrices().translate((float) element.renderX(screenWidth), (float) element.renderY(screenHeight));
                // ★修正: Z軸の 1.0f を削除し、XとYのみを指定
                context.getMatrices().scale(element.scale, element.scale);

                // 実プレイ中も範囲を測っておく。画面内へ寄せる判定を実表示基準で行うため
                element.beginMeasure();
                element.renderElement(context, false);
                element.endMeasure();
                context.getMatrices().popMatrix();
            }
        }

        // サーバーリブート警告は画面中央固定のため特別にここに残す
        if (ModConfig.INSTANCE.misc.enableRebootAlert && GameState.Server.isClosing && GameState.Server.closingTime != null) {
            renderServerClosingAlert(context, client, client.textRenderer);
        }
    }

    private static void renderServerClosingAlert(DrawContext context, MinecraftClient client, TextRenderer tr) {
        String text = "Server closing: " + GameState.Server.closingTime;
        context.getMatrices().pushMatrix();

        // ★修正: Z軸の指定を削除
        context.getMatrices().translate(client.getWindow().getScaledWidth() / 2f, client.getWindow().getScaledHeight() / 2f);
        context.getMatrices().scale(2.0f, 2.0f);

        context.drawTextWithShadow(tr, text, -tr.getWidth(text) / 2, -tr.fontHeight / 2, 0xFFFF5555);
        context.getMatrices().popMatrix();
    }
}