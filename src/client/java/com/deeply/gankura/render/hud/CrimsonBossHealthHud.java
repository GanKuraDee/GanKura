package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConstants;
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
        String hpText = isPreview ? previewHp : parseHealthString(raw);
        text(graphics, font, titleLabel, 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, hpText, 0, 12, 0xFFFFFFFF, true);
    }

    private String parseHealthString(String raw) {
        if (raw == null) return "";
        if (raw.startsWith(ModConstants.RAW_HEALTH_PREFIX)) return raw.substring(ModConstants.RAW_HEALTH_PREFIX.length());
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
