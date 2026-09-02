package com.deeply.gankura.handler;

import com.deeply.gankura.data.EnchantData;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.BestiaryMenu;
import com.deeply.gankura.util.TooltipText;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bestiary のティアを、ローマ数字から数字に直す。
 *
 * ティアはモブ名の後ろに足されている("Barbarian Duke X XV")。
 * まだ解放していない一族はティアが付かないので、代わりに 0 を足して、
 * どの行も同じ形で読めるようにする
 */
public final class BestiaryTooltipHandler {

    // 解放前の一族に出る断り書き
    private static final String LOCKED_LINE = "You haven't unlocked this Family yet!";

    // 名前の末尾に付いているローマ数字
    private static final Pattern TRAILING_TIER = Pattern.compile("\\s([IVXLCDM]+)$");

    private BestiaryTooltipHandler() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
            if (!config.enableBestiaryMenuTweaks || !config.enableBestiaryTierNumbers) return;
            if (!GameState.Server.isSkyblock() || !BestiaryMenu.isOpen()) return;
            if (lines.isEmpty()) return;

            Component renamed = rename(lines.get(0), isLocked(lines));
            if (renamed != null) lines.set(0, renamed);
        });
    }

    private static boolean isLocked(List<Component> lines) {
        for (Component line : lines) {
            if (line.getString().contains(LOCKED_LINE)) return true;
        }
        return false;
    }

    /**
     * 名前の行を書き換える。直すところが無ければ null を返す。
     *
     * 解放前はティアが付いていないので 0 を足すだけにする。
     * "Barbarian Duke X" のように名前がローマ数字で終わるモブがいるため、
     * 解放前の行では数字への読み替えを行わない
     */
    private static Component rename(Component name, boolean locked) {
        String text = name.getString();

        if (locked) {
            return TooltipText.appendKeeping(name, text.length(), " 0");
        }

        Matcher matcher = TRAILING_TIER.matcher(text);
        if (!matcher.find()) return null;

        int tier = EnchantData.romanToInt(matcher.group(1));
        if (tier <= 0) return null;

        // 末尾のローマ数字を落として、同じ書式のまま数字を足し直す
        return TooltipText.appendKeeping(name, matcher.start(), " " + tier);
    }
}
