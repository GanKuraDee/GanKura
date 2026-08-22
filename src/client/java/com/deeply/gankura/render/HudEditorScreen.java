package com.deeply.gankura.render;

import com.deeply.gankura.data.HudCategory;
import com.deeply.gankura.data.HudConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {

    // タブの見た目。画面下に1列で並べる
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int TAB_MAX_WIDTH = 100;
    private static final int TAB_ROW_BOTTOM_OFFSET = 55;

    // 絞り込んでいるカテゴリ。null はすべて表示
    private HudCategory selectedCategory = null;
    private final List<HudCategory> tabCategories = new ArrayList<>();
    private final List<ButtonWidget> tabButtons = new ArrayList<>();

    private HudElement draggingElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen() {
        super(Text.literal("GanKura HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset to Default"),
                button -> HudConfig.resetToDefault()
        ).dimensions(this.width / 2 - 75, this.height - 30, 150, 20).build());

        addCategoryTabs();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // ★魔法のようなループ処理: 1回のループで全てのHUDの当たり判定・枠・中身を描画！
        for (HudElement element : HudConfig.ELEMENTS) {
            if (!isEditable(element)) continue; // 設定でOFFのものはエディタにも出さない

            boolean isHovering = element.isHovering(mouseX, mouseY, this.width, this.height);
            boolean isDraggingThis = (draggingElement == element);
            int boxColor = (isHovering || isDraggingThis) ? 0x80FFFFFF : 0x40000000;

            // 当たり判定の枠を描画。枠は直前フレームの計測結果を使う(初回だけ固定サイズ)
            int drawX = element.renderX(this.width);
            int drawY = element.renderY(this.height);
            int boxX = drawX + element.hitOffsetX();
            int boxY = drawY + element.hitOffsetY();
            context.fill(boxX, boxY, boxX + element.hitWidth(), boxY + element.hitHeight(), boxColor);

            // HUDの中身を描画
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) drawX, (float) drawY);
            context.getMatrices().scale(element.scale, element.scale);
            // プレビューは固定文字列なので、ここで測った範囲がそのまま選択範囲になる
            element.beginMeasure();
            element.renderElement(context, true); // true = プレビューモード
            element.endMeasure();
            context.getMatrices().popMatrix();
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) { // 0 = 左クリック
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
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingElement != null) {
            draggingElement = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (draggingElement != null) {
            // ドラッグ中も画面内に収める
            draggingElement.x = draggingElement.clampX((int)mouseX - dragOffsetX, this.width);
            draggingElement.y = draggingElement.clampY((int)mouseY - dragOffsetY, this.height);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scroll = (float) verticalAmount * 0.1f;

        for (HudElement element : HudConfig.ELEMENTS) {
            if (isEditable(element) && element.isHovering(mouseX, mouseY, this.width, this.height)) {
                element.scale = Math.max(0.5f, Math.min(3.0f, element.scale + scroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    // 実際のゲーム中に同時に出ないHUDは、既定位置が重なっていても画面上でぶつからない。
    // 並びをカテゴリ単位で確かめられるよう、下のタブで絞り込めるようにする
    private void addCategoryTabs() {
        tabCategories.clear();
        tabButtons.clear();
        tabCategories.add(null);
        tabCategories.addAll(List.of(HudCategory.values()));

        int count = tabCategories.size();
        int tabWidth = Math.min(TAB_MAX_WIDTH, (this.width - TAB_GAP * (count + 1)) / count);
        int totalWidth = tabWidth * count + TAB_GAP * (count - 1);
        int x = (this.width - totalWidth) / 2;

        for (HudCategory category : tabCategories) {
            ButtonWidget button = ButtonWidget.builder(tabLabel(category), b -> selectCategory(category))
                    .dimensions(x, this.height - TAB_ROW_BOTTOM_OFFSET, tabWidth, TAB_HEIGHT).build();
            this.addDrawableChild(button);
            tabButtons.add(button);
            x += tabWidth + TAB_GAP;
        }
    }

    private void selectCategory(HudCategory category) {
        selectedCategory = category;
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setMessage(tabLabel(tabCategories.get(i)));
        }
    }

    // 選んでいるタブは括弧で囲って分かるようにする
    private Text tabLabel(HudCategory category) {
        String name = category == null ? "All" : category.label();
        return Text.literal(category == selectedCategory ? "[" + name + "]" : name);
    }

    // 移動画面で触れるHUDか。絞り込み中はそのカテゴリのものだけを扱う
    private boolean isEditable(HudElement element) {
        if (!element.isEnabled()) return false;
        return selectedCategory == null || element.category == selectedCategory;
    }

    @Override
    public void close() {
        HudConfig.save();
        super.close();
    }
}