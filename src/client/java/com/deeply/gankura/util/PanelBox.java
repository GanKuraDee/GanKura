package com.deeply.gankura.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 収納画面の隣に置く箱の地。
 *
 * 持ち物の窓と同じ、へこんで見える枠にしてあるので、
 * 何を並べる箱でも並べたときに浮かない
 */
public final class PanelBox {

    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;
    private static final int LIGHT_EDGE_COLOR = 0xFFFFFFFF;
    private static final int DARK_EDGE_COLOR = 0xFF555555;

    private PanelBox() {
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 2, LIGHT_EDGE_COLOR);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 2, LIGHT_EDGE_COLOR);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, DARK_EDGE_COLOR);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, DARK_EDGE_COLOR);
    }
}
