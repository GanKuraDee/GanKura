package com.deeply.gankura.gui;

import com.deeply.gankura.waypoint.HighlightStyle;
import com.deeply.gankura.waypoint.Waypoint;
import com.deeply.gankura.waypoint.WaypointData;
import com.deeply.gankura.waypoint.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

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
    private Text status;

    public WaypointScreen(Screen parent) {
        this(parent, WaypointManager.currentArea());
    }

    public WaypointScreen(Screen parent, String area) {
        super(Text.literal("Custom Waypoints"));
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
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                page = (page - 1 + pageCount) % pageCount;
                clearAndInit();
            }).dimensions(centerX - 70, height - 52, 20, WIDGET_HEIGHT).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                page = (page + 1) % pageCount;
                clearAndInit();
            }).dimensions(centerX + 50, height - 52, 20, WIDGET_HEIGHT).build());
        }

        buildFooter(left, centerX);
    }

    private void buildAreaRow(int left, int y) {
        TextFieldWidget areaBox = new TextFieldWidget(textRenderer, left, y, 194, WIDGET_HEIGHT, Text.literal("Area"));
        areaBox.setMaxLength(64);
        areaBox.setText(area);
        areaBox.setEditable(false);
        addDrawableChild(areaBox);

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> cycleArea(-1))
                .dimensions(left + 198, y, 20, WIDGET_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> cycleArea(1))
                .dimensions(left + 220, y, 20, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Use current area"),
                        button -> selectArea(WaypointManager.currentArea()))
                .dimensions(left + 244, y, 136, WIDGET_HEIGHT).build());
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
        clearAndInit();
    }

    private void buildGroupRow(int left, int y) {
        if (groupEdit != GroupEdit.NONE) {
            buildGroupNameRow(left, y);
            return;
        }

        WaypointManager manager = WaypointManager.getInstance();

        TextFieldWidget groupBox = new TextFieldWidget(textRenderer, left, y, 150, WIDGET_HEIGHT, Text.literal("Group"));
        groupBox.setMaxLength(64);
        groupBox.setPlaceholder(Text.literal("(default)"));
        groupBox.setText(group);
        groupBox.setEditable(false);
        addDrawableChild(groupBox);

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> cycleGroup(-1))
                .dimensions(left + 154, y, 20, WIDGET_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> cycleGroup(1))
                .dimensions(left + 176, y, 20, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> startGroupEdit(GroupEdit.CREATE))
                .tooltip(Tooltip.of(Text.literal("Add a group")))
                .dimensions(left + 200, y, 20, WIDGET_HEIGHT).build());

        ButtonWidget renameGroup = ButtonWidget.builder(Text.literal("✎"), button -> startGroupEdit(GroupEdit.RENAME))
                .tooltip(Tooltip.of(Text.literal("Rename this group")))
                .dimensions(left + 222, y, 20, WIDGET_HEIGHT).build();
        renameGroup.active = !group.equals(Waypoint.DEFAULT_GROUP);
        addDrawableChild(renameGroup);

        ButtonWidget removeGroup = ButtonWidget.builder(Text.literal("✖").formatted(Formatting.RED),
                        button -> confirmGroupRemoval())
                .tooltip(Tooltip.of(Text.literal("Remove this group (its waypoints move to the default one)")))
                .dimensions(left + 244, y, 20, WIDGET_HEIGHT).build();
        removeGroup.active = !group.equals(Waypoint.DEFAULT_GROUP);
        addDrawableChild(removeGroup);

        boolean groupEnabled = manager.isGroupEnabled(area, group);

        addDrawableChild(ButtonWidget.builder(toggleLabel("Group", groupEnabled), button -> {
            boolean enabled = !manager.isGroupEnabled(area, group);
            manager.setGroupEnabled(area, group, enabled);
            button.setMessage(toggleLabel("Group", enabled));
        }).dimensions(left + 268, y, 112, WIDGET_HEIGHT).build());
    }

    // グループを消すと中のウェイポイントも動くので、一度確認する
    private void confirmGroupRemoval() {
        String removed = group;
        int waypointCount = WaypointManager.getInstance().waypointsOfGroup(area, removed).size();

        client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        WaypointManager.getInstance().removeGroup(area, removed);
                        group = Waypoint.DEFAULT_GROUP;
                        page = 0;
                    }

                    client.setScreen(this);
                },
                Text.literal("Remove the group \"" + removed + "\"?"),
                Text.literal("Its " + waypointCount + " waypoints move to the default group.")));
    }

    private void startGroupEdit(GroupEdit mode) {
        groupEdit = mode;
        editedGroupName = mode == GroupEdit.RENAME ? group : "";
        clearAndInit();
    }

    // グループ名を打っている間の行。新規作成と名前の変更で共用する
    private void buildGroupNameRow(int left, int y) {
        TextFieldWidget nameBox = new TextFieldWidget(textRenderer, left, y, 268, WIDGET_HEIGHT, groupEditTitle());
        nameBox.setMaxLength(64);
        nameBox.setPlaceholder(Text.literal("Enter a group name"));
        nameBox.setText(editedGroupName);
        nameBox.setChangedListener(value -> editedGroupName = value);
        addDrawableChild(nameBox);
        setInitialFocus(nameBox);

        addDrawableChild(ButtonWidget.builder(
                        Text.literal(groupEdit == GroupEdit.RENAME ? "Rename" : "Create"),
                        button -> confirmGroupEdit())
                .dimensions(left + 272, y, 54, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> cancelGroupEdit())
                .dimensions(left + 330, y, 50, WIDGET_HEIGHT).build());
    }

    private Text groupEditTitle() {
        return Text.literal(groupEdit == GroupEdit.RENAME ? "Rename this group" : "New group");
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
        clearAndInit();
    }

    private void buildFooter(int left, int centerX) {
        WaypointData data = WaypointManager.getInstance().data();

        addDrawableChild(ButtonWidget.builder(toggleLabel("Render", data.enabled), button -> {
            data.enabled = !data.enabled;
            button.setMessage(toggleLabel("Render", data.enabled));
        }).dimensions(left, height - 52, 88, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel("Names", data.showNames), button -> {
            data.showNames = !data.showNames;
            button.setMessage(toggleLabel("Names", data.showNames));
        }).dimensions(left + ROW_WIDTH - 88, height - 52, 88, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add at my position"), button -> addAtPlayer())
                .dimensions(left, height - 28, 130, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add empty"),
                        button -> addWaypoint(new Waypoint(defaultName(), 0, 0, 0, Waypoint.DEFAULT_COLOR, group)))
                .dimensions(centerX - 45, height - 28, 90, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(left + ROW_WIDTH - 80, height - 28, 80, WIDGET_HEIGHT).build());
    }

    private void addRow(Waypoint waypoint, int x, int y) {
        TextFieldWidget nameBox = new TextFieldWidget(textRenderer, x + COLUMN_NAME, y, 98, WIDGET_HEIGHT,
                Text.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setText(waypoint.getName());
        nameBox.setChangedListener(waypoint::setName);
        addDrawableChild(nameBox);

        addDrawableChild(coordinateBox(x + COLUMN_X, y, waypoint, Axis.X));
        addDrawableChild(coordinateBox(x + COLUMN_Y, y, waypoint, Axis.Y));
        addDrawableChild(coordinateBox(x + COLUMN_Z, y, waypoint, Axis.Z));

        addDrawableChild(new ColorSwatchButton(x + COLUMN_SWATCH, y, 40, WIDGET_HEIGHT,
                waypoint::getColor, waypoint::getFillAlpha, () -> openColorPicker(waypoint)));

        addDrawableChild(ButtonWidget.builder(styleLabel(waypoint.getStyle()), button -> {
            waypoint.setStyle(waypoint.getStyle().next());
            button.setMessage(styleLabel(waypoint.getStyle()));
        }).dimensions(x + COLUMN_STYLE, y, 40, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("⇄"), button -> openGroupSelect(waypoint))
                .tooltip(Tooltip.of(Text.literal("Move to another group")))
                .dimensions(x + COLUMN_MOVE, y, 20, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(onOff(waypoint.isEnabled()), button -> {
            waypoint.setEnabled(!waypoint.isEnabled());
            button.setMessage(onOff(waypoint.isEnabled()));
        }).dimensions(x + COLUMN_TOGGLE, y, 26, WIDGET_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("✖").formatted(Formatting.RED), button -> {
            allWaypoints().remove(waypoint);
            clearAndInit();
        }).dimensions(x + COLUMN_DELETE, y, 18, WIDGET_HEIGHT).build());
    }

    // 別のグループへ移す。移した先のグループを選ぶと一覧に現れる
    private void openGroupSelect(Waypoint waypoint) {
        WaypointManager manager = WaypointManager.getInstance();
        client.setScreen(new GroupSelectScreen(this, manager.groups(area), waypoint.getGroup(), selected -> {
            waypoint.setGroup(selected);
            manager.save();
        }));
    }

    private void openColorPicker(Waypoint waypoint) {
        client.setScreen(new ColorPickerScreen(this, waypoint.getColor(), waypoint.getFillAlpha(),
                (rgb, fillAlpha) -> {
                    waypoint.setColor(rgb);
                    waypoint.setFillAlpha(fillAlpha);
                }));
    }

    // 座標の入力欄。数値として読めて、かつ他のウェイポイントと重ならないときだけ反映する。
    // 弾いた場合は文字を赤くして、ウェイポイントは元の位置のまま
    private TextFieldWidget coordinateBox(int x, int y, Waypoint waypoint, Axis axis) {
        TextFieldWidget box = new TextFieldWidget(textRenderer, x, y, 32, WIDGET_HEIGHT, Text.literal(axis.name()));
        box.setMaxLength(8);
        box.setText(Integer.toString(axis.get(waypoint)));
        box.setChangedListener(text -> {
            int value;

            try {
                value = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                box.setEditableColor(INVALID_COLOR);
                return;
            }

            if (isTaken(waypoint, axis, value)) {
                box.setEditableColor(INVALID_COLOR);
                status = Text.literal("That block already has a waypoint.");
                return;
            }

            axis.set(waypoint, value);
            box.setEditableColor(TextFieldWidget.DEFAULT_EDITABLE_COLOR);
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
        clearAndInit();
    }

    private void addAtPlayer() {
        if (client.player == null) return;

        BlockPos pos = client.player.getBlockPos();
        area = WaypointManager.currentArea();
        addWaypoint(Waypoint.of(defaultName(), pos, group));
    }

    // 同じブロックに既にあるときは、2つ目を作らずそちらを表示する
    private void addWaypoint(Waypoint waypoint) {
        Waypoint existing = WaypointManager.getInstance()
                .findAt(area, waypoint.getX(), waypoint.getY(), waypoint.getZ());

        if (existing != null) {
            status = Text.literal("That block already has a waypoint.");
            group = existing.getGroup();
            page = indexOfPage(existing);
            clearAndInit();
            return;
        }

        status = null;
        allWaypoints().add(waypoint);
        page = pageCount(shownWaypoints().size()) - 1;
        clearAndInit();
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

    private static Text toggleLabel(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private static Text onOff(boolean value) {
        return Text.literal(value ? "ON" : "OFF").formatted(value ? Formatting.GREEN : Formatting.GRAY);
    }

    private static Text styleLabel(HighlightStyle style) {
        return Text.literal(style.displayName());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (groupEdit != GroupEdit.NONE) {
            if (input.isEnter()) {
                confirmGroupEdit();
                return true;
            }

            if (input.isEscape()) {
                cancelGroupEdit();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        int pageCount = pageCount(shownWaypoints().size());

        if (pageCount > 1 && scrollY != 0.0D) {
            page = Math.clamp(page + (scrollY > 0.0D ? -1 : 1), 0, pageCount - 1);
            clearAndInit();
            return true;
        }

        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int left = centerX - ROW_WIDTH / 2;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 12, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal("Area"), left, AREA_ROW_Y - 10, LABEL_COLOR, false);
        context.drawText(textRenderer, groupEdit == GroupEdit.NONE ? Text.literal("Group") : groupEditTitle(),
                left, GROUP_ROW_Y - 10, LABEL_COLOR, false);

        int labelY = listTop - 10;
        context.drawText(textRenderer, Text.literal("Name"), left + COLUMN_NAME, labelY, LABEL_COLOR, false);
        context.drawText(textRenderer, Text.literal("X"), left + COLUMN_X, labelY, LABEL_COLOR, false);
        context.drawText(textRenderer, Text.literal("Y"), left + COLUMN_Y, labelY, LABEL_COLOR, false);
        context.drawText(textRenderer, Text.literal("Z"), left + COLUMN_Z, labelY, LABEL_COLOR, false);
        context.drawText(textRenderer, Text.literal("Color"), left + COLUMN_SWATCH, labelY, LABEL_COLOR, false);
        context.drawText(textRenderer, Text.literal("Style"), left + COLUMN_STYLE, labelY, LABEL_COLOR, false);

        List<Waypoint> waypoints = shownWaypoints();

        if (waypoints.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No waypoints in this group yet."),
                    centerX, listTop + 8, 0xFF808080);
        }

        int pageCount = pageCount(waypoints.size());

        if (pageCount > 1) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal((page + 1) + " / " + pageCount),
                    centerX, height - 46, 0xFFFFFFFF);
        }

        if (status != null) {
            context.drawCenteredTextWithShadow(textRenderer, status, centerX, height - 64, 0xFFFF5555);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        WaypointManager manager = WaypointManager.getInstance();
        manager.pruneEmptyAreas();
        manager.save();
    }
}
