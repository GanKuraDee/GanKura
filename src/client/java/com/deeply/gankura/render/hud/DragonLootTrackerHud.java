package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.DragonAlertType;
import com.deeply.gankura.data.DragonRareDrop;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LootStats;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

import java.util.Locale;

public class DragonLootTrackerHud extends HudElement {
    private static final int PREVIEW_FIGHTS = 12;
    private static final double PREVIEW_MAX_DPS = 1_234_500.0;
    private static final double PREVIEW_AVG_DPS = 987_600.0;

    private static final int SECTION_INDENT = 4;

    public DragonLootTrackerHud() {
        super("dragonTracker", 230, 158, 1.0f, 175, 190,
                () -> ModConfig.Combat.TheEnd.showDragonTrackerHud || ModConfig.Combat.TheEnd.showDragonDpsTracker,
                () -> GameState.Server.isTheEnd());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        text(graphics, font, "§d§lDragon Tracker", 0, 0, 0xFFFFFFFF, true);

        int y = 14;
        boolean drewLoot = false;
        DragonRareDrop[] drops = ModConfig.Combat.TheEnd.trackedDragonDrops;
        if (ModConfig.Combat.TheEnd.showDragonTrackerHud && drops.length > 0) {
            text(graphics, font, "§fLoot", 0, y, 0xFFFFFFFF, true);
            y += 12;
            // 表示する行と並び順は設定画面のドラッグリストに従う
            for (DragonRareDrop drop : drops) {
                int count = isPreview ? 0 : drop.count();
                text(graphics, font, drop.label() + "§f: §f" + count, SECTION_INDENT, y, 0xFFFFFFFF, true);
                y += 12;
            }
            drewLoot = true;
        }

        if (ModConfig.Combat.TheEnd.showDragonDpsTracker) {
            if (drewLoot) y += 4;
            text(graphics, font, "§fDPS", 0, y, 0xFFFFFFFF, true);
            y += 12;

            int fights = isPreview ? PREVIEW_FIGHTS : LootStats.dragonFights;
            double maxDps = isPreview ? PREVIEW_MAX_DPS : LootStats.dragonMaxDps;
            double avgDps = isPreview ? PREVIEW_AVG_DPS : LootStats.dragonAverageDps();
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
            y += 12;

            if (ModConfig.Combat.TheEnd.showDragonPerTypeDps) {
                y += 4;
                for (DragonAlertType type : DragonAlertType.values()) {
                    int typeFights;
                    double typeMax;
                    double typeAvg;
                    if (isPreview) {
                        typeFights = previewTypeFights(type);
                        typeMax = previewTypeMaxDps(type);
                        typeAvg = previewTypeAvgDps(type);
                    } else {
                        LootStats.DpsStats stats = LootStats.dragonTypeDps(type);
                        typeFights = stats != null ? stats.fights : 0;
                        typeMax = stats != null ? stats.maxDps : 0.0;
                        typeAvg = stats != null ? stats.averageDps() : 0.0;
                    }
                    String values = typeFights > 0
                            ? LootStats.dpsColor(typeMax) + LootStats.formatDps(typeMax) + " §7/ " + LootStats.dpsColor(typeAvg) + LootStats.formatDps(typeAvg)
                            : "§8-";
                    text(graphics, font,
                            type + " §7(" + String.format(Locale.ROOT, "%,d", typeFights) + ")§f: " + values,
                            SECTION_INDENT, y, 0xFFFFFFFF, true);
                    y += 12;
                }
            }
        }
    }

    private static int previewTypeFights(DragonAlertType type) {
        return switch (type) {
            case PROTECTOR, UNSTABLE, STRONG -> 2;
            case YOUNG -> 3;
            case OLD, WISE, SUPERIOR -> 1;
        };
    }

    private static double previewTypeMaxDps(DragonAlertType type) {
        return switch (type) {
            case PROTECTOR -> 850_000.0;
            case OLD -> 910_000.0;
            case UNSTABLE -> 1_050_000.0;
            case YOUNG -> 1_120_000.0;
            case STRONG -> 1_200_000.0;
            case WISE -> 950_000.0;
            case SUPERIOR -> 1_234_500.0;
        };
    }

    private static double previewTypeAvgDps(DragonAlertType type) {
        return switch (type) {
            case PROTECTOR -> 720_000.0;
            case OLD -> 910_000.0;
            case UNSTABLE -> 980_000.0;
            case YOUNG -> 1_010_000.0;
            case STRONG -> 1_080_000.0;
            case WISE -> 950_000.0;
            case SUPERIOR -> 1_234_500.0;
        };
    }
}