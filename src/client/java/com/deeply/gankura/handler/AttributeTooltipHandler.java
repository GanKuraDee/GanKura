package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.TooltipText;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attribute のティアを、ローマ数字から数字に直す。
 *
 * ティアは名前の後ろに付いている("Arthropod Fortune IV")。
 * 実際の値はロアに数字で書かれているので、そちらを写す。
 * 名前を読み解かずに済むので、読み違えようがない
 */
public final class AttributeTooltipHandler {

    // ロアに出ている今のティア
    private static final Pattern LEVEL_LINE = Pattern.compile("Attribute Level: ([\\d,]+)");

    // まだ解放していない項目に出る案内
    private static final String UNLOCK_LINE = "to unlock!";

    // 名前の末尾に付いているローマ数字
    private static final Pattern TRAILING_TIER = Pattern.compile("\\s([IVXLCDM]+)$");

    private AttributeTooltipHandler() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
            if (!config.enableAttributeMenuTweaks || !config.enableAttributeTierNumbers) return;
            if (!GameState.Server.isSkyblock() || lines.isEmpty()) return;

            Component name = lines.get(0);
            String level = findLevel(lines);

            if (level == null) {
                // まだ見つけていない項目にはティアが付かないので、0 を足して形をそろえる
                if (!isUndiscovered(lines)) return;
                lines.set(0, TooltipText.appendKeeping(name, name.getString().length(), " 0"));
                return;
            }

            Matcher matcher = TRAILING_TIER.matcher(name.getString());
            if (!matcher.find()) return;

            lines.set(0, TooltipText.appendKeeping(name, matcher.start(), " " + level));
        });
    }

    // まだ解放していない Attribute か。"Syphon 1 shard to unlock!" と案内が出る
    private static boolean isUndiscovered(List<Component> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).getString().contains(UNLOCK_LINE)) return true;
        }
        return false;
    }

    // ロアに書かれているティア。Attribute の項目でなければ null
    private static String findLevel(List<Component> lines) {
        for (int i = 1; i < lines.size(); i++) {
            Matcher matcher = LEVEL_LINE.matcher(lines.get(i).getString());
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }
}
