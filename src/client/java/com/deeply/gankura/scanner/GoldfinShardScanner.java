package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goldfin Shard のレベルを Attribute Menu から読む。
 *
 * Golden Fish の湧き待ちが、1レベルにつき30秒短くなる。
 * メニューはいつも開いているわけではないので、読めた値は設定に保存して使い回す。
 * そのため設定画面から手で直すこともできる。
 */
public class GoldfinShardScanner {

    private static final String MENU_TITLE = "Attribute Menu";
    // Goldfin Shard が元になっている項目だけを見る
    private static final String SOURCE = "Source: Goldfin Shard";
    // レベル0のときはこの行自体が出ない
    private static final Pattern LEVEL = Pattern.compile("Attribute Level: (?<level>\\d+)");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GoldfinShardScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (!GameState.Server.isSkyblock()) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!screen.getTitle().getString().contains(MENU_TITLE)) return;

        ScreenHandler menu = screen.getScreenHandler();
        for (int i = 0; i < menu.slots.size(); i++) {
            int level = goldfinLevel(menu.slots.get(i).getStack());
            if (level < 0) continue;

            ModConfig.INSTANCE.fishing.goldfinShardLevel = level;
            ModConfig.INSTANCE.fishing.goldfinShardRead = true;
            return;
        }
    }

    /**
     * Goldfin Shard の項目ならそのレベル。別の項目なら -1。
     *
     * レベルの行が無いときは、まだ上げていないので 0 とする
     */
    private static int goldfinLevel(ItemStack stack) {
        if (stack.isEmpty()) return -1;

        boolean found = false;
        int level = 0;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return -1;

        for (Text line : lore.lines()) {
            String text = ScoreboardUtils.stripColor(ScoreboardUtils.toLegacyString(line));
            if (text.contains(SOURCE)) found = true;

            Matcher matcher = LEVEL.matcher(text);
            if (matcher.find()) level = parseLevel(matcher.group("level"));
        }
        return found ? level : -1;
    }

    private static int parseLevel(String text) {
        try {
            return Math.max(0, Math.min(10, Integer.parseInt(text)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
