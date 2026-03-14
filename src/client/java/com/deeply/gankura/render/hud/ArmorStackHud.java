package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ArmorStackHud extends HudElement {
    public ArmorStackHud() {
        super("armorStack", 10, 50, 1.0f, 150, 15, () -> ModConfig.showArmorStackHud, () -> true);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int spacing = 8;
        List<String> parts = new ArrayList<>();

        if (isPreview) {
            parts.add("§6§l10ᝐ"); parts.add("§15⁑"); parts.add("§e§l8⚶"); parts.add("§23҉"); parts.add("§9§l2Ѫ");
        } else {
            if (GameState.Player.crimsonStack > 0) parts.add((GameState.Player.isCrimsonBold ? "§6§l" : "§6") + GameState.Player.crimsonStack + "ᝐ");
            if (GameState.Player.terrorStack > 0) parts.add((GameState.Player.isTerrorBold ? "§1§l" : "§1") + GameState.Player.terrorStack + "⁑");
            if (GameState.Player.hollowStack > 0) parts.add((GameState.Player.isHollowBold ? "§e§l" : "§e") + GameState.Player.hollowStack + "⚶");
            if (GameState.Player.fervorStack > 0) parts.add((GameState.Player.isFervorBold ? "§2§l" : "§2") + GameState.Player.fervorStack + "҉");
            if (GameState.Player.auroraStack > 0) parts.add((GameState.Player.isAuroraBold ? "§9§l" : "§9") + GameState.Player.auroraStack + "Ѫ");
        }

        if (parts.isEmpty()) return;
        int totalWidth = parts.stream().mapToInt(tr::getWidth).sum() + (parts.size() - 1) * spacing;
        int currentX = (150 / 2) - (totalWidth / 2); // 0基準での中央揃え

        for (String part : parts) {
            context.drawTextWithShadow(tr, part, currentX, 0, 0xFFFFFFFF);
            currentX += tr.getWidth(part) + spacing;
        }
    }
}