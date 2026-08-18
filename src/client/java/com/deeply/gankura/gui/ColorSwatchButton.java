package com.deeply.gankura.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.IntSupplier;

// 文字の代わりに色そのものを見せるボタン。押すと色選択画面へ移る。
// ButtonWidget には Text という入れ子クラスがあり、素の Text はそちらを指してしまうため、
// このクラスの中だけテキストを完全修飾で書く
public class ColorSwatchButton extends ButtonWidget {

    private final IntSupplier color;
    private final IntSupplier fillAlpha;

    public ColorSwatchButton(int x, int y, int width, int height, IntSupplier color, IntSupplier fillAlpha,
                             Runnable onPress) {
        super(x, y, width, height, net.minecraft.text.Text.empty(), button -> onPress.run(),
                DEFAULT_NARRATION_SUPPLIER);
        this.color = color;
        this.fillAlpha = fillAlpha;
        setTooltip(Tooltip.of(net.minecraft.text.Text.literal("Click to edit the color and opacity")));
    }

    // ボタンの下地は PressableWidget が描いてくれるので、その上へ色だけ重ねる
    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = getX() + 4;
        int top = getY() + 4;
        int right = getRight() - 4;
        int bottom = getBottom() - 4;
        int middle = (left + right) / 2;

        // 左半分が枠線の色、右半分は塗りつぶしの濃さが分かるように暗い下地の上へ重ねる
        context.fill(left, top, middle, bottom, 0xFF000000 | color.getAsInt());
        context.fill(middle, top, right, bottom, 0xFF303030);
        context.fill(middle, top, right, bottom, (fillAlpha.getAsInt() << 24) | color.getAsInt());
    }
}
