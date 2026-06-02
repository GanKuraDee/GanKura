package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class CrimsonLootTrackerHud extends HudElement {
    public CrimsonLootTrackerHud() {
        super("crimson_loot_tracker", 10, 350, 1.0f, 185, 144,
                () -> ModConfig.INSTANCE.crimsonIsle.showCrimsonLootTrackerHud,
                () -> ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                        || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, "§c§lNether Boss Loot Tracker", 0, 0, 0xFFFFFFFF);
        int y = 12;
        // §9 = 5555FF, §5 = AA00AA, §6 = FFAA00
        context.drawTextWithShadow(tr, fmt("§9Kuudra Key",          isPreview ? 0 : LootStats.kuudraKeys),          0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Hot Kuudra Key",       isPreview ? 0 : LootStats.hotKuudraKeys),       0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Magma Urchin",         isPreview ? 0 : LootStats.magmaUrchins),        0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§9Ragnarock Axe",        isPreview ? 0 : LootStats.ragnarockAxes),       0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Fire Veil Wand",       isPreview ? 0 : LootStats.fireVeilWands),       0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Fire Freeze Staff",    isPreview ? 0 : LootStats.fireFreezeStaffs),    0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Wand Of Strength",     isPreview ? 0 : LootStats.wandsOfStrength),     0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Flaming Fist",         isPreview ? 0 : LootStats.flamingFists),        0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Fire Fury Staff",      isPreview ? 0 : LootStats.fireFuryStaffs),      0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§5Magma Cube §7(Pet)",   isPreview ? 0 : LootStats.epicMagmaCubePets),   0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, fmt("§6Magma Cube §7(Pet)",   isPreview ? 0 : LootStats.legendaryMagmaCubePets), 0, y, 0xFFFFFFFF);
    }

    private static String fmt(String label, int count) {
        return label + "§f: §f" + count;
    }
}
