package com.deeply.gankura.gui;

import com.deeply.gankura.waypoint.Waypoint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

// ウェイポイントの移動先グループを選ぶだけの画面
public class GroupSelectScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final List<String> groups;
    private final String current;
    private final Consumer<String> onSelect;
    private int page;

    public GroupSelectScreen(Screen parent, List<String> groups, String current, Consumer<String> onSelect) {
        super(Text.literal("Move to Group"));
        this.parent = parent;
        this.groups = groups;
        this.current = current;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int listTop = 40;
        int rowsPerPage = Math.max(1, (height - 70 - listTop) / ROW_HEIGHT);
        int pageCount = Math.max(1, (groups.size() + rowsPerPage - 1) / rowsPerPage);
        page = Math.clamp(page, 0, pageCount - 1);

        int firstIndex = page * rowsPerPage;

        for (int row = 0; row < rowsPerPage && firstIndex + row < groups.size(); row++) {
            String group = groups.get(firstIndex + row);

            ButtonWidget button = ButtonWidget.builder(label(group), b -> {
                onSelect.accept(group);
                close();
            }).dimensions(centerX - BUTTON_WIDTH / 2, listTop + row * ROW_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            // 今いるグループは選び直しても意味がないので押せなくする
            button.active = !group.equals(current);
            addDrawableChild(button);
        }

        if (pageCount > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                page = (page - 1 + pageCount) % pageCount;
                clearAndInit();
            }).dimensions(centerX - BUTTON_WIDTH / 2 - 24, listTop, 20, BUTTON_HEIGHT).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                page = (page + 1) % pageCount;
                clearAndInit();
            }).dimensions(centerX + BUTTON_WIDTH / 2 + 4, listTop, 20, BUTTON_HEIGHT).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 50, height - 32, 100, BUTTON_HEIGHT).build());
    }

    private static Text label(String group) {
        return Text.literal(group.equals(Waypoint.DEFAULT_GROUP) ? "Default" : group);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
