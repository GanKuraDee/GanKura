package com.deeply.gankura.data;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * Nether Boss のレアドロップ。詳細は {@link GolemRareDrop} と同じ。
 *
 * <p>{@code itemName} はネームタグとの部分一致に使う文字列。ペット類は色で
 * Epic / Legendary を判別する必要があるため {@code itemName} を持たず、
 * 専用の判定を通す。
 *
 * <p>{@code boss} が null のドロップは全ボス共通、それ以外は該当ボス撃破時のみ対象。
 */
public enum CrimsonRareDrop {
    KUUDRA_KEY("§9Kuudra Key", "Kuudra Key", null, () -> LootStats.kuudraKeys, LootStats::addKuudraKey),
    HOT_KUUDRA_KEY("§5Hot Kuudra Key", "Hot Kuudra Key", null, () -> LootStats.hotKuudraKeys, LootStats::addHotKuudraKey),
    MAGMA_URCHIN("§5Magma Urchin", "Magma Urchin", null, () -> LootStats.magmaUrchins, LootStats::addMagmaUrchin),
    RAGNAROCK("§9Ragnarock", "Ragnarock", "BLADESOUL", () -> LootStats.ragnarockAxes, LootStats::addRagnarockAxe),
    FIRE_VEIL_WAND("§5Fire Veil Wand", "Fire Veil Wand", "ASHFANG", () -> LootStats.fireVeilWands, LootStats::addFireVeilWand),
    FIRE_FREEZE_STAFF("§5Fire Freeze Staff", "Fire Freeze Staff", "MAGE OUTLAW", () -> LootStats.fireFreezeStaffs, LootStats::addFireFreezeStaff),
    WAND_OF_STRENGTH("§5Wand of Strength", "Wand of Strength", "MAGE OUTLAW", () -> LootStats.wandsOfStrength, LootStats::addWandOfStrength),
    FLAMING_FIST("§5Flaming Fist", "Flaming Fist", "BARBARIAN DUKE X", () -> LootStats.flamingFists, LootStats::addFlamingFist),
    FIRE_FURY_STAFF("§5Fire Fury Staff", "Fire Fury Staff", "MAGMA BOSS", () -> LootStats.fireFuryStaffs, LootStats::addFireFuryStaff),
    EPIC_MAGMA_CUBE_PET("§7[Lvl 1] §5Magma Cube", null, "MAGMA BOSS", () -> LootStats.epicMagmaCubePets, LootStats::addEpicMagmaCubePet),
    LEGENDARY_MAGMA_CUBE_PET("§7[Lvl 1] §6Magma Cube", null, "MAGMA BOSS", () -> LootStats.legendaryMagmaCubePets, LootStats::addLegendaryMagmaCubePet);

    private final String label;
    private final String itemName;
    private final String boss;
    private final IntSupplier counter;
    private final Runnable increment;

    CrimsonRareDrop(String label, String itemName, String boss, IntSupplier counter, Runnable increment) {
        this.label = label;
        this.itemName = itemName;
        this.boss = boss;
        this.counter = counter;
        this.increment = increment;
    }

    public String label() {
        return label;
    }

    public String itemName() {
        return itemName;
    }

    /** 撃破したボスのドロップとして扱うか。共通ドロップはどのボスでも対象 */
    public boolean matchesBoss(String killedBoss) {
        return boss == null || boss.equals(killedBoss);
    }

    public int count() {
        return counter.getAsInt();
    }

    public void increment() {
        increment.run();
    }

    public static List<CrimsonRareDrop> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
