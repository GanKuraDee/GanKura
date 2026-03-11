package com.deeply.gankura.render;

import net.minecraft.client.gui.DrawContext;
import java.util.function.Supplier;

public abstract class HudElement {
    public final String id;
    public int x, y;
    public float scale;
    public final int defaultX, defaultY;
    public final float defaultScale;
    public final int width, height;

    private final Supplier<Boolean> enabledSupplier;
    private final Supplier<Boolean> visibilityCondition;

    public HudElement(String id, int defaultX, int defaultY, float defaultScale, int width, int height, Supplier<Boolean> enabledSupplier, Supplier<Boolean> visibilityCondition) {
        this.id = id;
        this.defaultX = defaultX; this.defaultY = defaultY; this.defaultScale = defaultScale;
        this.x = defaultX; this.y = defaultY; this.scale = defaultScale;
        this.width = width; this.height = height;
        this.enabledSupplier = enabledSupplier;
        this.visibilityCondition = visibilityCondition;
    }

    // 設定画面でONになっているか
    public boolean isEnabled() {
        return enabledSupplier.get();
    }

    // 実際に画面に描画すべきか (プレビュー時はONなら必ず表示)
    public boolean shouldRender(boolean isPreview) {
        if (isPreview) return isEnabled();
        return isEnabled() && visibilityCondition.get();
    }

    // デフォルト位置にリセット
    public void reset() {
        x = defaultX;
        y = defaultY;
        scale = defaultScale;
    }

    // マウスカーソルが重なっているか (当たり判定)
    public boolean isHovering(double mouseX, double mouseY) {
        int scaledW = (int)(width * scale);
        int scaledH = (int)(height * scale);
        return mouseX >= x - 5 && mouseX <= x + scaledW && mouseY >= y - 5 && mouseY <= y + scaledH;
    }

    // 具体的な描画処理 (中身は各HUDで定義する)
    public abstract void renderElement(DrawContext context, boolean isPreview);
}