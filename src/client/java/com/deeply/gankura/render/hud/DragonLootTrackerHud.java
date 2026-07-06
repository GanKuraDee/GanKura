package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class DragonLootTrackerHud extends HudElement {
    public DragonLootTrackerHud() {
        super("dragonTracker", 200, 200, 1.0f, 150, 36,
                () -> ModConfig.INSTANCE.theEnd.showDragonTrackerHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "§d§lDragon Loot Tracker", 0, 0, 0xFFFFFFFF, true);

        int epicCount = isPreview ? 1 : LootStats.epicDragonPets;
        int legCount = isPreview ? 2 : LootStats.legendaryDragonPets;

        graphics.text(font, String.format("§5Ender Dragon §7(Pet): §f%d", epicCount), 0, 12, 0xFFFFFFFF, true);
        graphics.text(font, String.format("§6Ender Dragon §7(Pet): §f%d", legCount), 0, 24, 0xFFFFFFFF, true);
    }
}