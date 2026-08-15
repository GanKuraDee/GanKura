package com.deeply.gankura.data;

import java.util.List;

/**
 * HP HUD に出す Crimson Isle のボス。
 *
 * ドラッグリストは toString() を表示に使うため、ラベルには色コードをそのまま入れている。
 * HUD は一度に1体しか出せないので、リストの並び順がそのまま表示の優先順位になる。
 */
public enum CrimsonHealthHudTarget {
    BLADESOUL("§8Bladesoul", "Bladesoul"),
    BARBARIAN_DUKE_X("§cBarbarian Duke X", "Barbarian Duke X"),
    MAGE_OUTLAW("§5Mage Outlaw", "Mage Outlaw"),
    ASHFANG("§7Ashfang", "Ashfang"),
    MAGMA_BOSS("§6Magma Boss", "Magma Boss");

    private final String label;
    private final String nameTag;

    CrimsonHealthHudTarget(String label, String nameTag) {
        this.label = label;
        this.nameTag = nameTag;
    }

    /** CrimsonBossEntry と突き合わせるための名前 */
    public String nameTag() {
        return nameTag;
    }

    /** 設定の初期値。すべて表示する状態から始める */
    public static List<CrimsonHealthHudTarget> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
