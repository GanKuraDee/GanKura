package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.mixin.ContainerScreenAccessor;
import com.deeply.gankura.util.PanelBox;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 来客の画面の横に、欲しがっている品を並べる。
 *
 * 品の名前は説明文の中にしか無く、手元に無いものは Bazaar で買うことになるので、
 * 名前を押したらそのまま Bazaar で引けるようにしてある。
 * 何が求められているかは {@link VisitorHandler} が読む
 */
public final class VisitorPanel {

    private static final String TITLE = "Wanted";
    private static final String SEARCH_HINT = "Click to search the Bazaar";

    private static final int TITLE_COLOR = 0xFF404040;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int COUNT_COLOR = 0xFF006B6B;
    // 名前に乗せたときの下敷き。押せることが分かるよう、スロットと同じ明るさで敷く
    private static final int HOVER_COLOR = 0x80FFFFFF;

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int COLUMN_GAP = 8;
    // 見出しの行だけは、次の行との間を少し空ける
    private static final int HEADER_SPACE = 3;
    // 収納画面との間隔
    private static final int MARGIN = 4;
    private static final int SCREEN_EDGE = 2;

    // 直前に描いた行の場所。押されたかどうかを見るために控えておく
    private static List<VisitorHandler.Required> shown = List.of();
    private static int nameLeft;
    private static int nameRight;
    private static int rowsTop;
    private static boolean drawn = false;

    private VisitorPanel() {
    }

    /** 名前を押したら、その品を Bazaar で探す */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> !clicked(event.x(), event.y()));
        });
    }

    private static boolean clicked(double mouseX, double mouseY) {
        if (!drawn) return false;

        int row = hoveredRow((int) mouseX, (int) mouseY, shown.size());
        if (row < 0) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        client.player.connection.sendCommand("bz " + shown.get(row).name());
        return true;
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int mouseX, int mouseY) {
        drawn = false;

        if (!ModConfig.INSTANCE.farming.garden.showVisitorItems) return;
        if (!GameState.Server.isGarden()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        VisitorHandler.Offer offer = VisitorHandler.offer(client.player.containerMenu);
        if (offer == null) return;

        draw(screen, graphics, offer.required(), mouseX, mouseY);
    }

    private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                             List<VisitorHandler.Required> required, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;

        List<String> counts = new ArrayList<>();
        for (VisitorHandler.Required item : required) {
            counts.add("x" + String.format(Locale.US, "%,d", item.count()));
        }

        int nameWidth = width(font, required.stream().map(VisitorHandler.Required::name).toList());
        int countWidth = width(font, counts);
        int inner = Math.max(nameWidth + COLUMN_GAP + countWidth, font.width(TITLE));
        int panelWidth = inner + PADDING * 2;
        // 見出しで1行、そのあとが一覧
        int panelHeight = PADDING * 2 + LINE_HEIGHT * (required.size() + 1) + HEADER_SPACE;

        ContainerScreenAccessor box = (ContainerScreenAccessor) screen;
        // 収納画面の右隣。画面から出てしまうときは端に寄せる
        int x = Math.max(Math.min(box.gankura$getLeftPos() + box.gankura$getImageWidth() + MARGIN,
                screen.width - panelWidth - SCREEN_EDGE), SCREEN_EDGE);
        int y = box.gankura$getTopPos();

        PanelBox.draw(graphics, x, y, panelWidth, panelHeight);

        int countRight = x + panelWidth - PADDING;
        int textY = y + PADDING;

        graphics.text(font, TITLE, x + PADDING, textY, TITLE_COLOR, false);
        textY += LINE_HEIGHT + HEADER_SPACE;

        shown = required;
        nameLeft = x + PADDING;
        nameRight = x + PADDING + nameWidth;
        rowsTop = textY;
        drawn = true;

        int hovered = hoveredRow(mouseX, mouseY, required.size());

        for (int i = 0; i < required.size(); i++) {
            if (i == hovered) {
                graphics.fill(nameLeft - 1, textY - 1, nameRight + 1, textY + font.lineHeight, HOVER_COLOR);
            }

            graphics.text(font, required.get(i).name(), nameLeft, textY, NAME_COLOR, false);
            graphics.text(font, counts.get(i), countRight - font.width(counts.get(i)), textY, COUNT_COLOR, false);
            textY += LINE_HEIGHT;
        }

        if (hovered < 0) return;

        // 何が起きるかを添える。箱の外なので、スロットの説明と取り合いにならない
        Component hint = Component.literal(SEARCH_HINT).withStyle(ChatFormatting.GRAY);
        graphics.setTooltipForNextFrame(font, List.of(hint.getVisualOrderText()),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
    }

    /** カーソルが乗っている行。名前の上でなければ -1 */
    private static int hoveredRow(int mouseX, int mouseY, int rows) {
        if (mouseX < nameLeft || mouseX > nameRight || mouseY < rowsTop) return -1;

        int row = (mouseY - rowsTop) / LINE_HEIGHT;
        return row < rows ? row : -1;
    }

    private static int width(Font font, List<String> texts) {
        int widest = 0;
        for (String text : texts) widest = Math.max(widest, font.width(text));
        return widest;
    }
}
