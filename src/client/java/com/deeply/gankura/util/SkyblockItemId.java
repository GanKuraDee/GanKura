package com.deeply.gankura.util;

import com.deeply.gankura.data.AttributeShards;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.Set;

/**
 * アイテムから SkyBlock の ID を取り出す。
 *
 * Hypixel はアイテムの custom_data に ID を入れている。
 * エンチャント本とルーンだけは中身で ID が変わるので、そこから組み立て直す。
 * ペットは種類と段が別に入っているので、こちらも分けて取り出す
 */
public final class SkyblockItemId {

    /** ペットの種類と、書かれている段 */
    public record Pet(String type, String tier) {
    }

    // 1つのアイテムから分かること。まとめて控えておく
    private record Info(String id, Pet pet) {
    }

    private static final Info NOTHING = new Info(null, null);

    // シャードの名前の後ろに付いている飾り。"Snoozle Shard" の形で書かれている
    private static final String SHARD_SUFFIX = " Shard";

    // 直前に読んだアイテム。ツールチップは毎フレーム組み直されるので、読み直しを省く
    private static ItemStack lastStack = null;
    private static Info lastInfo = NOTHING;

    private SkyblockItemId() {
    }

    /** SkyBlock のアイテムでなければ null */
    public static String of(ItemStack stack) {
        return info(stack).id();
    }

    /** ペットでなければ null */
    public static Pet pet(ItemStack stack) {
        return info(stack).pet();
    }

    private static Info info(ItemStack stack) {
        if (stack == lastStack) return lastInfo;

        lastInfo = read(stack);
        lastStack = stack;
        return lastInfo;
    }

    private static Info read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return NOTHING;

        CompoundTag extra = attributes(data.copyTag());
        String id = extra.getStringOr("id", "");
        if (id.isEmpty()) return NOTHING;

        return switch (id) {
            case "PET" -> new Info(id, pet(extra));
            // "ENCHANTMENT_ULTIMATE_WISE_5" のように、入っているエンチャントが ID になる
            case "ENCHANTED_BOOK" -> new Info(only(extra, "enchantments", (name, level) ->
                    "ENCHANTMENT_" + name.toUpperCase(Locale.ROOT) + "_" + level), null);
            // "ANTLERS_RUNE_3" のように、ルーンの名前と段が ID になる
            case "RUNE" -> new Info(only(extra, "runes", (name, level) ->
                    name.toUpperCase(Locale.ROOT) + "_RUNE_" + level), null);
            // シャードはどれも同じ ID なので、名前から引き直す
            case "ATTRIBUTE_SHARD" -> new Info(AttributeShards.idOf(shardName(stack)), null);
            default -> new Info(id, null);
        };
    }

    /**
     * シャードの名前。
     *
     * "Snoozle Shard" のように書かれていて、ダンジョンの箱などでは
     * その後ろに個数まで付く。対応表と突き合わせる分だけを切り出す
     */
    private static String shardName(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name == null) return "";

        int suffix = name.indexOf(SHARD_SUFFIX);
        return (suffix > 0 ? name.substring(0, suffix) : name).trim();
    }

    /**
     * 属性の入っているところ。
     *
     * 今の Hypixel は custom_data の直下に置いている。
     * 昔ながらの ExtraAttributes に入れてくる場合もあるので、あればそちらを見る
     */
    private static CompoundTag attributes(CompoundTag tag) {
        CompoundTag nested = tag.getCompoundOrEmpty("ExtraAttributes");
        return nested.isEmpty() ? tag : nested;
    }

    /**
     * ペットの中身。
     *
     * Hypixel は JSON を書いた文字列で寄越すが、
     * そのまま組み立てて寄越す場合もあるので、どちらでも読めるようにしておく
     */
    private static Pet pet(CompoundTag extra) {
        String written = extra.getStringOr("petInfo", "");
        if (!written.isEmpty()) return fromJson(written);

        CompoundTag nested = extra.getCompoundOrEmpty("petInfo");
        if (nested.isEmpty()) return null;

        return build(nested.getStringOr("type", ""), nested.getStringOr("tier", ""));
    }

    private static Pet fromJson(String written) {
        try {
            JsonObject json = JsonParser.parseString(written).getAsJsonObject();
            return build(text(json, "type"), text(json, "tier"));
        } catch (Exception ignored) {
            // 読めない書き方だった。ペットとして扱わない
            return null;
        }
    }

    private static String text(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
    }

    private static Pet build(String type, String tier) {
        return type.isEmpty() ? null : new Pet(type.toUpperCase(Locale.ROOT), tier.toUpperCase(Locale.ROOT));
    }

    private interface IdBuilder {
        String build(String name, int level);
    }

    /**
     * 中身がちょうど1つのときだけ、そこから ID を組み立てる。
     * 2つ以上入った本は1つの品として売られていないので、諦めて null を返す
     */
    private static String only(CompoundTag extra, String key, IdBuilder builder) {
        CompoundTag contents = extra.getCompoundOrEmpty(key);
        Set<String> names = contents.keySet();
        if (names.size() != 1) return null;

        String name = names.iterator().next();
        int level = contents.getIntOr(name, 0);
        return level <= 0 ? null : builder.build(name, level);
    }
}
