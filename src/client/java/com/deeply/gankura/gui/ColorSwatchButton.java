package com.deeply.gankura.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

// 文字の代わりに色そのものを見せるボタン。押すと色選択画面へ移る
public class ColorSwatchButton extends Button {

    private final IntSupplier color;
    private final IntSupplier fillAlpha;

    public ColorSwatchButton(int x, int y, int width, int height, IntSupplier color, IntSupplier fillAlpha,
                             Runnable onPress) {
        super(x, y, width, height, Component.empty(), button -> onPress.run(), DEFAULT_NARRATION);
        this.color = color;
        this.fillAlpha = fillAlpha;
        setTooltip(Tooltip.create(Component.literal("Click to edit the color and opacity")));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);

        int left = getX() + 4;
        int top = getY() + 4;
        int right = getRight() - 4;
        int bottom = getBottom() - 4;
        int middle = (left + right) / 2;

        // 左半分が枠線の色、右半分は塗りつぶしの濃さが分かるように暗い下地の上へ重ねる
        graphics.fill(left, top, middle, bottom, 0xFF000000 | color.getAsInt());
        graphics.fill(middle, top, right, bottom, 0xFF303030);
        graphics.fill(middle, top, right, bottom, (fillAlpha.getAsInt() << 24) | color.getAsInt());
    }
}
