package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font; // 旧 TextRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor; // 提供されたソースに基づく

import java.util.ArrayList;
import java.util.List;

public class ArmorStackHud extends HudElement {
    public ArmorStackHud() {
        super("armorStack", 400, 39, 1.0f, 150, 15, () -> ModConfig.INSTANCE.misc.showArmorStackHud, () -> true);
    }

    @Override
    // 引数の型を GuiGraphicsExtractor に変更
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
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

        // font.width() メソッドを使用
        int totalWidth = parts.stream().mapToInt(font::width).sum() + (parts.size() - 1) * spacing;
        int currentX = (150 / 2) - (totalWidth / 2);

        for (String part : parts) {
            /*
             * GuiGraphicsExtractor のメソッド:
             * public void text(Font font, String str, int x, int y, int color, boolean dropShadow)
             */
            graphics.text(font, part, currentX, 0, 0xFFFFFFFF, true);
            currentX += font.width(part) + spacing;
        }
    }
}