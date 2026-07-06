package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class CrimsonBossesHealthHud extends HudElement {

    public CrimsonBossesHealthHud() {
        super("crimson_bosses_health", 400, 396, 1.0f, 185, 24,
                CrimsonBossesHealthHud::isAnyEnabled,
                CrimsonBossesHealthHud::isAnyVisible);
    }

    private static boolean isAnyEnabled() {
        ModConfig.CrimsonIsleCategory c = ModConfig.INSTANCE.crimsonIsle;
        return c.showBladesoulHealthHud || c.showBarbarianHealthHud
                || c.showMageOutlawHealthHud || c.showAshfangHealthHud
                || c.showMagmaBossHealthHud;
    }

    private static boolean isAnyVisible() {
        if (!ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                && !ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode)) return false;
        for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
            if (isBossEnabled(boss) && boss.getHealth().get() != null) return true;
        }
        return false;
    }

    private static boolean isBossEnabled(CrimsonBossEntry boss) {
        return switch (boss.nameTag()) {
            case "Bladesoul"        -> ModConfig.INSTANCE.crimsonIsle.showBladesoulHealthHud;
            case "Barbarian Duke X" -> ModConfig.INSTANCE.crimsonIsle.showBarbarianHealthHud;
            case "Mage Outlaw"      -> ModConfig.INSTANCE.crimsonIsle.showMageOutlawHealthHud;
            case "Ashfang"          -> ModConfig.INSTANCE.crimsonIsle.showAshfangHealthHud;
            case "Magma Boss"       -> ModConfig.INSTANCE.crimsonIsle.showMagmaBossHealthHud;
            default                 -> false;
        };
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (isPreview) {
            context.drawTextWithShadow(tr, "§c§lNether Boss HP", 0, 0, 0xFFFFFFFF);
            context.drawTextWithShadow(tr, "§e30M§f/§a60M", 0, 12, 0xFFFFFFFF);
            return;
        }
        for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
            if (!isBossEnabled(boss)) continue;
            String raw = boss.getHealth().get();
            if (raw == null) continue;
            boolean isBar = raw.startsWith("BAR:");
            String hpText = parseHealthString(raw);
            context.drawTextWithShadow(tr, "§c§l" + boss.nameTag() + " HP", 0, 0, 0xFFFFFFFF);
            if (isBar) {
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(0.5f, 0.5f);
                context.drawTextWithShadow(tr, hpText, 0, 24, 0xFFFFFFFF);
                context.getMatrices().popMatrix();
            } else {
                context.drawTextWithShadow(tr, hpText, 0, 12, 0xFFFFFFFF);
            }
            return;
        }
    }

    private static String parseHealthString(String raw) {
        if (raw == null) return "";
        if (raw.startsWith("BAR:")) return raw.substring(4);
        String[] parts = raw.split("/");
        if (parts.length == 2) {
            double current = parseHealthValue(parts[0]);
            double max = parseHealthValue(parts[1]);
            String color = "§a";
            if (current >= 0 && max > 0) {
                if (current < max * 0.2) color = "§c";
                else if (current < max * 0.5) color = "§e";
            }
            return color + parts[0] + "§f/§a" + parts[1];
        }
        return "§a" + raw.replace("/", "§f/§a");
    }

    private static double parseHealthValue(String s) {
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
