package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class PetHud extends HudElement {
    public PetHud() {
        super("pet", 10, 10, 1.0f, 120, 30, () -> ModConfig.showPetHud, () -> true);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String petText = GameState.Player.activePetName != null ? GameState.Player.activePetName : "§7None";
        context.drawTextWithShadow(tr, "§e§lActive Pet", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, petText, 0, 12, 0xFFFFFFFF);
    }
}