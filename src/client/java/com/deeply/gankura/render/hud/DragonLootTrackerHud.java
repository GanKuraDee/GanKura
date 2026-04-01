package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class DragonLootTrackerHud extends HudElement {
    public DragonLootTrackerHud() {
        super("dragonTracker", 10, 190, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.dragon.showDragonTrackerHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, "§d§lDragon Loot Tracker", 0, 0, 0xFFFFFFFF);
        int epicCount = isPreview ? 1 : LootStats.epicDragonPets;
        int legCount = isPreview ? 2 : LootStats.legendaryDragonPets;
        context.drawTextWithShadow(tr, String.format("§5Ender Dragon §7(Pet): §f%d", epicCount), 0, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, String.format("§6Ender Dragon §7(Pet): §f%d", legCount), 0, 24, 0xFFFFFFFF);
    }
}