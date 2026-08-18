package com.deeply.gankura.gui;

import com.deeply.gankura.waypoint.HighlightStyle;
import com.deeply.gankura.waypoint.Waypoint;
import com.deeply.gankura.waypoint.WaypointData;
import com.deeply.gankura.waypoint.WaypointManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// ウェイポイントの一覧と編集。エリアとグループを選び、その中身を並べる
public class WaypointScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 380;
    private static final int WIDGET_HEIGHT = 20;
    private static final int INVALID_COLOR = 0xFF5555;
    private static final int LABEL_COLOR = 0xFFA0A0A0;

    // 縦の配置。見出しは説明する行の10ピクセル上に置く
    private static final int AREA_ROW_Y = 36;
    private static final int GROUP_ROW_Y = 70;
    private static final int LIST_TOP = 106;

    // 1行の中での横位置
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_X = 102;
    private static final int COLUMN_Y = 140;
    private static final int COLUMN_Z = 178;
    private static final int COLUMN_SWATCH = 216;
    private static final int COLUMN_STYLE = 260;
    private static final int COLUMN_MOVE = 308;
    private static final int COLUMN_TOGGLE = 332;
    private static final int COLUMN_DELETE = 362;

    private final Screen parent;
    private String area;
    private String group = Waypoint.DEFAULT_GROUP;
    private int page;

    private int rowsPerPage = 1;
    private int listTop;

    // グループの行が今何をしているか
    private enum GroupEdit {
        NONE, CREATE, RENAME
    }

    private GroupEdit groupEdit = GroupEdit.NONE;
    private String editedGroupName = "";

    // 重複などで編集を弾いたときにボタンの上へ出す
    private Component status;

    public WaypointScreen(Screen parent) {
        this(parent, WaypointManager.currentArea());
    }

    public WaypointScreen(Screen parent, String area) {
        super(Component.literal("Custom Waypoints"));
        this.parent = parent;
        this.area = area;
    }

    private List<Waypoint> allWaypoints() {
        return WaypointManager.getInstance().waypointsForEditing(area);
    }

    // 一覧に出すのは、選んでいるグループに属するものだけ
    private List<Waypoint> shownWaypoints() {
        return allWaypoints().stream().filter(waypoint -> waypoint.getGroup().equals(group)).toList();
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int left = centerX - ROW_WIDTH / 2;
        listTop = LIST_TOP;
        int listBottom = height - 60;
        rowsPerPage = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);

        List<Waypoint> waypoints = shownWaypoints();
        int pageCount = pageCount(waypoints.size());
        page = Math.clamp(page, 0, pageCount - 1);

        buildAreaRow(left, AREA_ROW_Y);
        buildGroupRow(left, GROUP_ROW_Y);

        int firstIndex = page * rowsPerPage;

        for (int row = 0; row < rowsPerPage && firstIndex + row < waypoints.size(); row++) {
            addRow(waypoints.get(firstIndex + row), left, listTop + row * ROW_HEIGHT);
        }

        if (pageCount > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page = (page - 1 + pageCount) % pageCount;
                rebuildWidgets();
            }).bounds(centerX - 70, height - 52, 20, WIDGET_HEIGHT).build());

            addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page = (page + 1) % pageCount;
                rebuildWidgets();
            }).bounds(centerX + 50, height - 52, 20, WIDGET_HEIGHT).build());
        }

        buildFooter(left, centerX);
    }

    private void buildAreaRow(int left, int y) {
        EditBox areaBox = new EditBox(font, left, y, 194, WIDGET_HEIGHT, Component.literal("Area"));
        areaBox.setMaxLength(64);
        areaBox.setValue(area);
        areaBox.setEditable(false);
        addRenderableWidget(areaBox);

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleArea(-1))
                .bounds(left + 198, y, 20, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleArea(1))
                .bounds(left + 220, y, 20, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Use current area"),
                button -> selectArea(WaypointManager.currentArea())).bounds(left + 244, y, 136, WIDGET_HEIGHT).build());
    }

    // 選べるエリアは、既に登録があるものと今いるエリア
    private List<String> areaChoices() {
        List<String> areas = new ArrayList<>(WaypointManager.getInstance().areaNames());

        for (String candidate : List.of(WaypointManager.currentArea(), area)) {
            if (!areas.contains(candidate)) areas.add(candidate);
        }

        return areas;
    }

    private void cycleArea(int direction) {
        List<String> areas = areaChoices();
        int index = Math.max(0, areas.indexOf(area));
        selectArea(areas.get((index + direction + areas.size()) % areas.size()));
    }

    private void selectArea(String selected) {
        area = selected;
        // グループはエリアごとなので、移った先では選び直しになる
        group = Waypoint.DEFAULT_GROUP;
        page = 0;
        rebuildWidgets();
    }

    private void buildGroupRow(int left, int y) {
        if (groupEdit != GroupEdit.NONE) {
            buildGroupNameRow(left, y);
            return;
        }

        WaypointManager manager = WaypointManager.getInstance();

        EditBox groupBox = new EditBox(font, left, y, 150, WIDGET_HEIGHT, Component.literal("Group"));
        groupBox.setMaxLength(64);
        groupBox.setHint(Component.literal("(default)"));
        groupBox.setValue(group);
        groupBox.setEditable(false);
        addRenderableWidget(groupBox);

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleGroup(-1))
                .bounds(left + 154, y, 20, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleGroup(1))
                .bounds(left + 176, y, 20, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("+"), button -> startGroupEdit(GroupEdit.CREATE))
                .tooltip(Tooltip.create(Component.literal("Add a group")))
                .bounds(left + 200, y, 20, WIDGET_HEIGHT).build());

        Button renameGroup = Button.builder(Component.literal("✎"), button -> startGroupEdit(GroupEdit.RENAME))
                .tooltip(Tooltip.create(Component.literal("Rename this group")))
                .bounds(left + 222, y, 20, WIDGET_HEIGHT).build();
        renameGroup.active = !group.equals(Waypoint.DEFAULT_GROUP);
        addRenderableWidget(renameGroup);

        Button removeGroup = Button.builder(Component.literal("✖").withStyle(ChatFormatting.RED),
                        button -> confirmGroupRemoval())
                .tooltip(Tooltip.create(Component.literal("Remove this group (its waypoints move to the default one)")))
                .bounds(left + 244, y, 20, WIDGET_HEIGHT).build();
        removeGroup.active = !group.equals(Waypoint.DEFAULT_GROUP);
        addRenderableWidget(removeGroup);

        boolean groupEnabled = manager.isGroupEnabled(area, group);

        addRenderableWidget(Button.builder(toggleLabel("Group", groupEnabled), button -> {
            boolean enabled = !manager.isGroupEnabled(area, group);
            manager.setGroupEnabled(area, group, enabled);
            button.setMessage(toggleLabel("Group", enabled));
        }).bounds(left + 268, y, 112, WIDGET_HEIGHT).build());
    }

    // グループを消すと中のウェイポイントも動くので、一度確認する
    private void confirmGroupRemoval() {
        if (minecraft == null) return;

        String removed = group;
        int waypointCount = WaypointManager.getInstance().waypointsOfGroup(area, removed).size();

        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        WaypointManager.getInstance().removeGroup(area, removed);
                        group = Waypoint.DEFAULT_GROUP;
                        page = 0;
                    }

                    minecraft.gui.setScreen(this);
                },
                Component.literal("Remove the group \"" + removed + "\"?"),
                Component.literal("Its " + waypointCount + " waypoints move to the default group.")));
    }

    private void startGroupEdit(GroupEdit mode) {
        groupEdit = mode;
        editedGroupName = mode == GroupEdit.RENAME ? group : "";
        rebuildWidgets();
    }

    // グループ名を打っている間の行。新規作成と名前の変更で共用する
    private void buildGroupNameRow(int left, int y) {
        EditBox nameBox = new EditBox(font, left, y, 268, WIDGET_HEIGHT, groupEditTitle());
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.literal("Enter a group name"));
        nameBox.setValue(editedGroupName);
        nameBox.setResponder(value -> editedGroupName = value);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(
                        Component.literal(groupEdit == GroupEdit.RENAME ? "Rename" : "Create"),
                        button -> confirmGroupEdit())
                .bounds(left + 272, y, 54, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> cancelGroupEdit())
                .bounds(left + 330, y, 50, WIDGET_HEIGHT).build());
    }

    private Component groupEditTitle() {
        return Component.literal(groupEdit == GroupEdit.RENAME ? "Rename this group" : "New group");
    }

    // 打った名前を反映し、そのグループを選んだ状態にする。続けて足すウェイポイントがそこへ入る
    private void confirmGroupEdit() {
        String name = editedGroupName.trim();

        if (!name.isEmpty()) {
            if (groupEdit == GroupEdit.RENAME) {
                WaypointManager.getInstance().renameGroup(area, group, name);
            } else {
                WaypointManager.getInstance().addGroup(area, name);
            }

            group = name;
            page = 0;
        }

        cancelGroupEdit();
    }

    private void cancelGroupEdit() {
        groupEdit = GroupEdit.NONE;
        editedGroupName = "";
        rebuildWidgets();
    }

    private void buildFooter(int left, int centerX) {
        WaypointData data = WaypointManager.getInstance().data();

        addRenderableWidget(Button.builder(toggleLabel("Render", data.enabled), button -> {
            data.enabled = !data.enabled;
            button.setMessage(toggleLabel("Render", data.enabled));
        }).bounds(left, height - 52, 88, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(toggleLabel("Names", data.showNames), button -> {
            data.showNames = !data.showNames;
            button.setMessage(toggleLabel("Names", data.showNames));
        }).bounds(left + ROW_WIDTH - 88, height - 52, 88, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Add at my position"),
                button -> addAtPlayer()).bounds(left, height - 28, 130, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Add empty"),
                        button -> addWaypoint(new Waypoint(defaultName(), 0, 0, 0, Waypoint.DEFAULT_COLOR, group)))
                .bounds(centerX - 45, height - 28, 90, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + ROW_WIDTH - 80, height - 28, 80, WIDGET_HEIGHT).build());
    }

    private void addRow(Waypoint waypoint, int x, int y) {
        EditBox nameBox = new EditBox(font, x + COLUMN_NAME, y, 98, WIDGET_HEIGHT, Component.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(waypoint.getName());
        nameBox.setResponder(waypoint::setName);
        addRenderableWidget(nameBox);

        addRenderableWidget(coordinateBox(x + COLUMN_X, y, waypoint, Axis.X));
        addRenderableWidget(coordinateBox(x + COLUMN_Y, y, waypoint, Axis.Y));
        addRenderableWidget(coordinateBox(x + COLUMN_Z, y, waypoint, Axis.Z));

        addRenderableWidget(new ColorSwatchButton(x + COLUMN_SWATCH, y, 40, WIDGET_HEIGHT,
                waypoint::getColor, waypoint::getFillAlpha, () -> openColorPicker(waypoint)));

        addRenderableWidget(Button.builder(styleLabel(waypoint.getStyle()), button -> {
            waypoint.setStyle(waypoint.getStyle().next());
            button.setMessage(styleLabel(waypoint.getStyle()));
        }).bounds(x + COLUMN_STYLE, y, 40, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("⇄"), button -> openGroupSelect(waypoint))
                .tooltip(Tooltip.create(Component.literal("Move to another group")))
                .bounds(x + COLUMN_MOVE, y, 20, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(onOff(waypoint.isEnabled()), button -> {
            waypoint.setEnabled(!waypoint.isEnabled());
            button.setMessage(onOff(waypoint.isEnabled()));
        }).bounds(x + COLUMN_TOGGLE, y, 26, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("✖").withStyle(ChatFormatting.RED), button -> {
            allWaypoints().remove(waypoint);
            rebuildWidgets();
        }).bounds(x + COLUMN_DELETE, y, 18, WIDGET_HEIGHT).build());
    }

    // 別のグループへ移す。移した先のグループを選ぶと一覧に現れる
    private void openGroupSelect(Waypoint waypoint) {
        if (minecraft == null) return;

        WaypointManager manager = WaypointManager.getInstance();
        minecraft.gui.setScreen(new GroupSelectScreen(this, manager.groups(area), waypoint.getGroup(), selected -> {
            waypoint.setGroup(selected);
            manager.save();
        }));
    }

    private void openColorPicker(Waypoint waypoint) {
        if (minecraft == null) return;

        minecraft.gui.setScreen(new ColorPickerScreen(this, waypoint.getColor(), waypoint.getFillAlpha(),
                (rgb, fillAlpha) -> {
                    waypoint.setColor(rgb);
                    waypoint.setFillAlpha(fillAlpha);
                }));
    }

    // 座標の入力欄。数値として読めて、かつ他のウェイポイントと重ならないときだけ反映する。
    // 弾いた場合は文字を赤くして、ウェイポイントは元の位置のまま
    private EditBox coordinateBox(int x, int y, Waypoint waypoint, Axis axis) {
        EditBox box = new EditBox(font, x, y, 32, WIDGET_HEIGHT, Component.literal(axis.name()));
        box.setMaxLength(8);
        box.setValue(Integer.toString(axis.get(waypoint)));
        box.setResponder(text -> {
            int value;

            try {
                value = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                box.setTextColor(INVALID_COLOR);
                return;
            }

            if (isTaken(waypoint, axis, value)) {
                box.setTextColor(INVALID_COLOR);
                status = Component.literal("That block already has a waypoint.");
                return;
            }

            axis.set(waypoint, value);
            box.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            status = null;
        });
        return box;
    }

    // その軸だけ動かしたとき、同じエリアの別のウェイポイントに重なるかどうか
    private boolean isTaken(Waypoint waypoint, Axis axis, int value) {
        int x = axis == Axis.X ? value : waypoint.getX();
        int y = axis == Axis.Y ? value : waypoint.getY();
        int z = axis == Axis.Z ? value : waypoint.getZ();

        Waypoint existing = WaypointManager.getInstance().findAt(area, x, y, z);
        return existing != null && existing != waypoint;
    }

    private enum Axis {
        X, Y, Z;

        int get(Waypoint waypoint) {
            return switch (this) {
                case X -> waypoint.getX();
                case Y -> waypoint.getY();
                case Z -> waypoint.getZ();
            };
        }

        void set(Waypoint waypoint, int value) {
            switch (this) {
                case X -> waypoint.setX(value);
                case Y -> waypoint.setY(value);
                case Z -> waypoint.setZ(value);
            }
        }
    }

    private void cycleGroup(int direction) {
        List<String> groups = WaypointManager.getInstance().groups(area);
        int index = groups.indexOf(group);

        if (index < 0) index = 0;

        // 入力欄は init() で新しい値のまま作り直される
        group = groups.get((index + direction + groups.size()) % groups.size());
        page = 0;
        rebuildWidgets();
    }

    private void addAtPlayer() {
        if (minecraft == null || minecraft.player == null) return;

        BlockPos pos = minecraft.player.blockPosition();
        area = WaypointManager.currentArea();
        addWaypoint(Waypoint.of(defaultName(), pos, group));
    }

    // 同じブロックに既にあるときは、2つ目を作らずそちらを表示する
    private void addWaypoint(Waypoint waypoint) {
        Waypoint existing = WaypointManager.getInstance()
                .findAt(area, waypoint.getX(), waypoint.getY(), waypoint.getZ());

        if (existing != null) {
            status = Component.literal("That block already has a waypoint.");
            group = existing.getGroup();
            page = indexOfPage(existing);
            rebuildWidgets();
            return;
        }

        status = null;
        allWaypoints().add(waypoint);
        page = pageCount(shownWaypoints().size()) - 1;
        rebuildWidgets();
    }

    private int indexOfPage(Waypoint waypoint) {
        int index = shownWaypoints().indexOf(waypoint);
        return index < 0 ? 0 : index / rowsPerPage;
    }

    private String defaultName() {
        return "Waypoint " + (shownWaypoints().size() + 1);
    }

    private int pageCount(int waypointCount) {
        return Math.max(1, (waypointCount + rowsPerPage - 1) / rowsPerPage);
    }

    private static Component toggleLabel(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private static Component onOff(boolean value) {
        return Component.literal(value ? "ON" : "OFF")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component styleLabel(HighlightStyle style) {
        return Component.literal(style.displayName());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (groupEdit != GroupEdit.NONE) {
            if (event.isConfirmation()) {
                confirmGroupEdit();
                return true;
            }

            if (event.isEscape()) {
                cancelGroupEdit();
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        int pageCount = pageCount(shownWaypoints().size());

        if (pageCount > 1 && scrollY != 0.0D) {
            page = Math.clamp(page + (scrollY > 0.0D ? -1 : 1), 0, pageCount - 1);
            rebuildWidgets();
            return true;
        }

        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int left = centerX - ROW_WIDTH / 2;

        graphics.centeredText(font, title, centerX, 12, 0xFFFFFFFF);
        graphics.text(font, Component.literal("Area"), left, AREA_ROW_Y - 10, LABEL_COLOR);
        graphics.text(font, groupEdit == GroupEdit.NONE ? Component.literal("Group") : groupEditTitle(),
                left, GROUP_ROW_Y - 10, LABEL_COLOR);

        int labelY = listTop - 10;
        graphics.text(font, Component.literal("Name"), left + COLUMN_NAME, labelY, LABEL_COLOR);
        graphics.text(font, Component.literal("X"), left + COLUMN_X, labelY, LABEL_COLOR);
        graphics.text(font, Component.literal("Y"), left + COLUMN_Y, labelY, LABEL_COLOR);
        graphics.text(font, Component.literal("Z"), left + COLUMN_Z, labelY, LABEL_COLOR);
        graphics.text(font, Component.literal("Color"), left + COLUMN_SWATCH, labelY, LABEL_COLOR);
        graphics.text(font, Component.literal("Style"), left + COLUMN_STYLE, labelY, LABEL_COLOR);

        List<Waypoint> waypoints = shownWaypoints();

        if (waypoints.isEmpty()) {
            graphics.centeredText(font, Component.literal("No waypoints in this group yet."), centerX,
                    listTop + 8, 0xFF808080);
        }

        int pageCount = pageCount(waypoints.size());

        if (pageCount > 1) {
            graphics.centeredText(font, Component.literal((page + 1) + " / " + pageCount), centerX, height - 46, 0xFFFFFFFF);
        }

        if (status != null) {
            graphics.centeredText(font, status, centerX, height - 64, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        WaypointManager manager = WaypointManager.getInstance();
        manager.pruneEmptyAreas();
        manager.save();
    }
}
