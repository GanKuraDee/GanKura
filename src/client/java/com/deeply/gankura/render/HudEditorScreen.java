package com.deeply.gankura.render;

import com.deeply.gankura.data.HudCategory;
import com.deeply.gankura.data.HudConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {

    // カテゴリの選択。横に並べると増えたときに画面下を埋めてしまうので、
    // 左右の矢印で送る1つのボタンにしている
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int TAB_WIDTH = 140;
    private static final int TAB_ARROW_WIDTH = 20;
    private static final int TAB_ROW_BOTTOM_OFFSET = 55;

    // 選べるカテゴリ。先頭の null はすべて表示
    private final List<HudCategory> tabCategories = new ArrayList<>();
    private int categoryIndex = 0;
    private Button categoryButton;

    private HudElement draggingElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen() {
        super(Component.literal("GanKura HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
                Component.literal("Reset to Default"),
                button -> HudConfig.resetToDefault()
        ).bounds(this.width / 2 - 75, this.height - 30, 150, 20).build());

        addCategoryTabs();

        // Minecraft 26.1.x では Screen に mouseScrolled がないため Fabric API で登録する
        ScreenMouseEvents.beforeMouseScroll(this).register((screen, mouseX, mouseY, h, v) -> {
            float scroll = (float) v * 0.1f;
            for (HudElement element : HudConfig.ELEMENTS) {
                if (isEditable(element) && element.isHovering(mouseX, mouseY, this.width, this.height)) {
                    element.scale = Math.max(0.5f, Math.min(3.0f, element.scale + scroll));
                    break;
                }
            }
        });
    }

    /**
     * Minecraft 26.1.2 仕様:
     * Screen クラスでは render ではなく extractRenderState をオーバーライドします。
     * 引数: GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 背景の半透明塗りつぶし
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);

        for (HudElement element : HudConfig.ELEMENTS) {
            if (!isEditable(element)) continue;

            boolean isHovering = element.isHovering(mouseX, mouseY, this.width, this.height);
            boolean isDraggingThis = (draggingElement == element);
            int boxColor = (isHovering || isDraggingThis) ? 0x80FFFFFF : 0x40000000;

            int drawX = element.renderX(this.width);
            int drawY = element.renderY(this.height);

            // 枠は直前フレームの計測結果を使う(初回だけ固定サイズ)
            int boxX = drawX + element.hitOffsetX();
            int boxY = drawY + element.hitOffsetY();
            graphics.fill(boxX, boxY, boxX + element.hitWidth(), boxY + element.hitHeight(), boxColor);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) drawX, (float) drawY);
            graphics.pose().scale(element.scale, element.scale);

            // プレビューは固定文字列なので、ここで測った範囲がそのまま選択範囲になる
            element.beginMeasure();
            element.renderElement(graphics, true);
            element.endMeasure();

            graphics.pose().popMatrix();
        }

        // Screen.class L104: super への委譲も extractRenderState を使用
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // MouseButtonEvent.class L10: フィールド名は x() と y() です
        double mouseX = event.x();
        double mouseY = event.y();
        // button() メソッドを使用して int 値を取得
        int button = event.button();

        if (button == 0) {
            for (HudElement element : HudConfig.ELEMENTS) {
                if (isEditable(element) && element.isHovering(mouseX, mouseY, this.width, this.height)) {
                    draggingElement = element;
                    // 画面外に出ていたHUDは寄せた位置から掴めるよう、描画位置を基準にオフセットを取る
                    dragOffsetX = (int)mouseX - element.renderX(this.width);
                    dragOffsetY = (int)mouseY - element.renderY(this.height);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingElement != null) {
            draggingElement = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingElement != null) {
            // ドラッグ中も画面内に収める
            draggingElement.x = draggingElement.clampX((int)event.x() - dragOffsetX, this.width);
            draggingElement.y = draggingElement.clampY((int)event.y() - dragOffsetY, this.height);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }


    // 実際のゲーム中に同時に出ないHUDは、既定位置が重なっていても画面上でぶつからない。
    // 並びをカテゴリ単位で確かめられるよう、下のタブで絞り込めるようにする
    private void addCategoryTabs() {
        tabCategories.clear();
        tabCategories.add(null);
        tabCategories.addAll(List.of(HudCategory.values()));
        categoryIndex = Math.min(categoryIndex, tabCategories.size() - 1);

        int totalWidth = TAB_WIDTH + (TAB_ARROW_WIDTH + TAB_GAP) * 2;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - TAB_ROW_BOTTOM_OFFSET;

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleCategory(-1))
                .bounds(x, y, TAB_ARROW_WIDTH, TAB_HEIGHT).build());

        // 真ん中を押しても送れるようにしておく
        categoryButton = Button.builder(tabLabel(), b -> cycleCategory(1))
                .bounds(x + TAB_ARROW_WIDTH + TAB_GAP, y, TAB_WIDTH, TAB_HEIGHT).build();
        this.addRenderableWidget(categoryButton);

        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleCategory(1))
                .bounds(x + TAB_ARROW_WIDTH + TAB_GAP + TAB_WIDTH + TAB_GAP, y,
                        TAB_ARROW_WIDTH, TAB_HEIGHT).build());
    }

    // 端まで行ったら反対側へ回る
    private void cycleCategory(int step) {
        int size = tabCategories.size();
        categoryIndex = ((categoryIndex + step) % size + size) % size;
        categoryButton.setMessage(tabLabel());
    }

    // 何件中の何件目か分かるよう、名前に番号を添える
    private Component tabLabel() {
        HudCategory category = selectedCategory();
        String name = category == null ? "All" : category.label();
        return Component.literal(name + " §7(" + (categoryIndex + 1) + "/" + tabCategories.size() + ")");
    }

    private HudCategory selectedCategory() {
        return tabCategories.isEmpty() ? null : tabCategories.get(categoryIndex);
    }

    // 移動画面で触れるHUDか。絞り込み中はそのカテゴリのものだけを扱う
    private boolean isEditable(HudElement element) {
        if (!element.isEnabled()) return false;
        HudCategory category = selectedCategory();
        return category == null || element.category == category;
    }

    @Override
    public void onClose() {
        HudConfig.save();
        super.onClose();
    }
}