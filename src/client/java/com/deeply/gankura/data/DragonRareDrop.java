package com.deeply.gankura.data;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * Ender Dragon のレアドロップ。詳細は {@link GolemRareDrop} と同じ。
 */
public enum DragonRareDrop {
    EPIC_DRAGON_PET("§5Ender Dragon §7(Pet)", () -> LootStats.epicDragonPets),
    LEGENDARY_DRAGON_PET("§6Ender Dragon §7(Pet)", () -> LootStats.legendaryDragonPets);

    private final String label;
    private final IntSupplier counter;

    DragonRareDrop(String label, IntSupplier counter) {
        this.label = label;
        this.counter = counter;
    }

    public String label() {
        return label;
    }

    public int count() {
        return counter.getAsInt();
    }

    public static List<DragonRareDrop> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
