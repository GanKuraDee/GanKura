package com.deeply.gankura.util;

import com.deeply.gankura.data.EnchantData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bestiary と Attribute の、今のティアを読み取る。
 *
 * どちらもツールチップと枠の両方で使うので、読み方をここにまとめておく
 */
public final class TierText {

    // 名前の末尾に付いているローマ数字
    public static final Pattern TRAILING_TIER = Pattern.compile("\\s([IVXLCDM]+)$");

    // Bestiary で、まだ解放していない一族に出る断り書き
    public static final String BESTIARY_LOCKED = "You haven't unlocked this Family yet!";
    // Bestiary の進み具合。数値が続くものだけを数える
    public static final Pattern BESTIARY_PROGRESS = Pattern.compile("Overall Progress:\\s*[\\d.,]+%");

    // Attribute の今のティアと、まだ見つけていない項目に出る案内
    public static final Pattern ATTRIBUTE_LEVEL = Pattern.compile("Attribute Level:\\s*([\\d,]+)");
    public static final String ATTRIBUTE_UNLOCK = "to unlock!";

    // 打ち止めの印。"Attribute Level: 10 (MAX!)" のように添えられる
    public static final String MAX_MARK = "MAX";

    // ペットの名前に付いているレベル("[Lvl 91] Orchid Mantis")
    public static final Pattern PET_LEVEL = Pattern.compile("\\[Lvl (\\d+)]");
    // Heart of the Forest / Mountain の項目に書かれているレベルと、入り切りの状態。
    // 上限まで上げた項目は "Level 50" と、分母を付けずに書かれる
    public static final Pattern HEART_LEVEL = Pattern.compile("Level\\s+([\\d,]+)");
    public static final String HEART_ENABLED = "ENABLED";
    public static final String HEART_DISABLED = "DISABLED";

    // Hunting Box のシャードに書かれている所持数("Owned: 72 Shards")
    public static final Pattern SHARDS_OWNED = Pattern.compile("Owned:\\s*([\\d,]+)\\s*Shards?");

    // ペットのロアに出る、育ちきった印と育ち具合
    public static final String PET_MAX = "MAX LEVEL";
    public static final String PET_PROGRESS = "Progress to Level";
    // 今出しているペットにだけ書かれる行。他のペットには "Left-click to summon!" と書かれる
    public static final String PET_DESPAWN = "to despawn!";

    private TierText() {
    }

    /**
     * Bestiary の項目の今のティア。項目でなければ null。
     *
     * ティアは名前の後ろに付いている("Thunder XII")。
     * 解放前は付かないので 0 として扱う
     */
    public static Integer bestiaryTier(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return null;

        boolean entry = false;
        for (Component line : lore.lines()) {
            String text = line.getString();

            if (text.contains(BESTIARY_LOCKED)) return 0;
            if (BESTIARY_PROGRESS.matcher(text).find()) entry = true;
        }
        if (!entry) return null;

        Matcher matcher = TRAILING_TIER.matcher(stack.getHoverName().getString());
        if (!matcher.find()) return null;

        int tier = EnchantData.romanToInt(matcher.group(1));
        return tier > 0 ? tier : null;
    }

    /**
     * Attribute の項目の今のティア。項目でなければ null。
     *
     * ロアに数字で書かれているので、そのまま読む。
     * まだ見つけていない項目は 0 として扱う
     */
    public static Integer attributeTier(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return null;

        boolean undiscovered = false;
        for (Component line : lore.lines()) {
            String text = line.getString();

            Matcher matcher = ATTRIBUTE_LEVEL.matcher(text);
            if (matcher.find()) return parse(matcher.group(1));
            if (text.contains(ATTRIBUTE_UNLOCK)) undiscovered = true;
        }
        return undiscovered ? 0 : null;
    }

    /**
     * その項目が打ち止めか。
     *
     * 進み具合の行、または今のティアの行に "(MAX!)" と添えられる
     */
    public static boolean isMaxed(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            String text = line.getString();
            if (!text.contains(MAX_MARK)) continue;

            if (ATTRIBUTE_LEVEL.matcher(text).find()) return true;
            if (BESTIARY_PROGRESS.matcher(text).find()) return true;
        }
        return false;
    }

    /**
     * そのシャードを今いくつ持っているか。シャードでなければ null。
     *
     * Hunting Box のロアにしか書かれていないので、そのまま読む
     */
    public static Integer shardsOwned(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return null;

        for (Component line : lore.lines()) {
            Matcher matcher = SHARDS_OWNED.matcher(line.getString());
            if (matcher.find()) return parse(matcher.group(1));
        }
        return null;
    }

    /**
     * Heart of the Forest / Mountain の項目の今のレベル。
     *
     * 未解放の項目にも "Level 1/50" と書かれるが、そちらは解放したら付く値で、
     * 今のレベルではない。解放済みの印である入り切りの行があるものだけ読む。
     * 解放済みでもレベルを持たない項目があり、その場合は null
     */
    public static Integer heartLevel(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return null;

        Integer level = null;
        boolean unlocked = false;

        for (Component line : lore.lines()) {
            String text = line.getString();

            // 2つ目のレベルは "=====[ UPGRADE ]=====" の下に出る、上げた後の値
            if (level == null) {
                Matcher matcher = HEART_LEVEL.matcher(text);
                if (matcher.find()) level = parse(matcher.group(1));
            }
            if (heartStatus(text) != null) unlocked = true;
        }
        return unlocked ? level : null;
    }

    /** Heart of the Forest / Mountain の項目が入っているか。未解放の項目なら null */
    public static Boolean heartEnabled(ItemStack stack) {
        ItemLore lore = lore(stack);
        if (lore == null) return null;

        for (Component line : lore.lines()) {
            Boolean status = heartStatus(line.getString());
            if (status != null) return status;
        }
        return null;
    }

    /** その行が示す入り切り。状態の行でなければ null */
    private static Boolean heartStatus(String text) {
        String plain = ChatFormatting.stripFormatting(text);
        if (plain == null) return null;

        plain = plain.trim();
        if (HEART_ENABLED.equals(plain)) return Boolean.TRUE;
        if (HEART_DISABLED.equals(plain)) return Boolean.FALSE;
        return null;
    }

    /**
     * ペットの今のレベル。ペットでなければ null。
     *
     * レベルは名前の頭に付いているが、他の品にも同じ書き方のものがあり得るので、
     * ペットにしか出ないロアの行と揃っているときだけ読む
     */
    public static Integer petLevel(ItemStack stack) {
        if (!isPet(stack)) return null;

        Matcher matcher = PET_LEVEL.matcher(stack.getHoverName().getString());
        return matcher.find() ? parse(matcher.group(1)) : null;
    }

    /** そのペットが育ちきっているか */
    public static boolean isPetMaxed(ItemStack stack) {
        return hasLine(stack, PET_MAX);
    }

    private static boolean isPet(ItemStack stack) {
        return hasLine(stack, PET_MAX) || hasLine(stack, PET_PROGRESS);
    }

    private static boolean hasLine(ItemStack stack, String mark) {
        ItemLore lore = lore(stack);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            if (line.getString().contains(mark)) return true;
        }
        return false;
    }

    private static ItemLore lore(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(DataComponents.LORE);
    }

    private static Integer parse(String number) {
        try {
            return Integer.parseInt(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
