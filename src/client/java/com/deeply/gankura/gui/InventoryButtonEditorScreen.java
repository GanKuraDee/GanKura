package com.deeply.gankura.gui;

import com.deeply.gankura.data.InventoryButton;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inventory Button の置き場所とコマンドを決める画面。
 *
 * NotEnoughUpdates の /neubuttons を移植したもの。
 * 疑似的に描いた持ち物画面の周りに枠を並べ、押した枠の中身を右の欄で決める。
 * 枠の位置そのものはひな型で決まっていて、動かすのではなく使う枠を選ぶ形になっている
 */
public class InventoryButtonEditorScreen extends Screen {

    private static final int BUTTON_SIZE = InventoryButtonTexture.BUTTON_SIZE;
    private static final int PANEL_WIDTH = InventoryButtonTexture.PANEL_WIDTH;
    private static final int PANEL_HEIGHT = InventoryButtonTexture.PANEL_HEIGHT;
    private static final int INVENTORY_WIDTH = InventoryButtonTexture.INVENTORY_WIDTH;
    private static final int INVENTORY_HEIGHT = InventoryButtonTexture.INVENTORY_HEIGHT;

    // 編集パネルの中での位置
    private static final int FIELD_X = 7;
    private static final int FIELD_WIDTH = PANEL_WIDTH - 14;
    private static final int FIELD_HEIGHT = 16;
    private static final int COMMAND_LABEL_Y = 7;
    private static final int COMMAND_FIELD_Y = 19;
    private static final int BACKGROUND_LABEL_Y = 40;
    private static final int BACKGROUND_ROW_Y = 50;
    private static final int ICON_TYPE_LABEL_Y = 74;
    private static final int ICON_TYPE_ROW_Y = 84;
    private static final int ICON_LABEL_Y = 105;
    private static final int ICON_FIELD_Y = 115;
    private static final int GRID_TOP = 136;
    private static final int GRID_BOTTOM = 196;
    private static final int GRID_FIRST_ROW_Y = 137;
    private static final int GRID_LEFT = 12;
    private static final int GRID_COLUMNS = 6;
    private static final int GRID_ROWS = 4;
    private static final int GRID_STEP = 20;
    private static final int SCROLLBAR_X = 137;

    // 見出しとひな型の名前の色
    private static final int LABEL_COLOR = 0xFFA0A0A0;
    private static final int PRESET_COLOR = 0xFF909090;
    private static final int SAVED_PRESET_COLOR = 0xFFD0D0D0;
    private static final int STATUS_COLOR = 0xFFFFAA00;
    private static final int PLACEHOLDER_COLOR = 0xFFCCCCCC;
    private static final int SELECTED_OUTLINE = 0xFF0000FF;
    // 使っていない枠は薄く見せる
    private static final int INACTIVE_TINT = 0x80FFFFFF;
    private static final int OPAQUE = 0xFFFFFFFF;

    // ひな型一覧の位置
    private static final int PRESET_PANEL_GAP = 22;
    private static final int PRESET_TITLE_Y = 10;
    private static final int PRESET_FIRST_Y = 25;
    private static final int PRESET_STEP = 10;

    // 保存欄の大きさ
    private static final int SAVE_WIDTH = 88;
    private static final int SAVE_BUTTON_HEIGHT = 20;
    // 知らせを出しておく時間
    private static final long STATUS_MILLIS = 3000;

    private final Screen parent;

    private int guiLeft;
    private int guiTop;

    private int panelLeft;
    private int panelTop;
    private boolean showArrow;

    private InventoryButton editing;
    private InventoryButtonIcons.IconType iconType = InventoryButtonIcons.IconType.ITEM;
    private final List<String> searchedIcons = new ArrayList<>();
    private int scroll;

    private EditBox commandBox;
    private EditBox iconBox;
    private EditBox nameBox;

    // 保存や削除の結果。しばらく出してから消す
    private Component status;
    private long statusUntil;

    public InventoryButtonEditorScreen(Screen parent) {
        super(Component.literal("Inventory Buttons"));
        this.parent = parent;
    }

    private static List<InventoryButton> buttons() {
        return ModConfig.INSTANCE.inventoryButtons.buttons;
    }

    @Override
    protected void init() {
        guiLeft = width / 2 - INVENTORY_WIDTH / 2;
        guiTop = height / 2 - INVENTORY_HEIGHT / 2;

        commandBox = new EditBox(font, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Command"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("/warp hub"));
        commandBox.setResponder(value -> {
            if (editing != null) editing.command = value;
        });
        addWidget(commandBox);

        iconBox = new EditBox(font, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Icon"));
        iconBox.setMaxLength(64);
        iconBox.setHint(Component.literal("Search"));
        iconBox.setResponder(value -> search());
        addWidget(iconBox);

        int saveLeft = guiLeft - SAVE_WIDTH - PRESET_PANEL_GAP - 2;
        nameBox = new EditBox(font, saveLeft, guiTop + 2, SAVE_WIDTH, FIELD_HEIGHT,
                Component.literal("Preset name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal("Preset name"));
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.literal("Save Preset"), button -> savePreset())
                .bounds(saveLeft, guiTop + 2 + FIELD_HEIGHT + 4, SAVE_WIDTH, SAVE_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(
                        Component.literal("Saves the current buttons to the preset list.")))
                .build());

        select(null);
    }

    // -------------------------------------------------- 選択

    private void select(InventoryButton button) {
        editing = button;

        if (button == null) {
            commandBox.visible = false;
            iconBox.visible = false;
            setFocused(null);
            return;
        }

        commandBox.visible = true;
        iconBox.visible = true;
        commandBox.setValue(button.commandOrEmpty());
        iconBox.setValue("");
        scroll = 0;
        search();
        layoutPanel();
    }

    // 編集パネルは選んだ枠の下に出す。画面からはみ出すときは中に収める
    private void layoutPanel() {
        if (editing == null) return;

        int x = buttonX(editing);
        int y = buttonY(editing);

        panelLeft = x + BUTTON_SIZE / 2 - PANEL_WIDTH / 2;
        panelTop = y + BUTTON_SIZE + 2;
        showArrow = true;

        if (panelTop + PANEL_HEIGHT + 5 > height) {
            panelTop = height - PANEL_HEIGHT - 5;
            showArrow = false;
        }
        if (panelLeft < 5) {
            panelLeft = 5;
            showArrow = false;
        }
        if (panelLeft + PANEL_WIDTH + 5 > width) {
            panelLeft = width - PANEL_WIDTH - 5;
            showArrow = false;
        }

        commandBox.setX(panelLeft + FIELD_X);
        commandBox.setY(panelTop + COMMAND_FIELD_Y);
        iconBox.setX(panelLeft + FIELD_X);
        iconBox.setY(panelTop + ICON_FIELD_Y);
    }

    private int buttonX(InventoryButton button) {
        return guiLeft + button.x + (button.anchorRight ? INVENTORY_WIDTH : 0);
    }

    private int buttonY(InventoryButton button) {
        return guiTop + button.y + (button.anchorBottom ? INVENTORY_HEIGHT : 0);
    }

    // -------------------------------------------------- 描画

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        InventoryButtonTexture.drawInventory(graphics, guiLeft, guiTop);

        for (InventoryButton button : buttons()) {
            if (button == null) continue;

            int x = buttonX(button);
            int y = buttonY(button);
            boolean active = button.isActive();

            InventoryButtonTexture.drawButton(graphics, button.backgroundIndex, x, y,
                    active ? OPAQUE : INACTIVE_TINT);

            if (active) {
                InventoryButtonIcons.render(graphics, button.iconOrEmpty(), x + 1, y + 1);
            } else {
                graphics.text(font, Component.literal("+"), x + 6, y + 5, PLACEHOLDER_COLOR, false);
            }
        }

        drawPresets(graphics);
    }

    private void drawPresets(GuiGraphicsExtractor graphics) {
        Map<String, List<InventoryButton>> presets = InventoryButtonPresets.presets();
        if (presets.isEmpty()) return;

        int left = presetLeft();
        InventoryButtonTexture.drawPresetPanel(graphics, left, guiTop, INVENTORY_HEIGHT);

        int centerX = left + InventoryButtonTexture.PRESET_PANEL_WIDTH / 2;
        graphics.centeredText(font, Component.literal("Presets").withStyle(style -> style.withUnderlined(true)),
                centerX, guiTop + PRESET_TITLE_Y, LABEL_COLOR);

        int index = 0;
        for (String name : presets.keySet()) {
            // 自分で保存した分は消せるので、Mod が持っている分と色を変えて見分けられるようにする
            int color = InventoryButtonPresets.isBuiltIn(name) ? PRESET_COLOR : SAVED_PRESET_COLOR;
            graphics.centeredText(font, Component.literal(name), centerX,
                    guiTop + PRESET_FIRST_Y + PRESET_STEP * index++, color);
        }

        graphics.centeredText(font, Component.literal("Click to load"), centerX,
                guiTop + INVENTORY_HEIGHT - 24, PRESET_COLOR);
        graphics.centeredText(font, Component.literal("Right click to delete"), centerX,
                guiTop + INVENTORY_HEIGHT - 14, PRESET_COLOR);

        if (status != null && System.currentTimeMillis() < statusUntil) {
            graphics.centeredText(font, status, guiLeft - SAVE_WIDTH / 2 - PRESET_PANEL_GAP - 2,
                    guiTop + 2 + FIELD_HEIGHT + 4 + SAVE_BUTTON_HEIGHT + 6, STATUS_COLOR);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (editing == null) return;

        // 編集パネルは、ひな型の読み書きボタンより後に描く。
        // 先に描くと、あちらのボタンがパネルの上に出てしまう
        InventoryButtonTexture.drawPanel(graphics, panelLeft, panelTop);
        if (showArrow) {
            InventoryButtonTexture.drawArrow(graphics,
                    buttonX(editing) + BUTTON_SIZE / 2 - 3, buttonY(editing) + BUTTON_SIZE);
        }

        commandBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        iconBox.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(font, Component.literal("Command"), panelLeft + FIELD_X, panelTop + COMMAND_LABEL_Y,
                LABEL_COLOR, false);
        graphics.text(font, Component.literal("Background"), panelLeft + FIELD_X, panelTop + BACKGROUND_LABEL_Y,
                LABEL_COLOR, false);

        for (int i = 0; i < InventoryButtonTexture.BACKGROUND_TYPES; i++) {
            int x = panelLeft + FIELD_X + GRID_STEP * i;
            int y = panelTop + BACKGROUND_ROW_Y;

            if (i == editing.backgroundIndex) {
                graphics.fill(x - 1, y - 1, x + BUTTON_SIZE + 1, y + BUTTON_SIZE + 1, SELECTED_OUTLINE);
            }
            InventoryButtonTexture.drawSwatch(graphics, i, x, y);
        }

        graphics.text(font, Component.literal("Icon Type"), panelLeft + FIELD_X, panelTop + ICON_TYPE_LABEL_Y,
                LABEL_COLOR, false);

        InventoryButtonIcons.IconType[] types = InventoryButtonIcons.IconType.values();
        for (int i = 0; i < types.length; i++) {
            int x = panelLeft + FIELD_X + GRID_STEP * i;
            int y = panelTop + ICON_TYPE_ROW_Y;

            if (types[i] == iconType) {
                graphics.fill(x - 1, y - 1, x + BUTTON_SIZE + 1, y + BUTTON_SIZE + 1, SELECTED_OUTLINE);
            }
            InventoryButtonTexture.drawSlot(graphics, x, y);
            graphics.item(iconTypeSample(types[i]), x + 1, y + 1);
        }

        graphics.text(font, Component.literal("Icon Selector"), panelLeft + FIELD_X, panelTop + ICON_LABEL_Y,
                LABEL_COLOR, false);

        drawIconGrid(graphics);
    }

    // タブに描く見本。アイテム・ヘッド・Mod が持つ絵の順
    private ItemStack iconTypeSample(InventoryButtonIcons.IconType type) {
        return switch (type) {
            case ITEM -> new ItemStack(Items.DIAMOND_SWORD);
            case SKULL -> InventoryButtonIcons.getStack(
                    "skull:c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56");
            case EXTRA -> new ItemStack(Items.LEAD);
        };
    }

    private void drawIconGrid(GuiGraphicsExtractor graphics) {
        graphics.enableScissor(panelLeft, panelTop + GRID_TOP, panelLeft + PANEL_WIDTH, panelTop + GRID_BOTTOM);

        int offset = scroll % GRID_STEP;
        int startIndex = startIndex();
        int endIndex = Math.min(searchedIcons.size(), startIndex + GRID_COLUMNS * GRID_ROWS);

        for (int i = startIndex; i < endIndex; i++) {
            int column = (i - startIndex) % GRID_COLUMNS;
            int row = (i - startIndex) / GRID_COLUMNS;
            int x = panelLeft + GRID_LEFT + column * GRID_STEP;
            int y = panelTop + GRID_FIRST_ROW_Y + row * GRID_STEP - offset;

            InventoryButtonTexture.drawSlot(graphics, x, y);
            InventoryButtonIcons.render(graphics, searchedIcons.get(i), x + 1, y + 1);
        }

        graphics.disableScissor();

        drawScrollBar(graphics);
    }

    private void drawScrollBar(GuiGraphicsExtractor graphics) {
        int max = maxScroll();
        if (max <= 0) return;

        int trackHeight = GRID_BOTTOM - GRID_TOP - 6;
        int barHeight = Math.max(2, trackHeight * trackHeight / (trackHeight + max));
        int barY = (trackHeight - barHeight) * Math.clamp(scroll, 0, max) / max;

        graphics.fill(panelLeft + SCROLLBAR_X, panelTop + GRID_FIRST_ROW_Y + barY,
                panelLeft + SCROLLBAR_X + 2, panelTop + GRID_FIRST_ROW_Y + barY + barHeight, 0xFF202020);
    }

    private int startIndex() {
        return Math.max(0, scroll / GRID_STEP * GRID_COLUMNS);
    }

    private int maxScroll() {
        if (searchedIcons.isEmpty()) return 0;

        int rows = (searchedIcons.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
        return Math.max(0, rows * GRID_STEP - (GRID_BOTTOM - GRID_TOP));
    }

    // -------------------------------------------------- 操作

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        // パネルはひな型の読み書きボタンの上に重なるので、
        // パネルの中を押したときは、下に隠れているボタンへ渡さない
        if (editing != null && insidePanel(mouseX, mouseY)) {
            if (commandBox.mouseClicked(event, doubleClick)) {
                setFocused(commandBox);
                return true;
            }
            if (iconBox.mouseClicked(event, doubleClick)) {
                setFocused(iconBox);
                return true;
            }

            setFocused(null);
            clickPanel(mouseX, mouseY);
            return true;
        }

        if (super.mouseClicked(event, doubleClick)) return true;

        for (InventoryButton button : buttons()) {
            if (button == null) continue;

            int x = buttonX(button);
            int y = buttonY(button);
            if (mouseX < x || mouseX > x + BUTTON_SIZE || mouseY < y || mouseY > y + BUTTON_SIZE) continue;

            select(editing == button ? null : button);
            return true;
        }

        if (editing == null && clickPreset(mouseX, mouseY, event.button())) return true;

        select(null);
        return true;
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        return mouseX >= panelLeft && mouseX <= panelLeft + PANEL_WIDTH
                && mouseY >= panelTop && mouseY <= panelTop + PANEL_HEIGHT;
    }

    private void clickPanel(double mouseX, double mouseY) {
        int localX = (int) (mouseX - panelLeft);
        int localY = (int) (mouseY - panelTop);

        // 枠の絵柄
        if (localY >= BACKGROUND_ROW_Y && localY <= BACKGROUND_ROW_Y + BUTTON_SIZE) {
            int index = rowIndex(localX, InventoryButtonTexture.BACKGROUND_TYPES);
            if (index >= 0) {
                editing.backgroundIndex = index;
                return;
            }
        }

        // アイコンの種類
        if (localY >= ICON_TYPE_ROW_Y && localY <= ICON_TYPE_ROW_Y + BUTTON_SIZE) {
            InventoryButtonIcons.IconType[] types = InventoryButtonIcons.IconType.values();
            int index = rowIndex(localX, types.length);
            if (index >= 0 && types[index] != iconType) {
                iconType = types[index];
                scroll = 0;
                search();
            }
            return;
        }

        // 並んでいるアイコン
        if (localY >= GRID_TOP && localY <= GRID_BOTTOM) {
            String icon = iconAt(mouseX, mouseY);
            if (icon != null) editing.icon = icon;
        }
    }

    // 20ピクセル間隔で並んでいる列のうち、どれを押したか。隙間なら -1
    private static int rowIndex(int localX, int count) {
        for (int i = 0; i < count; i++) {
            int x = FIELD_X + GRID_STEP * i;
            if (localX >= x && localX <= x + BUTTON_SIZE) return i;
        }
        return -1;
    }

    private String iconAt(double mouseX, double mouseY) {
        int offset = scroll % GRID_STEP;
        int startIndex = startIndex();
        int endIndex = Math.min(searchedIcons.size(), startIndex + GRID_COLUMNS * GRID_ROWS);

        for (int i = startIndex; i < endIndex; i++) {
            int column = (i - startIndex) % GRID_COLUMNS;
            int row = (i - startIndex) / GRID_COLUMNS;
            int x = panelLeft + GRID_LEFT + column * GRID_STEP;
            int y = panelTop + GRID_FIRST_ROW_Y + row * GRID_STEP - offset;

            if (mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE) {
                return searchedIcons.get(i);
            }
        }
        return null;
    }

    private boolean clickPreset(double mouseX, double mouseY, int mouseButton) {
        int left = presetLeft();
        if (mouseX < left || mouseX > left + InventoryButtonTexture.PRESET_PANEL_WIDTH) return false;

        int index = 0;
        for (Map.Entry<String, List<InventoryButton>> preset : InventoryButtonPresets.presets().entrySet()) {
            int top = guiTop + PRESET_FIRST_Y - 1 + PRESET_STEP * index++;
            if (mouseY < top || mouseY > top + PRESET_STEP) continue;

            // 右クリックは削除。Mod が持っている分は消せない
            if (mouseButton == 1) {
                if (InventoryButtonPresets.delete(preset.getKey())) {
                    setStatus(Component.literal("Deleted " + preset.getKey() + "."));
                } else {
                    setStatus(Component.literal("That preset cannot be deleted."));
                }
                return true;
            }

            replaceButtons(InventoryButtonPresets.copyOf(preset.getValue()));
            setStatus(Component.literal("Loaded " + preset.getKey() + "."));
            return true;
        }
        return false;
    }

    private int presetLeft() {
        return guiLeft + INVENTORY_WIDTH + PRESET_PANEL_GAP;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (editing == null || scrollY == 0) return false;

        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * GRID_STEP, 0, maxScroll());
        return true;
    }

    // -------------------------------------------------- 一覧と受け渡し

    private void search() {
        searchedIcons.clear();
        searchedIcons.addAll(InventoryButtonIcons.search(iconType, iconBox == null ? "" : iconBox.getValue()));
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    private void replaceButtons(List<InventoryButton> buttons) {
        ModConfig.InventoryButtonsCategory config = ModConfig.INSTANCE.inventoryButtons;
        config.buttons.clear();
        config.buttons.addAll(buttons);
        select(null);
    }

    // 今の並びを一覧に足す。名前を入れていなければ空いている番号を使う
    private void savePreset() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) name = InventoryButtonPresets.nextName();

        String problem = InventoryButtonPresets.save(name, buttons());
        if (problem != null) {
            setStatus(Component.literal(problem));
            return;
        }

        nameBox.setValue("");
        setStatus(Component.literal("Saved " + name + "."));
    }

    private void setStatus(Component message) {
        status = message;
        statusUntil = System.currentTimeMillis() + STATUS_MILLIS;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        // 画面を閉じた時点で確実に残す。次に開くまでに落ちても設定が消えないようにする
        ModConfig.INSTANCE.saveNow();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
