package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class GolemLootTrackerHud extends HudElement {
    public GolemLootTrackerHud() {
        super("tracker", 260, 100, 1.0f, 150, 50,
                () -> ModConfig.showLootTrackerHud,
                () -> ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, "§6§lGolem Loot Tracker", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, String.format("§5Golem §7(Pet): §f%d", LootStats.epicGolemPets), 0, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, String.format("§6Golem §7(Pet): §f%d", LootStats.legendaryGolemPets), 0, 24, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, String.format("§6Tier Boost Core: §f%d", LootStats.tierBoostCores), 0, 36, 0xFFFFFFFF);
    }
}