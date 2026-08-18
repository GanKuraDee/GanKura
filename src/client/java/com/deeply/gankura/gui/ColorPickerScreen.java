package com.deeply.gankura.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

// 色を目で見て選ぶ画面。彩度/明度の面と色相のバー、色見本、塗りつぶしの濃さのスライダー
public class ColorPickerScreen extends Screen {

    // 決定したときに色を受け取る相手
    @FunctionalInterface
    public interface Result {
        void apply(int rgb, int fillAlpha);
    }

    private static final int MAX_FIELD_WIDTH = 176;
    private static final int MAX_FIELD_HEIGHT = 120;
    private static final int MIN_FIELD_HEIGHT = 40;
    private static final int HUE_WIDTH = 20;
    private static final int SWATCH_SIZE = 20;
    private static final int SWATCH_GAP = 2;
    private static final int WIDGET_HEIGHT = 20;
    private static final int LABEL_COLOR = 0xFFA0A0A0;
    private static final int INVALID_COLOR = 0xFF5555;
    private static final int MARKER_COLOR = 0xFFFFFFFF;
    private static final int MARKER_SHADOW = 0xFF000000;

    private final Screen parent;
    private final Result result;

    private float hue;
    private float saturation;
    private float brightness;
    private int fillAlpha;

    // ここから下は画面サイズに合わせて init() で決める
    private int fieldX;
    private int fieldY;
    private int fieldWidth;
    private int fieldHeight;
    private int hueX;
    private int swatchesX;
    private int swatchesY;
    private int swatchesPerRow;
    private int previewX;
    private int previewY;
    private int previewWidth;

    private EditBox hexBox;
    private boolean draggingField;
    private boolean draggingHue;
    // 画面側から16進の欄を書き換えている間だけ立てる。入力欄の反応と取り合いにならないようにする
    private boolean updatingHex;

    public ColorPickerScreen(Screen parent, int rgb, int fillAlpha, Result result) {
        super(Component.literal("Pick a color"));
        this.parent = parent;
        this.result = result;
        this.fillAlpha = Math.clamp(fillAlpha, 0, 255);
        setColor(rgb);
    }

    private void setColor(int rgb) {
        float[] hsv = toHsv(rgb);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private int rgb() {
        return Mth.hsvToRgb(hue, saturation, brightness) & 0xFFFFFF;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        // 一番横幅を食う色見本の行が収まる幅を先に決め、他はそれに合わせる
        int contentWidth = Math.min(width - 16, MAX_FIELD_WIDTH + 8 + HUE_WIDTH);
        swatchesPerRow = Math.max(1, (contentWidth + SWATCH_GAP) / (SWATCH_SIZE + SWATCH_GAP));
        int swatchRows = (ColorPalette.colors().size() + swatchesPerRow - 1) / swatchesPerRow;

        fieldX = centerX - contentWidth / 2;
        fieldWidth = contentWidth - 8 - HUE_WIDTH;
        hueX = fieldX + fieldWidth + 8;
        swatchesX = fieldX;

        // 下の行から順に位置を決め、余った高さを彩度/明度の面に回す
        int buttonsY = height - 28;
        swatchesY = buttonsY - 10 - (swatchRows * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP);
        int hexY = swatchesY - 12 - WIDGET_HEIGHT;
        int sliderY = hexY - 6 - WIDGET_HEIGHT;
        fieldY = 26;
        fieldHeight = Math.clamp(sliderY - 6 - fieldY, MIN_FIELD_HEIGHT, MAX_FIELD_HEIGHT);

        addRenderableWidget(new AlphaSlider(fieldX, sliderY, contentWidth));

        int hexWidth = Math.min(80, contentWidth / 2 - 4);
        previewX = fieldX + hexWidth + 8;
        previewY = hexY;
        previewWidth = contentWidth - hexWidth - 8;

        hexBox = new EditBox(font, fieldX, hexY, hexWidth, WIDGET_HEIGHT, Component.literal("Color"));
        hexBox.setMaxLength(7);
        hexBox.setValue(ColorPalette.toHex(rgb()));
        hexBox.setResponder(text -> {
            if (updatingHex) return;

            Integer color = ColorPalette.parse(text);
            if (color == null) {
                hexBox.setTextColor(INVALID_COLOR);
            } else {
                hexBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                setColor(color);
            }
        });
        addRenderableWidget(hexBox);

        int buttonWidth = Math.min(100, contentWidth / 2 - 4);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            result.apply(rgb(), fillAlpha);
            onClose();
        }).bounds(centerX - buttonWidth - 4, buttonsY, buttonWidth, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(centerX + 4, buttonsY, buttonWidth, WIDGET_HEIGHT).build());
    }

    private void refreshHex() {
        if (hexBox != null) {
            updatingHex = true;
            hexBox.setValue(ColorPalette.toHex(rgb()));
            hexBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            updatingHex = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        double mouseX = event.x();
        double mouseY = event.y();

        if (inside(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight)) {
            draggingField = true;
            pickFromField(mouseX, mouseY);
            return true;
        }

        if (inside(mouseX, mouseY, hueX, fieldY, HUE_WIDTH, fieldHeight)) {
            draggingHue = true;
            pickFromHueBar(mouseY);
            return true;
        }

        int swatch = swatchAt(mouseX, mouseY);
        if (swatch >= 0) {
            setColor(ColorPalette.colors().get(swatch));
            refreshHex();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingField) {
            pickFromField(event.x(), event.y());
            return true;
        }

        if (draggingHue) {
            pickFromHueBar(event.y());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingField = false;
        draggingHue = false;
        return super.mouseReleased(event);
    }

    private void pickFromField(double mouseX, double mouseY) {
        saturation = Math.clamp((float) (mouseX - fieldX) / (fieldWidth - 1), 0.0F, 1.0F);
        brightness = 1.0F - Math.clamp((float) (mouseY - fieldY) / (fieldHeight - 1), 0.0F, 1.0F);
        refreshHex();
    }

    private void pickFromHueBar(double mouseY) {
        hue = Math.clamp((float) (mouseY - fieldY) / (fieldHeight - 1), 0.0F, 1.0F);
        refreshHex();
    }

    private int swatchAt(double mouseX, double mouseY) {
        int cell = SWATCH_SIZE + SWATCH_GAP;
        int column = (int) (mouseX - swatchesX) / cell;
        int row = (int) (mouseY - swatchesY) / cell;

        if (mouseX < swatchesX || mouseY < swatchesY || column >= swatchesPerRow) return -1;

        // 見本と見本の隙間を押しても選んだことにはしない
        if ((int) (mouseX - swatchesX) % cell >= SWATCH_SIZE || (int) (mouseY - swatchesY) % cell >= SWATCH_SIZE) {
            return -1;
        }

        int index = row * swatchesPerRow + column;
        return index < ColorPalette.colors().size() ? index : -1;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        graphics.centeredText(font, title, centerX, 12, 0xFFFFFFFF);

        drawSaturationValueField(graphics);
        drawHueBar(graphics);
        drawSwatches(graphics);
        drawPreview(graphics);

        graphics.text(font, Component.literal("Presets"), swatchesX, swatchesY - 11, LABEL_COLOR);
    }

    // 1ピクセル幅の縦グラデーションを横に並べる。左右が彩度、上下が明度
    private void drawSaturationValueField(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < fieldWidth; i++) {
            float columnSaturation = (float) i / (fieldWidth - 1);
            int top = 0xFF000000 | Mth.hsvToRgb(hue, columnSaturation, 1.0F);
            graphics.fillGradient(fieldX + i, fieldY, fieldX + i + 1, fieldY + fieldHeight, top, 0xFF000000);
        }

        graphics.outline(fieldX - 1, fieldY - 1, fieldWidth + 2, fieldHeight + 2, 0xFF000000);

        int markerX = fieldX + Math.round(saturation * (fieldWidth - 1));
        int markerY = fieldY + Math.round((1.0F - brightness) * (fieldHeight - 1));
        graphics.outline(markerX - 4, markerY - 4, 9, 9, MARKER_SHADOW);
        graphics.outline(markerX - 3, markerY - 3, 7, 7, MARKER_COLOR);
    }

    private void drawHueBar(GuiGraphicsExtractor graphics) {
        int segments = 6;

        for (int i = 0; i < segments; i++) {
            int top = 0xFF000000 | Mth.hsvToRgb((float) i / segments, 1.0F, 1.0F);
            int bottom = 0xFF000000 | Mth.hsvToRgb((float) (i + 1) / segments, 1.0F, 1.0F);
            int y0 = fieldY + fieldHeight * i / segments;
            int y1 = fieldY + fieldHeight * (i + 1) / segments;
            graphics.fillGradient(hueX, y0, hueX + HUE_WIDTH, y1, top, bottom);
        }

        graphics.outline(hueX - 1, fieldY - 1, HUE_WIDTH + 2, fieldHeight + 2, 0xFF000000);

        int markerY = fieldY + Math.round(hue * (fieldHeight - 1));
        graphics.fill(hueX - 2, markerY - 1, hueX + HUE_WIDTH + 2, markerY + 2, MARKER_SHADOW);
        graphics.fill(hueX - 1, markerY, hueX + HUE_WIDTH + 1, markerY + 1, MARKER_COLOR);
    }

    private void drawSwatches(GuiGraphicsExtractor graphics) {
        List<Integer> colors = ColorPalette.colors();

        for (int i = 0; i < colors.size(); i++) {
            int x = swatchesX + (i % swatchesPerRow) * (SWATCH_SIZE + SWATCH_GAP);
            int y = swatchesY + (i / swatchesPerRow) * (SWATCH_SIZE + SWATCH_GAP);
            graphics.fill(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, 0xFF000000 | colors.get(i));
            graphics.outline(x, y, SWATCH_SIZE, SWATCH_SIZE, 0xFF000000);
        }
    }

    // 明るい下地と暗い下地の上に重ねて、塗りつぶしの濃さを見比べられるようにする
    private void drawPreview(GuiGraphicsExtractor graphics) {
        int right = previewX + previewWidth;
        int bottom = previewY + WIDGET_HEIGHT;

        graphics.fill(previewX, previewY, previewX + previewWidth / 2, bottom, 0xFFFFFFFF);
        graphics.fill(previewX + previewWidth / 2, previewY, right, bottom, 0xFF303030);
        graphics.fill(previewX, previewY, right, bottom, (fillAlpha << 24) | rgb());
        graphics.outline(previewX, previewY, previewWidth, WIDGET_HEIGHT, 0xFF000000 | rgb());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // RGB から色相・彩度・明度へ。いずれも 0〜1 で返す
    private static float[] toHsv(int rgb) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        float hue = 0.0F;

        if (delta > 0.0F) {
            if (max == red) {
                hue = ((green - blue) / delta) % 6.0F;
            } else if (max == green) {
                hue = (blue - red) / delta + 2.0F;
            } else {
                hue = (red - green) / delta + 4.0F;
            }

            hue /= 6.0F;
            if (hue < 0.0F) hue += 1.0F;
        }

        return new float[]{hue, max == 0.0F ? 0.0F : delta / max, max};
    }

    private class AlphaSlider extends AbstractSliderButton {

        AlphaSlider(int x, int y, int width) {
            super(x, y, width, WIDGET_HEIGHT, Component.empty(), fillAlpha / 255.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Fill opacity: " + Math.round(fillAlpha * 100.0F / 255.0F) + "%"));
        }

        @Override
        protected void applyValue() {
            fillAlpha = (int) Math.round(value * 255.0D);
        }
    }
}
