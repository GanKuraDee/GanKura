package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.EnchantedBookText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * エンチャント本の枠に、中身のエンチャントとその段を添える。
 *
 * 本はどれも名前も絵柄も同じなので、並んでいる状態では
 * 1つずつカーソルを合わせないと中身が分からない。
 * 段はアイテムの個数と同じ右下、名前の頭文字は枠の上辺に置く
 */
@Mixin(AbstractContainerScreen.class)
public class EnchantedBookSlotMixin {

    @Unique
    private static final int SLOT_SIZE = 16;

    // 段の数字を置く位置。アイテムの個数と同じ、右下の隅
    @Unique
    private static final int LEVEL_RIGHT = 17;
    @Unique
    private static final int LEVEL_BOTTOM = 9;
    @Unique
    private static final int LEVEL_COLOR = 0xFFFFFF55;

    // 色はロアの記号が決めるので、ここは記号の無い文字が出たときの受け皿
    @Unique
    private static final int NAME_COLOR = 0xFFFFFFFF;

    // 数字は読めないと意味が無いので、アイテムを描いた後に重ねる
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void gankura$markEnchantedBook(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                           CallbackInfo ci) {
        if (!ModConfig.Interface.enableEnchantedBookSlots) return;
        if (!GameState.Server.isSkyblock()) return;

        EnchantedBookText.Book book = EnchantedBookText.of(slot.getItem());
        if (book == null) return;

        Font font = Minecraft.getInstance().font;

        if (ModConfig.Interface.showEnchantedBookTier) {
            String level = String.valueOf(book.level());
            graphics.text(font, level, slot.x + LEVEL_RIGHT - font.width(level), slot.y + LEVEL_BOTTOM,
                    LEVEL_COLOR);
        }
        if (ModConfig.Interface.showEnchantedBookName) {
            gankura$drawName(graphics, font, slot, book);
        }
    }

    /** 頭文字を枠の幅に収めて描く。3文字を超えるものだけ少し縮む */
    @Unique
    private void gankura$drawName(GuiGraphicsExtractor graphics, Font font, Slot slot,
                                  EnchantedBookText.Book book) {
        // ロアと同じ色記号を頭に付ける。太さも記号のまま乗る
        String text = book.style() + EnchantedBookText.initials(book.name());
        int width = font.width(text);
        if (width <= 0) return;

        float scale = Math.min(1.0f, (float) SLOT_SIZE / width);

        graphics.pose().pushMatrix();
        graphics.pose().translate(slot.x, (float) slot.y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, NAME_COLOR);
        graphics.pose().popMatrix();
    }
}
