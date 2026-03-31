package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class BroodmotherHealthHud extends HudElement {
    public BroodmotherHealthHud() {
        super("broodmother_health", 260, 180, 1.0f, 120, 30,
                () -> ModConfig.INSTANCE.broodmother.showBroodmotherHealthHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map) && GameState.Broodmother.health != null);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        String hpText = isPreview ? "§e3,000§f/§a6,000" : parseHealthString(GameState.Broodmother.health);

        context.drawTextWithShadow(tr, "§4§lBroodmother HP", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, hpText, 0, 12, 0xFFFFFFFF);
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
            // ★修正: 計算する前にカンマ(,)を削除してエラーを防ぐ
            s = s.trim().replace(",", "");
            if (s.isEmpty()) return 0;
            double multiplier = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'M' || last == 'm') { multiplier = 1_000_000.0; s = s.substring(0, s.length() - 1); }
            else if (last == 'k' || last == 'K') { multiplier = 1_000.0; s = s.substring(0, s.length() - 1); }
            return Double.parseDouble(s) * multiplier;
        } catch (NumberFormatException e) { return 0; }
    }
}