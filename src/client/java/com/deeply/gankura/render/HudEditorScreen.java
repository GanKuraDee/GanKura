package com.deeply.gankura.render;

import com.deeply.gankura.data.HudConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {

    private boolean draggingStats = false;
    private boolean draggingTracker = false;
    private boolean draggingHealth = false;
    private boolean draggingPet = false;
    private boolean draggingArmorStack = false;

    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // 当たり判定のベースサイズ
    private final int STATS_WIDTH = 150;
    private final int STATS_HEIGHT = 50;
    private final int TRACKER_WIDTH = 150;
    private final int TRACKER_HEIGHT = 50;
    private final int HEALTH_WIDTH = 100;
    private final int HEALTH_HEIGHT = 30;
    private final int PET_WIDTH = 120;
    private final int PET_HEIGHT = 30;
    private final int ARMOR_STACK_WIDTH = 150;
    private final int ARMOR_STACK_HEIGHT = 15;

    public HudEditorScreen() {
        super(Text.literal("GanKura HUD Editor"));
    }

    // ★追加: 画面を開いた時にボタンを配置する
    @Override
    protected void init() {
        super.init();

        // 画面の下部中央に「Reset to Default」ボタンを配置
        this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("Reset to Default"),
                button -> HudConfig.resetToDefault() // ボタンを押したらリセットメソッドを呼ぶ
        ).dimensions(this.width / 2 - 75, this.height - 30, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // --- Golem Status ---
        boolean hoverStats = isHovering(mouseX, mouseY, HudConfig.statsX, HudConfig.statsY, STATS_WIDTH, STATS_HEIGHT, HudConfig.statsScale);
        int statsBoxColor = (hoverStats || draggingStats) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.statsX - 5, HudConfig.statsY - 5, HudConfig.statsX + (int)(STATS_WIDTH * HudConfig.statsScale), HudConfig.statsY + (int)(STATS_HEIGHT * HudConfig.statsScale), statsBoxColor);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) HudConfig.statsX, (float) HudConfig.statsY);
        context.getMatrices().scale(HudConfig.statsScale, HudConfig.statsScale);
        HudRenderer.renderStats(context, textRenderer, 0, 0, true);
        context.getMatrices().popMatrix();

        // --- Loot Tracker ---
        boolean hoverTracker = isHovering(mouseX, mouseY, HudConfig.trackerX, HudConfig.trackerY, TRACKER_WIDTH, TRACKER_HEIGHT, HudConfig.trackerScale);
        int trackerBoxColor = (hoverTracker || draggingTracker) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.trackerX - 5, HudConfig.trackerY - 5, HudConfig.trackerX + (int)(TRACKER_WIDTH * HudConfig.trackerScale), HudConfig.trackerY + (int)(TRACKER_HEIGHT * HudConfig.trackerScale), trackerBoxColor);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) HudConfig.trackerX, (float) HudConfig.trackerY);
        context.getMatrices().scale(HudConfig.trackerScale, HudConfig.trackerScale);
        HudRenderer.renderTracker(context, textRenderer, 0, 0);
        context.getMatrices().popMatrix();

        // --- Golem Health ---
        boolean hoverHealth = isHovering(mouseX, mouseY, HudConfig.healthX, HudConfig.healthY, HEALTH_WIDTH, HEALTH_HEIGHT, HudConfig.healthScale);
        int healthBoxColor = (hoverHealth || draggingHealth) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.healthX - 5, HudConfig.healthY - 5, HudConfig.healthX + (int)(HEALTH_WIDTH * HudConfig.healthScale), HudConfig.healthY + (int)(HEALTH_HEIGHT * HudConfig.healthScale), healthBoxColor);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) HudConfig.healthX, (float) HudConfig.healthY);
        context.getMatrices().scale(HudConfig.healthScale, HudConfig.healthScale);
        HudRenderer.renderHealth(context, textRenderer, 0, 0, true);
        context.getMatrices().popMatrix();

        // --- Active Pet ---
        boolean hoverPet = isHovering(mouseX, mouseY, HudConfig.petX, HudConfig.petY, PET_WIDTH, PET_HEIGHT, HudConfig.petScale);
        int petBoxColor = (hoverPet || draggingPet) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.petX - 5, HudConfig.petY - 5, HudConfig.petX + (int)(PET_WIDTH * HudConfig.petScale), HudConfig.petY + (int)(PET_HEIGHT * HudConfig.petScale), petBoxColor);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) HudConfig.petX, (float) HudConfig.petY);
        context.getMatrices().scale(HudConfig.petScale, HudConfig.petScale);
        HudRenderer.renderPetHud(context, textRenderer, 0, 0, true);
        context.getMatrices().popMatrix();

        // --- Armor Stack ---
        boolean hoverArmorStack = isHovering(mouseX, mouseY, HudConfig.armorStackX, HudConfig.armorStackY, ARMOR_STACK_WIDTH, ARMOR_STACK_HEIGHT, HudConfig.armorStackScale);
        int armorStackColor = (hoverArmorStack || draggingArmorStack) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.armorStackX - 5, HudConfig.armorStackY - 5, HudConfig.armorStackX + (int)(ARMOR_STACK_WIDTH * HudConfig.armorStackScale), HudConfig.armorStackY + (int)(ARMOR_STACK_HEIGHT * HudConfig.armorStackScale), armorStackColor);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) HudConfig.armorStackX, (float) HudConfig.armorStackY);
        context.getMatrices().scale(HudConfig.armorStackScale, HudConfig.armorStackScale);
        HudRenderer.renderArmorStackHud(context, textRenderer, 0, 0, true);
        context.getMatrices().popMatrix();

        // ★修正: 説明テキストからRキーの記述を削除
        context.drawCenteredTextWithShadow(textRenderer, "Drag to move HUDs. Scroll to Resize. Press ESC to save & exit.", width / 2, 20, 0xFFFFFFFF);

        // ★追加: 配置したボタンを描画するために必要 (ここはそのまま残します)
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.statsX, HudConfig.statsY, STATS_WIDTH, STATS_HEIGHT, HudConfig.statsScale)) {
                draggingStats = true;
                dragOffsetX = (int)mouseX - HudConfig.statsX;
                dragOffsetY = (int)mouseY - HudConfig.statsY;
                return true;
            }
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.trackerX, HudConfig.trackerY, TRACKER_WIDTH, TRACKER_HEIGHT, HudConfig.trackerScale)) {
                draggingTracker = true;
                dragOffsetX = (int)mouseX - HudConfig.trackerX;
                dragOffsetY = (int)mouseY - HudConfig.trackerY;
                return true;
            }
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.healthX, HudConfig.healthY, HEALTH_WIDTH, HEALTH_HEIGHT, HudConfig.healthScale)) {
                draggingHealth = true;
                dragOffsetX = (int)mouseX - HudConfig.healthX;
                dragOffsetY = (int)mouseY - HudConfig.healthY;
                return true;
            }
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.petX, HudConfig.petY, PET_WIDTH, PET_HEIGHT, HudConfig.petScale)) {
                draggingPet = true;
                dragOffsetX = (int)mouseX - HudConfig.petX;
                dragOffsetY = (int)mouseY - HudConfig.petY;
                return true;
            }
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.armorStackX, HudConfig.armorStackY, ARMOR_STACK_WIDTH, ARMOR_STACK_HEIGHT, HudConfig.armorStackScale)) {
                draggingArmorStack = true;
                dragOffsetX = (int)mouseX - HudConfig.armorStackX;
                dragOffsetY = (int)mouseY - HudConfig.armorStackY;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = draggingStats || draggingTracker || draggingHealth || draggingPet || draggingArmorStack;
        draggingStats = false;
        draggingTracker = false;
        draggingHealth = false;
        draggingPet = false;
        draggingArmorStack = false;
        return wasDragging;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (draggingStats) {
            HudConfig.statsX = (int)mouseX - dragOffsetX;
            HudConfig.statsY = (int)mouseY - dragOffsetY;
            return true;
        }
        if (draggingTracker) {
            HudConfig.trackerX = (int)mouseX - dragOffsetX;
            HudConfig.trackerY = (int)mouseY - dragOffsetY;
            return true;
        }
        if (draggingHealth) {
            HudConfig.healthX = (int)mouseX - dragOffsetX;
            HudConfig.healthY = (int)mouseY - dragOffsetY;
            return true;
        }
        if (draggingPet) {
            HudConfig.petX = (int)mouseX - dragOffsetX;
            HudConfig.petY = (int)mouseY - dragOffsetY;
            return true;
        }
        if (draggingArmorStack) {
            HudConfig.armorStackX = (int)mouseX - dragOffsetX;
            HudConfig.armorStackY = (int)mouseY - dragOffsetY;
            return true;
        }
        return false;
    }

    // ★追加: マウスホイールでのリサイズ機能
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // ホイール1回につき0.1倍ずつ変化
        float scroll = (float) verticalAmount * 0.1f;

        // カーソルが合っているHUDのスケールを 0.5倍 ～ 3.0倍 の範囲で増減させる
        if (isHovering((int)mouseX, (int)mouseY, HudConfig.statsX, HudConfig.statsY, STATS_WIDTH, STATS_HEIGHT, HudConfig.statsScale)) {
            HudConfig.statsScale = Math.max(0.5f, Math.min(3.0f, HudConfig.statsScale + scroll));
            return true;
        }
        if (isHovering((int)mouseX, (int)mouseY, HudConfig.trackerX, HudConfig.trackerY, TRACKER_WIDTH, TRACKER_HEIGHT, HudConfig.trackerScale)) {
            HudConfig.trackerScale = Math.max(0.5f, Math.min(3.0f, HudConfig.trackerScale + scroll));
            return true;
        }
        if (isHovering((int)mouseX, (int)mouseY, HudConfig.healthX, HudConfig.healthY, HEALTH_WIDTH, HEALTH_HEIGHT, HudConfig.healthScale)) {
            HudConfig.healthScale = Math.max(0.5f, Math.min(3.0f, HudConfig.healthScale + scroll));
            return true;
        }
        if (isHovering((int)mouseX, (int)mouseY, HudConfig.petX, HudConfig.petY, PET_WIDTH, PET_HEIGHT, HudConfig.petScale)) {
            HudConfig.petScale = Math.max(0.5f, Math.min(3.0f, HudConfig.petScale + scroll));
            return true;
        }
        if (isHovering((int)mouseX, (int)mouseY, HudConfig.armorStackX, HudConfig.armorStackY, ARMOR_STACK_WIDTH, ARMOR_STACK_HEIGHT, HudConfig.armorStackScale)) {
            HudConfig.armorStackScale = Math.max(0.5f, Math.min(3.0f, HudConfig.armorStackScale + scroll));
            return true;
        }

        return false;
    }

    @Override
    public void close() {
        HudConfig.save();
        super.close();
    }

    // ★変更: スケール(倍率)も考慮して当たり判定を計算するように修正
    private boolean isHovering(int mouseX, int mouseY, int x, int y, int w, int h, float scale) {
        int scaledW = (int)(w * scale);
        int scaledH = (int)(h * scale);
        return mouseX >= x - 5 && mouseX <= x + scaledW && mouseY >= y - 5 && mouseY <= y + scaledH;
    }
}