package com.deeply.gankura.render;

import com.deeply.gankura.data.HudConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
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

        // Minecraft 26.1.x では Screen に mouseScrolled がないため Fabric API で登録する
        ScreenMouseEvents.beforeMouseScroll(this).register((screen, mouseX, mouseY, h, v) -> {
            float scroll = (float) v * 0.1f;
            for (HudElement element : HudConfig.ELEMENTS) {
                if (element.isEnabled() && element.isHovering(mouseX, mouseY, this.width, this.height)) {
                    element.scale = Math.max(0.5f, Math.min(3.0f, element.scale + scroll));
                    break;
                }
            }
        });
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

            boolean isHovering = element.isHovering(mouseX, mouseY, this.width, this.height);
            boolean isDraggingThis = (draggingElement == element);
            int boxColor = (isHovering || isDraggingThis) ? 0x80FFFFFF : 0x40000000;

            int drawX = element.renderX(this.width);
            int drawY = element.renderY(this.height);

            // 枠は直前フレームの計測結果を使う(初回だけ固定サイズ)
            int boxX = drawX + element.hitOffsetX();
            int boxY = drawY + element.hitOffsetY();
            graphics.fill(boxX, boxY, boxX + element.hitWidth(), boxY + element.hitHeight(), boxColor);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) drawX, (float) drawY);
            graphics.pose().scale(element.scale, element.scale);

            // プレビューは固定文字列なので、ここで測った範囲がそのまま選択範囲になる
            element.beginMeasure();
            element.renderElement(graphics, true);
            element.endMeasure();

            graphics.pose().popMatrix();
        }

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
                if (element.isEnabled() && element.isHovering(mouseX, mouseY, this.width, this.height)) {
                    draggingElement = element;
                    // 画面外に出ていたHUDは寄せた位置から掴めるよう、描画位置を基準にオフセットを取る
                    dragOffsetX = (int)mouseX - element.renderX(this.width);
                    dragOffsetY = (int)mouseY - element.renderY(this.height);
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
            // ドラッグ中も画面内に収める
            draggingElement.x = draggingElement.clampX((int)event.x() - dragOffsetX, this.width);
            draggingElement.y = draggingElement.clampY((int)event.y() - dragOffsetY, this.height);
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