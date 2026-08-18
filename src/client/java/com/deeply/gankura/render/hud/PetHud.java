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
        super("pet", 10, 10, 1.0f, 200, 34, () -> ModConfig.INSTANCE.misc.showPetHud, () -> true);
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

        // 案内文は2行あるので、改行で分けて順に描く
        int y = 12;
        for (String line : petText.split("\n")) {
            drawTextWithShadow(context, tr, line, 0, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}