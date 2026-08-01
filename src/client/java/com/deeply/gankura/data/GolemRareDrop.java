package com.deeply.gankura.data;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * End Stone Protector のレアドロップ。
 * 設定画面のドラッグリスト(Add/ゴミ箱)で選択され、スキャン対象と Loot Tracker HUD の表示行を兼ねる。
 * ドラッグリストは toString() を表示に使うため、ラベルには色コードをそのまま入れている。
 */
public enum GolemRareDrop {
    EPIC_GOLEM_PET("§5Golem §7(Pet)", () -> LootStats.epicGolemPets),
    LEGENDARY_GOLEM_PET("§6Golem §7(Pet)", () -> LootStats.legendaryGolemPets),
    TIER_BOOST_CORE("§6Tier Boost Core", () -> LootStats.tierBoostCores);

    private final String label;
    private final IntSupplier counter;

    GolemRareDrop(String label, IntSupplier counter) {
        this.label = label;
        this.counter = counter;
    }

    public String label() {
        return label;
    }

    public int count() {
        return counter.getAsInt();
    }

    /** 設定の初期値。すべて有効な状態から始める */
    public static List<GolemRareDrop> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
