package com.deeply.gankura.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import com.deeply.gankura.data.HudCategory;

import java.util.function.Supplier;

public abstract class HudElement {
    public final String id;
    // 画面左上からの座標
    public int x, y;
    public float scale;
    public final int defaultX, defaultY;
    public final float defaultScale;
    public int width, height;
    // どの場面で出るHUDか。移動画面の絞り込みに使う。登録時に HudConfig が設定する
    public HudCategory category = HudCategory.GENERAL;

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

    public int scaledWidth() {
        return (int)(width * scale);
    }

    public int scaledHeight() {
        return (int)(height * scale);
    }

    // --- プレビュー描画の実寸計測 ---
    // width/height は表示内容が変わっても揺れないよう固定値にしてあるが、
    // その値は実際の文字列より大きめ。エディタの選択範囲だけは実際に描いた文字の範囲に合わせたいので、
    // プレビュー描画中に text(...) が描いた右端・下端を記録しておく。
    // アンカーの基準計算には固定値のまま width/height を使う(計測前後で配置が動かないようにするため)
    // 左端が0とは限らない(Armor Stack HUD のように幅の中央へ寄せて描くものがある)ため、
    // 右下だけでなく左上も記録する
    private int measuredLeft = Integer.MAX_VALUE;
    private int measuredTop = Integer.MAX_VALUE;
    private int measuredRight = Integer.MIN_VALUE;
    private int measuredBottom = Integer.MIN_VALUE;
    private boolean measuring = false;

    public void beginMeasure() {
        measuring = true;
        measuredLeft = Integer.MAX_VALUE;
        measuredTop = Integer.MAX_VALUE;
        measuredRight = Integer.MIN_VALUE;
        measuredBottom = Integer.MIN_VALUE;
    }

    public void endMeasure() {
        measuring = false;
    }

    private boolean hasMeasured() {
        return measuredRight > measuredLeft && measuredBottom > measuredTop;
    }

    // 選択範囲の左上。HUDの原点から描画内容までのズレぶん
    public int hitOffsetX() {
        return hasMeasured() ? (int)(measuredLeft * scale) : 0;
    }

    public int hitOffsetY() {
        return hasMeasured() ? (int)(measuredTop * scale) : 0;
    }

    // 選択範囲(当たり判定・エディタの枠)のサイズ。計測できていればそれを使う
    public int hitWidth() {
        return (int)((hasMeasured() ? measuredRight - measuredLeft : width) * scale);
    }

    public int hitHeight() {
        return (int)((hasMeasured() ? measuredBottom - measuredTop : height) * scale);
    }

    // 各HUDはこのメソッド経由で文字を描く。描画と同時に範囲を記録する
    protected void drawTextWithShadow(DrawContext context, TextRenderer font, String str, int x, int y, int color) {
        context.drawTextWithShadow(font, str, x, y, color);
        if (!measuring) return;
        measuredLeft = Math.min(measuredLeft, x);
        measuredTop = Math.min(measuredTop, y);
        measuredRight = Math.max(measuredRight, x + font.getWidth(str));
        measuredBottom = Math.max(measuredBottom, y + font.fontHeight);
    }

    // 実際の描画位置。GUIスケール変更などで画面が縮んだとき、
    // 保存値は書き換えずに描画時だけ画面内へ寄せる(画面が広がれば元の配置に戻る)
    public int renderX(int screenWidth) {
        return clampX(x, screenWidth);
    }

    public int renderY(int screenHeight) {
        return clampY(y, screenHeight);
    }

    // 画面内に収める判定は宣言サイズではなく「実際に描かれている範囲」で行う。
    // 宣言サイズは実表示より大きめなので、それを基準にするとHUDごとに
    // 移動できる範囲が変わり、画面端まで寄せられなくなる
    public int clampX(int pos, int screenWidth) {
        return clamp(pos, hitOffsetX(), hitWidth(), screenWidth);
    }

    public int clampY(int pos, int screenHeight) {
        return clamp(pos, hitOffsetY(), hitHeight(), screenHeight);
    }

    private static int clamp(int pos, int contentOffset, int contentSize, int screenSize) {
        // pos はHUDの原点。画面内に収めたいのは原点ではなく描画内容なので、
        // 内容までのズレ(contentOffset)を差し引いた範囲で丸める
        int min = -contentOffset;
        int max = screenSize - contentOffset - contentSize;
        // 内容が画面より大きい場合は端に合わせる以外にないので左上へ寄せる
        if (max < min) return min;
        return Math.max(min, Math.min(pos, max));
    }

    // マウスカーソルが重なっているか (当たり判定)。描画位置と一致させるため画面サイズを渡す
    public boolean isHovering(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int boxX = renderX(screenWidth) + hitOffsetX();
        int boxY = renderY(screenHeight) + hitOffsetY();
        return mouseX >= boxX && mouseX <= boxX + hitWidth()
                && mouseY >= boxY && mouseY <= boxY + hitHeight();
    }

    // 具体的な描画処理 (中身は各HUDで定義する)
    public abstract void renderElement(DrawContext context, boolean isPreview);
}
