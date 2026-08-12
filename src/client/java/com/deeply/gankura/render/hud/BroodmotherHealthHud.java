package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class BroodmotherHealthHud extends HudElement {
    public BroodmotherHealthHud() {
        super("broodmother_health", 10, 148, 1.0f, 120, 24,
                () -> ModConfig.INSTANCE.spidersDen.showBroodmotherHealthHud,
                () -> GameState.Server.isSpidersDen() && GameState.Broodmother.health != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        // Minecraft.getInstance().font を使用
        Font font = Minecraft.getInstance().font;

        String hpText = isPreview ? "§e3,000§f/§a6,000" : parseHealthString(GameState.Broodmother.health);

        // GuiGraphicsExtractor のメソッド形式に修正
        // text(graphics, font, 文字列, x, y, 色, 影の有無)
        text(graphics, font, "§4§lBroodmother HP", 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, hpText, 0, 12, 0xFFFFFFFF, true);
    }

    private String parseHealthString(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("/");
        if (parts.length == 2) {
            double current = parseHealthValue(parts[0]);
            double max = parseHealthValue(parts[1]);
            String colorCode = "§a";

            if (current >= 0 && max > 0) {
                if (current < (max * 0.2)) {
                    colorCode = "§c"; // 20%未満で赤
                } else if (current < (max * 0.5)) {
                    colorCode = "§e"; // 50%未満で黄
                }
            }
            return colorCode + parts[0] + "§f/§a" + parts[1];
        }
        return "§a" + raw.replace("/", "§f/§a");
    }

    private double parseHealthValue(String s) {
        try {
            s = s.trim().replace(",", "");
            if (s.isEmpty()) return 0;
            double multiplier = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'M' || last == 'm') {
                multiplier = 1_000_000.0;
                s = s.substring(0, s.length() - 1);
            } else if (last == 'k' || last == 'K') {
                multiplier = 1_000.0;
                s = s.substring(0, s.length() - 1);
            }
            return Double.parseDouble(s) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}