package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.CrimsonHealthHudTarget;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CrimsonBossesHealthHud extends HudElement {

    public CrimsonBossesHealthHud() {
        super("crimson_bosses_health", 230, 238, 1.0f, 185, 24,
                CrimsonBossesHealthHud::isAnyEnabled,
                CrimsonBossesHealthHud::isAnyVisible);
    }

    private static boolean isAnyEnabled() {
        ModConfig.CrimsonIsleCategory c = ModConfig.INSTANCE.crimsonIsle;
        return c.showCrimsonHealthHud && !c.crimsonHealthHudTargets.isEmpty();
    }

    private static boolean isAnyVisible() {
        return GameState.Server.isCrimsonIsle() && firstVisibleBoss() != null;
    }

    // HUD には1体ぶんしか収まらないので、設定リストの並び順を優先順位として使い、
    // 体力を取得できている最初のボスを表示する
    private static CrimsonBossEntry firstVisibleBoss() {
        if (!ModConfig.INSTANCE.crimsonIsle.showCrimsonHealthHud) return null;

        for (CrimsonHealthHudTarget target : ModConfig.INSTANCE.crimsonIsle.crimsonHealthHudTargets) {
            for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
                if (!boss.nameTag().equals(target.nameTag())) continue;
                if (boss.getHealth().get() != null) return boss;
            }
        }
        return null;
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        if (isPreview) {
            text(graphics, font, "§c§lNether Boss HP", 0, 0, 0xFFFFFFFF, true);
            text(graphics, font, "§e30M§f/§a60M", 0, 12, 0xFFFFFFFF, true);
            return;
        }
        CrimsonBossEntry boss = firstVisibleBoss();
        if (boss == null) return;

        text(graphics, font, "§c§l" + titleOf(boss), 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, parseHealthString(boss.getHealth().get()), 0, 12, 0xFFFFFFFF, true);
    }

    // Magma Boss は分裂中(Kill the Magmas)だけタイトルが変わる
    private static String titleOf(CrimsonBossEntry boss) {
        if ("Magma Boss".equals(boss.nameTag()) && GameState.MagmaBoss.healthLabel != null) {
            return GameState.MagmaBoss.healthLabel;
        }
        return boss.nameTag() + " HP";
    }

    private static String parseHealthString(String raw) {
        if (raw == null) return "";
        // サイドバー由来の値は色コード込みで完成しているのでそのまま表示する
        if (raw.startsWith(ModConstants.RAW_HEALTH_PREFIX)) return raw.substring(ModConstants.RAW_HEALTH_PREFIX.length());
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
