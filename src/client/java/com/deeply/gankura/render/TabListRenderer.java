package com.deeply.gankura.render;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.TabListCleaner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * Tab の一覧を、列ごとの幅で描き直す。
 *
 * バニラは一番長い行に合わせた幅を全部の列で使い回し、
 * さらに画面幅に収まらないときは幅を縮めるだけなので、
 * 短い列には余白が残り、長い列は枠から文字がはみ出す。
 * ここでは列ごとに幅を測って、それぞれの中身に合わせる
 */
public final class TabListRenderer {

    // 1行の高さと、行の下に空ける隙間
    private static final int ROW_HEIGHT = 9;
    private static final int ROW_FILL_HEIGHT = 8;
    // 列と列の間
    private static final int COLUMN_GAP = 5;
    // 一覧の上端
    private static final int TOP = 10;

    // 顔と接続状態の目盛りが要る幅
    private static final int HEAD_WIDTH = 9;
    private static final int PING_WIDTH = 13;

    // 一覧全体の後ろに敷く色と、1行ごとに敷く色
    private static final int BACKDROP_COLOR = 0x80000000;
    private static final int ROW_COLOR = 553648127;

    private static final int TEXT_COLOR = -1;

    // 縮めるときに画面の左右へ残す余白
    private static final int SCREEN_MARGIN = 2;
    // これ以上小さくすると読めないので、ここで止める
    private static final float MIN_SCALE = 0.4F;

    // 接続状態の目盛りはバニラに描いてもらう
    @FunctionalInterface
    public interface PingRenderer {
        void render(GuiGraphicsExtractor graphics, int width, int x, int y, PlayerInfo entry);
    }

    private TabListRenderer() {
    }

    /**
     * @return ここで描いたなら true。false のときはバニラに任せる
     */
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, PlayerTabOverlay overlay,
                                 List<PlayerInfo> entries, Component header, Component footer,
                                 PingRenderer ping) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableTabListTweaks || !config.fitTabColumns || entries.isEmpty()) return false;

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        int count = entries.size();
        int rows = count;
        int columns = 1;
        while (rows > PlayerTabOverlay.MAX_ROWS_PER_COL) {
            columns++;
            rows = (count + columns - 1) / columns;
        }

        boolean showPing = !config.hideTabListPing;

        int[] widths = new int[columns];
        boolean[] heads = new boolean[columns];
        measure(overlay, entries, rows, columns, font, config.hideTabListHeads, showPing, widths, heads);

        int totalWidth = -COLUMN_GAP;
        for (int width : widths) totalWidth += width + COLUMN_GAP;

        // 画面からはみ出すときは、収まる大きさまで全体を縮める
        float scale = scaleFor(config, screenWidth, totalWidth);
        Matrix3x2fStack pose = graphics.pose();
        if (scale < 1.0F) {
            pose.pushMatrix();
            pose.scale(scale, scale);
        }
        // 縮めた後の座標で数え直した画面幅。中央に寄せる基準に使う
        int drawWidth = Math.round(screenWidth / scale);

        int left = drawWidth / 2 - totalWidth / 2;
        int top = TOP;

        Component shownHeader = config.hideTabListAds ? null : header;
        Component shownFooter = config.hideTabListAds ? null : footer;

        top = drawLines(graphics, font, shownHeader, drawWidth, totalWidth, top);

        // 一覧全体の下敷き
        graphics.fill(drawWidth / 2 - totalWidth / 2 - 1, top - 1,
                drawWidth / 2 + totalWidth / 2 + 1, top + rows * ROW_HEIGHT, BACKDROP_COLOR);

        drawEntries(graphics, overlay, entries, font, rows, columns, widths, heads,
                left, top, showPing, ping, config.hideTabListHeads);

        drawLines(graphics, font, shownFooter, drawWidth, totalWidth, top + rows * ROW_HEIGHT + 1);

        if (scale < 1.0F) pose.popMatrix();
        return true;
    }

    // 画面に収めるための縮小率。収まっているなら 1
    private static float scaleFor(ModConfig.InterfaceCategory config, int screenWidth, int totalWidth) {
        if (!config.shrinkTabList) return 1.0F;

        int room = screenWidth - SCREEN_MARGIN * 2;
        if (totalWidth <= room) return 1.0F;

        return Math.max(MIN_SCALE, (float) room / totalWidth);
    }

    // 列ごとの幅と、その列に顔が並ぶかどうかを測る
    private static void measure(PlayerTabOverlay overlay, List<PlayerInfo> entries, int rows, int columns,
                                Font font, boolean hideExtraHeads, boolean showPing,
                                int[] widths, boolean[] heads) {
        for (int i = 0; i < entries.size(); i++) {
            int column = i / rows;
            if (column >= columns) break;

            PlayerInfo entry = entries.get(i);
            widths[column] = Math.max(widths[column], font.width(overlay.getNameForDisplay(entry)));
            if (hasHead(overlay, entry, hideExtraHeads)) heads[column] = true;
        }

        for (int column = 0; column < columns; column++) {
            // 顔は列の中で揃えたいので、1つでもあれば列ごと空けておく
            if (heads[column]) widths[column] += HEAD_WIDTH;
            if (showPing) widths[column] += PING_WIDTH;
        }
    }

    private static void drawEntries(GuiGraphicsExtractor graphics, PlayerTabOverlay overlay,
                                    List<PlayerInfo> entries, Font font, int rows, int columns,
                                    int[] widths, boolean[] heads, int left, int top,
                                    boolean showPing, PingRenderer ping, boolean hideExtraHeads) {
        Minecraft client = Minecraft.getInstance();
        int rowColor = client.options.getBackgroundColor(ROW_COLOR);

        for (int i = 0; i < rows * columns; i++) {
            int column = i / rows;
            int row = i % rows;

            int x = left;
            for (int before = 0; before < column; before++) x += widths[before] + COLUMN_GAP;
            int y = top + row * ROW_HEIGHT;

            graphics.fill(x, y, x + widths[column], y + ROW_FILL_HEIGHT, rowColor);
            if (i >= entries.size()) continue;

            PlayerInfo entry = entries.get(i);
            int textX = x;

            if (heads[column]) {
                if (hasHead(overlay, entry, hideExtraHeads)) {
                    PlayerFaceExtractor.extractRenderState(graphics, entry.getSkin().body().texturePath(),
                            x, y, 8, entry.showHat(), false, -1);
                }
                textX += HEAD_WIDTH;
            }

            graphics.text(font, overlay.getNameForDisplay(entry), textX, y, TEXT_COLOR);
            if (showPing) ping.render(graphics, widths[column], x, y, entry);
        }
    }

    // 顔を出す行か。隠す設定を切っているなら、どの行にも出す
    private static boolean hasHead(PlayerTabOverlay overlay, PlayerInfo entry, boolean hideExtraHeads) {
        return !hideExtraHeads || TabListCleaner.hasHead(overlay, entry);
    }

    // 上下の宣伝文。無いときは何もせず、次に描く高さだけ返す
    private static int drawLines(GuiGraphicsExtractor graphics, Font font, Component text,
                                 int screenWidth, int totalWidth, int top) {
        if (text == null) return top;

        List<FormattedCharSequence> lines = font.split(text, screenWidth - 50);
        if (lines.isEmpty()) return top;

        int height = lines.size() * ROW_HEIGHT;
        graphics.fill(screenWidth / 2 - totalWidth / 2 - 1, top - 1,
                screenWidth / 2 + totalWidth / 2 + 1, top + height, BACKDROP_COLOR);

        int y = top;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, screenWidth / 2 - font.width(line) / 2, y, TEXT_COLOR);
            y += ROW_HEIGHT;
        }
        return y + 1;
    }
}
