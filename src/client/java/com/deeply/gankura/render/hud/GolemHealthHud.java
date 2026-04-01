package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class GolemHealthHud extends HudElement {
    public GolemHealthHud() {
        super("health", 260, 150, 1.0f, 100, 30,
                () -> ModConfig.INSTANCE.golem.showGolemHealthHud,
                () -> (ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode)) && GameState.Golem.health != null);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String hpText = isPreview ? "§e2.4M§f/§a5M" : parseHealthString(GameState.Golem.health);
        context.drawTextWithShadow(tr, "§c§lGolem HP", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, hpText, 0, 12, 0xFFFFFFFF);
    }

    private String parseHealthString(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("/");
        if (parts.length == 2) {
            double current = parseHealthValue(parts[0]);
            double max = parseHealthValue(parts[1]);
            String colorCode = "§a"; // デフォルトは緑(§a)

            if (current >= 0 && max > 0) {
                // ★修正: 固定値(1M)ではなく、Broodmotherと同じ割合(20%・50%)の判定に変更
                if (current < (max * 0.2)) {
                    colorCode = "§c"; // 20%未満で赤色(§c)
                } else if (current < (max * 0.5)) {
                    colorCode = "§e"; // 50%未満で黄色(§e)
                }
            }
            return colorCode + parts[0] + "§f/§a" + parts[1];
        }
        return "§a" + raw.replace("/", "§f/§a");
    }

    private double parseHealthValue(String s) {
        try {
            s = s.trim().replace(",", "");;
            if (s.isEmpty()) return 0;
            double multiplier = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'M' || last == 'm') { multiplier = 1_000_000.0; s = s.substring(0, s.length() - 1); }
            else if (last == 'k' || last == 'K') { multiplier = 1_000.0; s = s.substring(0, s.length() - 1); }
            return Double.parseDouble(s) * multiplier;
        } catch (NumberFormatException e) { return 0; }
    }
}