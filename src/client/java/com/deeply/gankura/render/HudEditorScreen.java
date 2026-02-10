package com.deeply.gankura.render;

import com.deeply.gankura.data.HudConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {

    // ドラッグ状態
    private boolean draggingStats = false;
    private boolean draggingTracker = false;
    // ★追加
    private boolean draggingHealth = false;

    // ドラッグ開始時のオフセット
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // 当たり判定サイズ (概算)
    private final int STATS_WIDTH = 150;
    private final int STATS_HEIGHT = 50;
    private final int TRACKER_WIDTH = 150;
    private final int TRACKER_HEIGHT = 50;
    // ★追加
    private final int HEALTH_WIDTH = 100;
    private final int HEALTH_HEIGHT = 30;

    public HudEditorScreen() {
        super(Text.literal("GanKura HUD Editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // --- Golem Status ---
        boolean hoverStats = isHovering(mouseX, mouseY, HudConfig.statsX, HudConfig.statsY, STATS_WIDTH, STATS_HEIGHT);
        int statsBoxColor = (hoverStats || draggingStats) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.statsX - 5, HudConfig.statsY - 5, HudConfig.statsX + STATS_WIDTH, HudConfig.statsY + STATS_HEIGHT, statsBoxColor);

        // プレビュー描画
        HudRenderer.renderStats(context, textRenderer, HudConfig.statsX, HudConfig.statsY, true);

        // --- Loot Tracker ---
        boolean hoverTracker = isHovering(mouseX, mouseY, HudConfig.trackerX, HudConfig.trackerY, TRACKER_WIDTH, TRACKER_HEIGHT);
        int trackerBoxColor = (hoverTracker || draggingTracker) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.trackerX - 5, HudConfig.trackerY - 5, HudConfig.trackerX + TRACKER_WIDTH, HudConfig.trackerY + TRACKER_HEIGHT, trackerBoxColor);

        // 描画
        HudRenderer.renderTracker(context, textRenderer, HudConfig.trackerX, HudConfig.trackerY);

        // --- ★追加: Golem Health ---
        boolean hoverHealth = isHovering(mouseX, mouseY, HudConfig.healthX, HudConfig.healthY, HEALTH_WIDTH, HEALTH_HEIGHT);
        int healthBoxColor = (hoverHealth || draggingHealth) ? 0x80FFFFFF : 0x40000000;
        context.fill(HudConfig.healthX - 5, HudConfig.healthY - 5, HudConfig.healthX + HEALTH_WIDTH, HudConfig.healthY + HEALTH_HEIGHT, healthBoxColor);

        HudRenderer.renderHealth(context, textRenderer, HudConfig.healthX, HudConfig.healthY, true);

        // 説明
        context.drawCenteredTextWithShadow(textRenderer, "Drag to move HUDs. Press ESC to save & exit.", width / 2, 20, 0xFFFFFF);
    }

// ... (前略)

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // ★重要: click変数から座標とボタンを取得してください
        // エラーが出る場合、click.getMouseX() や click.x() などを試してください
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button(); // 0:左, 1:右

        if (button == 0) { // 左クリック
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.statsX, HudConfig.statsY, STATS_WIDTH, STATS_HEIGHT)) {
                draggingStats = true;
                dragOffsetX = (int)mouseX - HudConfig.statsX;
                dragOffsetY = (int)mouseY - HudConfig.statsY;
                return true;
            }
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.trackerX, HudConfig.trackerY, TRACKER_WIDTH, TRACKER_HEIGHT)) {
                draggingTracker = true;
                dragOffsetX = (int)mouseX - HudConfig.trackerX;
                dragOffsetY = (int)mouseY - HudConfig.trackerY;
                return true;
            }
            // ★追加
            if (isHovering((int)mouseX, (int)mouseY, HudConfig.healthX, HudConfig.healthY, HEALTH_WIDTH, HEALTH_HEIGHT)) {
                draggingHealth = true;
                dragOffsetX = (int)mouseX - HudConfig.healthX;
                dragOffsetY = (int)mouseY - HudConfig.healthY;
                return true;
            }
        }
        return false; // superは削除
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = draggingStats || draggingTracker;
        draggingStats = false;
        draggingTracker = false;
        draggingHealth = false; // ★追加
        return wasDragging;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        // ★重要: ここも同様に座標を取得してください
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
        return false;
    }


    @Override
    public void close() {
        HudConfig.save(); // 保存して閉じる
        super.close();
    }

    private boolean isHovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x - 5 && mouseX <= x + w && mouseY >= y - 5 && mouseY <= y + h;
    }
}