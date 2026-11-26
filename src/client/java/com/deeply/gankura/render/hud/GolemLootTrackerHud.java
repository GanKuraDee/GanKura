package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.GolemRareDrop;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

import java.util.Locale;

public class GolemLootTrackerHud extends HudElement {
    private static final int PREVIEW_FIGHTS = 12;
    private static final double PREVIEW_MAX_DPS = 1_234_500.0;
    private static final double PREVIEW_AVG_DPS = 987_600.0;

    private static final int SECTION_INDENT = 4;

    public GolemLootTrackerHud() {
        super("tracker", 230, 50, 1.0f, 150, 114,
                () -> ModConfig.Combat.TheEnd.showLootTrackerHud || ModConfig.Combat.TheEnd.showGolemDpsTracker,
                () -> GameState.Server.isTheEnd());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        text(graphics, font, "§6§lGolem Tracker", 0, 0, 0xFFFFFFFF, true);

        int y = 14;
        boolean drewLoot = false;
        GolemRareDrop[] drops = ModConfig.Combat.TheEnd.trackedGolemDrops;
        if (ModConfig.Combat.TheEnd.showLootTrackerHud && drops.length > 0) {
            text(graphics, font, "§fLoot", 0, y, 0xFFFFFFFF, true);
            y += 12;
            // 表示する行と並び順は設定画面のドラッグリストに従う
            for (GolemRareDrop drop : drops) {
                int count = isPreview ? 0 : drop.count();
                text(graphics, font, drop.label() + "§f: §f" + count, SECTION_INDENT, y, 0xFFFFFFFF, true);
                y += 12;
            }
            drewLoot = true;
        }

        if (ModConfig.Combat.TheEnd.showGolemDpsTracker) {
            if (drewLoot) y += 4;
            text(graphics, font, "§fDPS", 0, y, 0xFFFFFFFF, true);
            y += 12;

            int fights = isPreview ? PREVIEW_FIGHTS : LootStats.golemFights;
            double maxDps = isPreview ? PREVIEW_MAX_DPS : LootStats.golemMaxDps;
            double avgDps = isPreview ? PREVIEW_AVG_DPS : LootStats.golemAverageDps();
            String maxText = fights > 0
                    ? LootStats.dpsColor(maxDps) + LootStats.formatDps(maxDps)
                    : "§8-";
            String avgText = fights > 0
                    ? LootStats.dpsColor(avgDps) + LootStats.formatDps(avgDps)
                    : "§8-";

            text(graphics, font, "§7Fights§f: §f" + String.format(Locale.ROOT, "%,d", fights), SECTION_INDENT, y, 0xFFFFFFFF, true);
            y += 12;
            text(graphics, font, "§7Max DPS§f: " + maxText, SECTION_INDENT, y, 0xFFFFFFFF, true);
            y += 12;
            text(graphics, font, "§7Avg DPS§f: " + avgText, SECTION_INDENT, y, 0xFFFFFFFF, true);
        }
    }
}