package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.GoldenFishHandler;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

// Golden Fish の湧き待ちと、湧いている間の残り時間
public class GoldenFishHud extends HudElement {

    // Goldfin Shard の上限。これ以上は上がらない
    private static final int MAX_SHARD_LEVEL = 10;

    public GoldenFishHud() {
        super("golden_fish", 10, 110, 1.0f, 260, 64,
                () -> ModConfig.INSTANCE.fishing.showGoldenFishTimer,
                () -> !lines(false).isEmpty());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        int y = 0;
        for (String line : lines(isPreview)) {
            text(graphics, font, line, 0, y, 0xFFFFFFFF, true);
            y += 12;
        }
    }

    private static List<String> lines(boolean isPreview) {
        // Crimson Isle で溶岩用の竿を持っているときだけ釣れる
        if (!isPreview && !GoldenFishHandler.isActive()) return List.of();

        List<String> lines = new ArrayList<>();
        lines.add("§6§lGolden Fish");

        if (isPreview) {
            lines.add("§7Can spawn since: §b1:05");
            lines.add("§7Chance: §b26%");
            lines.add(goldfinLine(MAX_SHARD_LEVEL));
            return lines;
        }

        long despawn = GoldenFishHandler.despawnRemaining();
        if (despawn > 0) {
            lines.add("§7Despawns in: §b" + time(despawn));
            int interactions = GoldenFishHandler.interactions();
            String color = interactions >= GoldenFishHandler.MAX_INTERACTIONS ? "§a" : "§b";
            lines.add("§7Interactions: " + color + interactions + "/" + GoldenFishHandler.MAX_INTERACTIONS);
            return lines;
        }

        long spawn = GoldenFishHandler.spawnRemaining();
        // 竿を溶岩に投げるまでは数えようがないので、何も出さない
        if (spawn < 0) return List.of();

        if (spawn > 0) {
            lines.add("§7Can spawn in: §b" + time(spawn));
        } else {
            lines.add("§7Can spawn since: §b" + time(GoldenFishHandler.availableFor()));
            lines.add(String.format("§7Chance: §b%.0f%%", GoldenFishHandler.chance() * 100));
        }

        long rod = GoldenFishHandler.rodRemaining();
        if (rod >= 0) lines.add("§7Throw rod in: §b" + time(rod));

        lines.add(goldfinLine(ModConfig.INSTANCE.fishing.goldfinShardLevel));
        return lines;
    }

    /**
     * 短縮の元にしているレベル。読み取れているか確かめられるように出す。
     *
     * 何秒短くなっているかまで出すと、レベルを上げた効果が分かりやすい
     */
    private static String goldfinLine(int level) {
        // レベル0と見分けられないと、短縮を見落としていることに気づけない
        if (!ModConfig.INSTANCE.fishing.goldfinShardRead) {
            return "§7Goldfin Shard Level: §cUnknown §8(open the Attribute Menu)";
        }

        String suffix = level >= MAX_SHARD_LEVEL ? " §a(Max)" : "";
        return "§7Goldfin Shard Level: §b" + level + suffix
                + " §8(-" + time(level * GoldenFishHandler.SHARD_BONUS_MS) + ")";
    }

    private static String time(long millis) {
        long seconds = millis / 1000;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
