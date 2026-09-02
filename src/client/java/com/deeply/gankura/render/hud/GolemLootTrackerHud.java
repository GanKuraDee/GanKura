package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.GolemRareDrop;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class GolemLootTrackerHud extends HudElement {
    public GolemLootTrackerHud() {
        super("tracker", 230, 50, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.combat.theEnd.showLootTrackerHud,
                () -> GameState.Server.isTheEnd());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        text(graphics, font, "§6§lGolem Loot Tracker", 0, 0, 0xFFFFFFFF, true);

        // 表示する行と並び順は設定画面のドラッグリストに従う
        int y = 12;
        for (GolemRareDrop drop : ModConfig.INSTANCE.combat.theEnd.trackedGolemDrops) {
            int count = isPreview ? 0 : drop.count();
            text(graphics, font, drop.label() + "§f: §f" + count, 0, y, 0xFFFFFFFF, true);
            y += 12;
        }
    }
}