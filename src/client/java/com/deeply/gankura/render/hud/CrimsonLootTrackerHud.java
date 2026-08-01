package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.CrimsonRareDrop;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CrimsonLootTrackerHud extends HudElement {
    public CrimsonLootTrackerHud() {
        super("crimson_loot_tracker", 305, 84, 1.0f, 185, 144,
                () -> ModConfig.INSTANCE.crimsonIsle.showCrimsonLootTrackerHud,
                () -> ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                        || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        text(graphics, font, "§c§lNether Boss Loot Tracker", 0, 0, 0xFFFFFFFF, true);

        // 表示する行と並び順は設定画面のドラッグリストに従う
        int y = 12;
        for (CrimsonRareDrop drop : ModConfig.INSTANCE.crimsonIsle.trackedCrimsonDrops) {
            int count = isPreview ? 0 : drop.count();
            text(graphics, font, drop.label() + "§f: §f" + count, 0, y, 0xFFFFFFFF, true);
            y += 12;
        }
    }
}
