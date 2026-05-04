package com.deeply.gankura.render;

import com.deeply.gankura.data.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private HudElement draggingElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen() {
        super(Component.literal("GanKura HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
                Component.literal("Reset to Default"),
                button -> HudConfig.resetToDefault()
        ).bounds(this.width / 2 - 75, this.height - 30, 150, 20).build());
    }

    /**
     * Minecraft 26.1.2 仕様:
     * Screen クラスでは render ではなく extractRenderState をオーバーライドします。
     * 引数: GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 背景の半透明塗りつぶし
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        for (HudElement element : HudConfig.ELEMENTS) {
            if (!element.isEnabled()) continue;

            boolean isHovering = element.isHovering(mouseX, mouseY);
            boolean isDraggingThis = (draggingElement == element);
            int boxColor = (isHovering || isDraggingThis) ? 0x80FFFFFF : 0x40000000;

            int scaledW = (int)(element.width * element.scale);
            int scaledH = (int)(element.height * element.scale);

            graphics.fill(element.x - 5, element.y - 5, element.x + scaledW, element.y + scaledH, boxColor);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) element.x, (float) element.y);
            graphics.pose().scale(element.scale, element.scale);

            element.renderElement(graphics, true);

            graphics.pose().popMatrix();
        }

        graphics.centeredText(this.font, "Drag to move HUDs. Scroll to Resize. Press ESC to save & exit.", this.width / 2, 20, 0xFFFFFFFF);

        // Screen.class L104: super への委譲も extractRenderState を使用
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // MouseButtonEvent.class L10: フィールド名は x() と y() です
        double mouseX = event.x();
        double mouseY = event.y();
        // button() メソッドを使用して int 値を取得
        int button = event.button();

        if (button == 0) {
            for (HudElement element : HudConfig.ELEMENTS) {
                if (element.isEnabled() && element.isHovering(mouseX, mouseY)) {
                    draggingElement = element;
                    dragOffsetX = (int)mouseX - element.x;
                    dragOffsetY = (int)mouseY - element.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingElement != null) {
            draggingElement = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingElement != null) {
            draggingElement.x = (int)event.x() - dragOffsetX;
            draggingElement.y = (int)event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        super.onClose();
    }
}