package com.deeply.gankura.render;

import com.deeply.gankura.data.HudConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {

    private HudElement draggingElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen() {
        super(Text.literal("GanKura HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Reset to Default"),
                button -> HudConfig.resetToDefault()
        ).dimensions(this.width / 2 - 75, this.height - 30, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // ★魔法のようなループ処理: 1回のループで全てのHUDの当たり判定・枠・中身を描画！
        for (HudElement element : HudConfig.ELEMENTS) {
            if (!element.isEnabled()) continue; // 設定でOFFのものはエディタにも出さない

            boolean isHovering = element.isHovering(mouseX, mouseY);
            boolean isDraggingThis = (draggingElement == element);
            int boxColor = (isHovering || isDraggingThis) ? 0x80FFFFFF : 0x40000000;

            // 当たり判定の枠を描画
            int scaledW = (int)(element.width * element.scale);
            int scaledH = (int)(element.height * element.scale);
            context.fill(element.x - 5, element.y - 5, element.x + scaledW, element.y + scaledH, boxColor);

            // HUDの中身を描画
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) element.x, (float) element.y);
            context.getMatrices().scale(element.scale, element.scale);
            element.renderElement(context, true); // true = プレビューモード
            context.getMatrices().popMatrix();
        }

        context.drawCenteredTextWithShadow(textRenderer, "Drag to move HUDs. Scroll to Resize. Press ESC to save & exit.", width / 2, 20, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) { // 0 = 左クリック
            for (HudElement element : HudConfig.ELEMENTS) {
                if (element.isEnabled() && element.isHovering(mouseX, mouseY)) {
                    draggingElement = element;
                    dragOffsetX = (int)mouseX - element.x;
                    dragOffsetY = (int)mouseY - element.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingElement != null) {
            draggingElement = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (draggingElement != null) {
            draggingElement.x = (int)mouseX - dragOffsetX;
            draggingElement.y = (int)mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scroll = (float) verticalAmount * 0.1f;

        for (HudElement element : HudConfig.ELEMENTS) {
            if (element.isEnabled() && element.isHovering(mouseX, mouseY)) {
                element.scale = Math.max(0.5f, Math.min(3.0f, element.scale + scroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        HudConfig.save();
        super.close();
    }
}