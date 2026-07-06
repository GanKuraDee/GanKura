package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class GolemHealthHud extends HudElement {
    public GolemHealthHud() {
        super("health", 200, 106, 1.0f, 100, 24,
                () -> ModConfig.INSTANCE.theEnd.showGolemHealthHud,
                () -> (ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode)) && GameState.Golem.health != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        String hpText = isPreview ? "§e2.4M§f/§a5M" : parseHealthString(GameState.Golem.health);

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "§c§lGolem HP", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, hpText, 0, 12, 0xFFFFFFFF, true);
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
                    colorCode = "§c";
                } else if (current < (max * 0.5)) {
                    colorCode = "§e";
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