package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ArachneHealthHud extends HudElement {
    public ArachneHealthHud() {
        super("arachne_health", 400, 107, 1.0f, 120, 24,
                () -> ModConfig.INSTANCE.spidersDen.showArachneHealthHud,
                () -> GameState.Arachne.inSanctuary && (GameState.Arachne.health != null || GameState.Arachne.broodCount > 0));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        if (isPreview) {
            graphics.text(font, "§5§lArachne HP", 0, 0, 0xFFFFFFFF, true);
            graphics.text(font, "§e3,000§f/§a6,000", 0, 12, 0xFFFFFFFF, true);
            return;
        }

        // Arachne 本体の HP が取得できている間はそちらを優先し、分裂後(HP不明)は Brood の残数を表示する
        if (GameState.Arachne.health != null) {
            graphics.text(font, "§5§lArachne HP", 0, 0, 0xFFFFFFFF, true);
            graphics.text(font, parseHealthString(GameState.Arachne.health), 0, 12, 0xFFFFFFFF, true);
        } else {
            graphics.text(font, "§d§lArachne's Brood", 0, 0, 0xFFFFFFFF, true);
            graphics.text(font, "§fRemaining: §e" + GameState.Arachne.broodCount, 0, 12, 0xFFFFFFFF, true);
        }
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
