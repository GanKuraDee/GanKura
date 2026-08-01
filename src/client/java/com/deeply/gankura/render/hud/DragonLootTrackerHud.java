package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.DragonRareDrop;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class DragonLootTrackerHud extends HudElement {
    public DragonLootTrackerHud() {
        super("dragonTracker", 165, 166, 1.0f, 150, 36,
                () -> ModConfig.INSTANCE.theEnd.showDragonTrackerHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        drawTextWithShadow(context, tr, "§d§lDragon Loot Tracker", 0, 0, 0xFFFFFFFF);
        // 表示する行と並び順は設定画面のドラッグリストに従う
        int y = 12;
        for (DragonRareDrop drop : ModConfig.INSTANCE.theEnd.trackedDragonDrops) {
            int count = isPreview ? 0 : drop.count();
            drawTextWithShadow(context, tr, drop.label() + "§f: §f" + count, 0, y, 0xFFFFFFFF);
            y += 12;
        }
    }
}