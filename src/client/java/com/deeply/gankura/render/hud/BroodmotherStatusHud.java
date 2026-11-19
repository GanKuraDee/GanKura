package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class BroodmotherStatusHud extends HudElement {
    public BroodmotherStatusHud() {
        super("broodmother", 230, 10, 1.0f, 150, 24,
                () -> ModConfig.Combat.SpidersDen.showBroodmotherStatusHud,
                () -> GameState.Server.isSpidersDen());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        // Minecraft.getInstance().font を使用
        Font font = Minecraft.getInstance().font;
        String displayStats;

        if (!isPreview && GameState.Broodmother.widgetMissing) {
            text(graphics, font, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF, true);
            missingWidget(graphics, font, 12, "Broodmother", "/widget -> General Info Widget -> Show Broodmother");
            return;
        }

        if (isPreview) {
            displayStats = "Stage: §e4 §e(25s)";
        } else {
            String stage = GameState.Broodmother.stage;

            if ("Alive!".equals(stage)) {
                displayStats = "§cStage: 5 (Spawned)";
            } else if ("Scanning...".equals(stage)) {
                displayStats = "Stage: §8Scanning...";
            } else {
                String num = switch (stage) {
                    case "Slain" -> "§70";
                    case "Dormant" -> "§71";
                    case "Soon" -> "§72";
                    case "Awakening" -> "§73";
                    case "Imminent" -> "§e4";
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;

                if ("Imminent".equals(stage)) displayStats += " " + stage4Suffix();
            }
        }

        text(graphics, font, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, displayStats, 0, 12, 0xFFFFFFFF, true);
    }

    /**
     * Stage 4 の後ろに付ける表示。
     *
     * Stage 4 に変わる瞬間をこのロビーで見ていれば、湧くまでの残り秒数を出す。
     * ロビーに入った時点ですでに Stage 4 だったときや、数え終わってもまだ湧かないときは "(Soon)"
     */
    private static String stage4Suffix() {
        long target = GameState.Broodmother.spawnTargetTick;
        Minecraft client = Minecraft.getInstance();
        if (target > 0 && client.level != null) {
            long ticksLeft = target - GameState.Server.estimatedTicks(client.level.getGameTime());
            if (ticksLeft > 0) return String.format("§e(%ds)", (ticksLeft + 19) / 20);
        }
        return "§e(Soon)";
    }
}