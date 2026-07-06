package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class GolemLootTrackerHud extends HudElement {
    public GolemLootTrackerHud() {
        super("tracker", 200, 51, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.theEnd.showLootTrackerHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "§6§lGolem Loot Tracker", 0, 0, 0xFFFFFFFF, true);

        int epicPets = isPreview ? 1 : LootStats.epicGolemPets;
        int legPets = isPreview ? 0 : LootStats.legendaryGolemPets;
        int cores = isPreview ? 2 : LootStats.tierBoostCores;

        graphics.text(font, String.format("§5Golem §7(Pet): §f%d", epicPets), 0, 12, 0xFFFFFFFF, true);
        graphics.text(font, String.format("§6Golem §7(Pet): §f%d", legPets), 0, 24, 0xFFFFFFFF, true);
        graphics.text(font, String.format("§6Tier Boost Core: §f%d", cores), 0, 36, 0xFFFFFFFF, true);
    }
}