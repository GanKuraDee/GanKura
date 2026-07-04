package com.deeply.gankura.render.hud;

import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

public class CrimsonBossHealthHud extends HudElement {
    private final String titleLabel;
    private final String previewHp;
    private final Supplier<String> healthSupplier;

    public CrimsonBossHealthHud(String id, int defaultX, int defaultY,
            String titleLabel, String previewHp,
            Supplier<Boolean> enabledSupplier,
            Supplier<Boolean> visibleSupplier,
            Supplier<String> healthSupplier) {
        super(id, defaultX, defaultY, 1.0f, 160, 30, enabledSupplier, visibleSupplier);
        this.titleLabel = titleLabel;
        this.previewHp = previewHp;
        this.healthSupplier = healthSupplier;
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        String raw = isPreview ? null : healthSupplier.get();
        boolean isBar = !isPreview && raw != null && raw.startsWith("BAR:");
        String hpText = isPreview ? previewHp : parseHealthString(raw);
        graphics.text(font, titleLabel, 0, 0, 0xFFFFFFFF, true);
        if (isBar) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5f, 0.5f);
            // 0.5 スケールのため、画面上の y=12 に表示するには y=24 を指定
            graphics.text(font, hpText, 0, 24, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        } else {
            graphics.text(font, hpText, 0, 12, 0xFFFFFFFF, true);
        }
    }

    private String parseHealthString(String raw) {
        if (raw == null) return "";
        if (raw.startsWith("BAR:")) {
            return raw.substring(4);
        }
        String[] parts = raw.split("/");
        if (parts.length == 2) {
            double current = parseHealthValue(parts[0]);
            double max = parseHealthValue(parts[1]);
            String color = "§a";
            if (current >= 0 && max > 0) {
                if (current < (max * 0.2)) color = "§c";
                else if (current < (max * 0.5)) color = "§e";
            }
            return color + parts[0] + "§f/§a" + parts[1];
        }
        return "§a" + raw.replace("/", "§f/§a");
    }

    private double parseHealthValue(String s) {
        try {
            s = s.trim().replace(",", "");
            if (s.isEmpty()) return 0;
            double mult = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'M' || last == 'm') { mult = 1_000_000.0; s = s.substring(0, s.length() - 1); }
            else if (last == 'k' || last == 'K') { mult = 1_000.0; s = s.substring(0, s.length() - 1); }
            return Double.parseDouble(s) * mult;
        } catch (NumberFormatException e) { return 0; }
    }
}
