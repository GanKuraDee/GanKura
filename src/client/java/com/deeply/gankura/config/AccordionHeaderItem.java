package com.deeply.gankura.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.teamresourceful.resourcefulconfig.client.components.options.SeparatorItem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 折りたたみ見出しの行。行のどこを押しても開閉する。
 * 見た目は ResourcefulConfig の区切り線の行に、開閉の印を足したもの。字下げは ConfigAccordions が付ける
 */
public class AccordionHeaderItem extends SeparatorItem {

    private static final int HOVER_COLOR = 0x20FFFFFF;

    private final String group;
    // 検索中は全部開いて見せているだけなので、押しても開閉しない
    private final boolean locked;

    public AccordionHeaderItem(String group, boolean open, boolean locked) {
        super(title(group, open), description(group));
        this.group = group;
        this.locked = locked;
    }

    private static Component title(String group, boolean open) {
        return Component.literal(open ? "▼ " : "▶ ")
                .append(Component.translatable(group));
    }

    private static Component description(String group) {
        String key = group + ".desc";
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.empty();
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTicks) {
        if (this.isHovered() && !this.locked) {
            graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), HOVER_COLOR);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (this.locked || event.input() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (!this.isMouseOver(event.x(), event.y())) return false;
        ConfigAccordions.toggle(this.group);
        return true;
    }
}
