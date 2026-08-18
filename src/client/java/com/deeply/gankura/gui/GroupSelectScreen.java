package com.deeply.gankura.gui;

import com.deeply.gankura.waypoint.Waypoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        super(Component.literal("Move to Group"));
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

            Button button = Button.builder(label(group), b -> {
                onSelect.accept(group);
                onClose();
            }).bounds(centerX - BUTTON_WIDTH / 2, listTop + row * ROW_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            // 今いるグループは選び直しても意味がないので押せなくする
            button.active = !group.equals(current);
            addRenderableWidget(button);
        }

        if (pageCount > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page = (page - 1 + pageCount) % pageCount;
                rebuildWidgets();
            }).bounds(centerX - BUTTON_WIDTH / 2 - 24, listTop, 20, BUTTON_HEIGHT).build());

            addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page = (page + 1) % pageCount;
                rebuildWidgets();
            }).bounds(centerX + BUTTON_WIDTH / 2 + 4, listTop, 20, BUTTON_HEIGHT).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(centerX - 50, height - 32, 100, BUTTON_HEIGHT).build());
    }

    private static Component label(String group) {
        return Component.literal(group.equals(Waypoint.DEFAULT_GROUP) ? "Default" : group);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
