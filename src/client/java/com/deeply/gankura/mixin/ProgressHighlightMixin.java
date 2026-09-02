package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.BestiaryMenu;
import com.deeply.gankura.util.TierText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bestiary と Attribute の枠を、達成状況で塗り分ける。
 *
 * どれが埋まっていて、どれが残っているかを、1つずつ開かずに見分けられるようにする。
 * 判定はロアの進み具合の行から取る
 */
@Mixin(AbstractContainerScreen.class)
public class ProgressHighlightMixin {

    // 打ち止めの印。"Attribute Level: 10 (MAX!)" のように添えられる
    @Unique
    private static final String MAX_MARK = "MAX";

    @Unique
    private static final int MAXED_COLOR = 0x6055FF55;
    @Unique
    private static final int UNFINISHED_COLOR = 0x60FF5555;

    @Unique
    private static final int SLOT_SIZE = 16;

    // ティアの数字を置く位置。アイテムの個数と同じ、右下の隅
    @Unique
    private static final int TIER_RIGHT = 17;
    @Unique
    private static final int TIER_BOTTOM = 9;
    @Unique
    private static final int TIER_COLOR = 0xFFFFFFFF;

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void gankura$markProgress(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                      CallbackInfo ci) {
        if (!GameState.Server.isSkyblock()) return;

        // 枠を描くところでは、画面の左上へ寄せる分だけ既に座標がずらしてある。
        // ここではスロットの位置をそのまま使う
        Integer color = gankura$colorFor(slot.getItem());
        if (color != null) {
            graphics.fill(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, color);
        }

        gankura$drawTier(graphics, slot);
    }

    // 今のティアを、アイテムの個数と同じ右下の隅に出す
    @Unique
    private void gankura$drawTier(GuiGraphicsExtractor graphics, Slot slot) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        ItemStack stack = slot.getItem();

        Integer tier = null;
        boolean hideMaxed = false;

        if (config.enableAttributeMenuTweaks && config.showAttributeTier) {
            tier = TierText.attributeTier(stack);
            hideMaxed = config.hideMaxedAttributeTier;
        }
        if (tier == null && config.enableBestiaryMenuTweaks && config.showBestiaryTier
                && BestiaryMenu.isOpen()) {
            tier = TierText.bestiaryTier(stack);
            hideMaxed = config.hideMaxedBestiaryTier;
        }
        if (tier == null) return;

        // 打ち止めのものは色で分かるので、数字まで出さない選び方もできる
        if (hideMaxed && TierText.isMaxed(stack)) return;

        Font font = Minecraft.getInstance().font;
        String text = String.valueOf(tier);

        graphics.text(font, text, slot.x + TIER_RIGHT - font.width(text), slot.y + TIER_BOTTOM,
                TIER_COLOR);
    }

    /**
     * その枠に塗る色。対象でなければ null。
     *
     * Attribute はロアの書き方が Bestiary と違うので、別々に見る。
     * Bestiary の方は他の画面にも似た文面があり得るので、
     * Bestiary の画面を開いているときだけ塗る
     */
    @Unique
    private Integer gankura$colorFor(ItemStack stack) {
        if (stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        boolean bestiary = config.enableBestiaryMenuTweaks && config.highlightBestiaryProgress
                && BestiaryMenu.isOpen();

        for (Component line : lore.lines()) {
            String text = line.getString();

            if (config.enableAttributeMenuTweaks && config.highlightAttributeProgress) {
                if (TierText.ATTRIBUTE_LEVEL.matcher(text).find()) {
                    return text.contains(MAX_MARK) ? MAXED_COLOR : UNFINISHED_COLOR;
                }
                if (text.contains(TierText.ATTRIBUTE_UNLOCK)) return UNFINISHED_COLOR;
            }

            if (bestiary) {
                if (text.contains(TierText.BESTIARY_LOCKED)) return UNFINISHED_COLOR;
                if (TierText.BESTIARY_PROGRESS.matcher(text).find()) {
                    return text.contains(MAX_MARK) ? MAXED_COLOR : UNFINISHED_COLOR;
                }
            }
        }
        return null;
    }
}
