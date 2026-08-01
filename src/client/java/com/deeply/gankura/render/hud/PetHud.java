package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class PetHud extends HudElement {
    private static final long LEVEL_UP_DISPLAY_MS = 5000;

    public PetHud() {
        super("pet", 10, 10, 1.0f, 120, 24, () -> ModConfig.INSTANCE.misc.showPetHud, () -> true);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String petText = isPreview
                ? "§7[Pet Lvl] §fPet Name"
                : (GameState.Player.activePetName != null ? GameState.Player.activePetName : "§7None");

        // レベルアップ直後は一定時間だけペット名の横に黄色く表示する
        if (!isPreview && System.currentTimeMillis() - GameState.Player.petLevelUpTime < LEVEL_UP_DISPLAY_MS) {
            petText += " §e(Level up!)";
        }

        drawTextWithShadow(context, tr, "§e§lActive Pet", 0, 0, 0xFFFFFFFF);
        drawTextWithShadow(context, tr, petText, 0, 12, 0xFFFFFFFF);
    }
}