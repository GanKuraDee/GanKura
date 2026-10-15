package com.deeply.gankura.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * SkyBlock のアイテムのレアリティ。並びは Hypixel の段の順で、後ろほど高い。
 *
 * レアリティは説明文の最終行に "LEGENDARY SWORD" や "MYTHIC ACCESSORY" のように書かれている。
 * Recombobulate した品は前後に飾りの文字が付き、"a MYTHIC ACCESSORY a" のようになる
 */
public enum ItemRarity {
    COMMON("Common", "§f"),
    UNCOMMON("Uncommon", "§a"),
    RARE("Rare", "§9"),
    EPIC("Epic", "§5"),
    LEGENDARY("Legendary", "§6"),
    MYTHIC("Mythic", "§d"),
    DIVINE("Divine", "§b"),
    SPECIAL("Special", "§c"),
    VERY_SPECIAL("Very Special", "§c"),
    ULTIMATE("Ultimate", "§4"),
    ADMIN("Admin", "§4");

    // Recombobulate した品の前後に付く飾り。§k で隠されているが、文字としては "a"
    private static final String RECOMB_MARK = "a ";

    private final String label;
    private final String color;
    // 説明文に書かれている形。"VERY SPECIAL" だけ間が空く
    private final String written;

    ItemRarity(String label, String color) {
        this.label = label;
        this.color = color;
        this.written = name().replace('_', ' ');
    }

    /** 色付きの名前。"§6Legendary" のように出す */
    public String colored() {
        return color + label;
    }

    public boolean atLeast(ItemRarity other) {
        return compareTo(other) >= 0;
    }

    /** 設定画面の選択肢に出す名前 */
    @Override
    public String toString() {
        return label;
    }

    /** そのアイテムのレアリティ。説明文から読めなければ null */
    public static ItemRarity of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        // 最終行のはずだが、後ろに空行が付く品もあるので、下から最初の文字のある行を見る
        List<Component> lines = lore.lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String text = lines.get(i).getString().trim();
            if (text.isEmpty()) continue;
            return parse(text);
        }
        return null;
    }

    private static ItemRarity parse(String line) {
        String text = line.startsWith(RECOMB_MARK) ? line.substring(RECOMB_MARK.length()) : line;

        // "UNCOMMON" を "COMMON" と、"VERY SPECIAL" を "SPECIAL" と取り違えないよう、長い名前から見る
        ItemRarity best = null;
        for (ItemRarity rarity : values()) {
            boolean matches = text.equals(rarity.written) || text.startsWith(rarity.written + " ");
            if (matches && (best == null || rarity.written.length() > best.written.length())) best = rarity;
        }
        return best;
    }
}
