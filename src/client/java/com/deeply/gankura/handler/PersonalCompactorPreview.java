package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.SkyblockItemIcons;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Personal Compactor / Personal Deletor にカーソルを乗せたとき、
 * 自動で固める・消すように登録してある品を、名前のすぐ下にアイコンで並べる。
 *
 * 登録してある品は、アイテムの属性に "personal_compact_0" や "personal_deletor_0" の形で
 * 枠ごとの SkyBlock ID として書かれている。枠の数は段で決まる。
 * 入り切りは、どちらの種類でも "PERSONAL_DELETOR_ACTIVE" に書かれている
 */
public final class PersonalCompactorPreview {

    // "PERSONAL_COMPACTOR_7000" のように、種類と段が ID に入っている
    private static final Pattern ITEM_ID = Pattern.compile("PERSONAL_(COMPACTOR|DELETOR)_(\\d+)");

    // 段ごとの枠の数
    private static final Map<Integer, Integer> SLOTS = Map.of(4000, 1, 5000, 3, 6000, 7, 7000, 12);

    private static final String COMPACT_PREFIX = "personal_compact_";
    private static final String DELETOR_PREFIX = "personal_deletor_";
    private static final String ACTIVE = "PERSONAL_DELETOR_ACTIVE";

    /** ツールチップに差し込む中身。枠の数だけ並び、空いている枠は null */
    public record Preview(List<String> itemIds, boolean active) implements TooltipComponent {
    }

    private PersonalCompactorPreview() {
    }

    public static void register() {
        ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof Preview preview ? new Grid(preview) : null);
    }

    /**
     * そのアイテムに出すプレビュー。Compactor / Deletor でなければ、元の画像をそのまま返す
     */
    public static Optional<TooltipComponent> tooltipImage(ItemStack stack, Optional<TooltipComponent> original) {
        if (!ModConfig.Interface.enablePersonalCompactorPreview || !GameState.Server.isSkyblock()) return original;

        Preview preview = read(stack);
        return preview == null ? original : Optional.of(preview);
    }

    private static Preview read(ItemStack stack) {
        String id = SkyblockItemId.of(stack);
        if (id == null) return null;

        Matcher matcher = ITEM_ID.matcher(id);
        if (!matcher.matches()) return null;

        Integer slots = SLOTS.get(Integer.parseInt(matcher.group(2)));
        if (slots == null) return null;

        String prefix = matcher.group(1).equals("COMPACTOR") ? COMPACT_PREFIX : DELETOR_PREFIX;
        List<String> items = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            String item = SkyblockItemId.stringAttribute(stack, prefix + slot);
            items.add(item == null || item.isEmpty() ? null : item);
        }

        return new Preview(items, SkyblockItemId.attribute(stack, ACTIVE) == 1);
    }

    /** 枠を並べて描く。見た目はバンドルの中身の一覧に合わせている */
    private static final class Grid implements ClientTooltipComponent {

        private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
        private static final int SLOT_SIZE = 24;
        private static final int ITEM_OFFSET = 4;
        // 1行に並べる数。12 枠のものは 6 枠ずつ 2 行にする
        private static final int MAX_COLUMNS = 7;
        private static final int WRAPPED_COLUMNS = 6;

        private static final int STATUS_GAP = 2;
        private static final int UNKNOWN_COLOR = 0xFFAAAAAA;

        private static final Component ENABLED = Component.literal("Enabled").withColor(0xFF55FF55);
        private static final Component DISABLED = Component.literal("Disabled").withColor(0xFFFF5555);

        private final Preview preview;
        private final int columns;
        private final int rows;

        Grid(Preview preview) {
            this.preview = preview;
            int slots = preview.itemIds().size();
            this.columns = slots <= MAX_COLUMNS ? slots : WRAPPED_COLUMNS;
            this.rows = (slots + columns - 1) / columns;
        }

        private boolean showStatus() {
            return ModConfig.Interface.showPersonalCompactorStatus;
        }

        @Override
        public int getHeight(Font font) {
            int status = showStatus() ? font.lineHeight + STATUS_GAP : 0;
            return status + rows * SLOT_SIZE + STATUS_GAP;
        }

        @Override
        public int getWidth(Font font) {
            int grid = columns * SLOT_SIZE;
            return showStatus() ? Math.max(grid, font.width(status())) : grid;
        }

        private Component status() {
            return Component.literal("Status: ").withColor(0xFFAAAAAA)
                    .append(preview.active() ? ENABLED : DISABLED);
        }

        @Override
        public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
            int top = y;
            if (showStatus()) {
                graphics.text(font, status(), x, top, 0xFFFFFFFF);
                top += font.lineHeight + STATUS_GAP;
            }

            List<String> items = preview.itemIds();
            for (int slot = 0; slot < items.size(); slot++) {
                int slotX = x + (slot % columns) * SLOT_SIZE;
                int slotY = top + (slot / columns) * SLOT_SIZE;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                String id = items.get(slot);
                if (id == null) continue;

                ItemStack icon = SkyblockItemIcons.of(id);
                if (icon == SkyblockItemIcons.LOADING) continue;
                if (icon == null) {
                    // 見た目が分からない品。空き枠と見分けられるよう印だけ置く
                    graphics.centeredText(font, "?", slotX + SLOT_SIZE / 2, slotY + (SLOT_SIZE - font.lineHeight) / 2 + 1,
                            UNKNOWN_COLOR);
                    continue;
                }
                graphics.item(icon, slotX + ITEM_OFFSET, slotY + ITEM_OFFSET);
            }
        }
    }
}
