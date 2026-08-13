package com.deeply.gankura.data;

import java.util.List;

/**
 * Highlight / Tracer / Nameplate の対象モブ。
 * 設定画面ではこの3機能ごとにドラッグリストを持ち、リストに載っているモブだけが対象になる。
 * ドラッグリストは toString() を表示に使うため、ラベルには色コードをそのまま入れている。
 *
 * 色は Glowing の輪郭(RGB)と Tracer の線(ARGB)で別々に持つ。ARGB 側は不透明固定。
 */
public enum MobVisualTarget {
    GOLEM("§6End Stone Protector", 0xFFAA00),
    DRAGON("§dDragon", 0xFF55FF),
    BROODMOTHER("§cBroodmother", 0xFF5555),
    ARACHNE("§5Arachne", 0xAA00AA),
    ARACHNE_BROOD("§dArachne's Brood", 0xFF55FF),
    BARBARIAN_DUKE_X("§cBarbarian Duke X", 0xFF5555),
    BLADESOUL("§8Bladesoul", 0x555555),
    MAGE_OUTLAW("§5Mage Outlaw", 0xAA00AA),
    ASHFANG("§7Ashfang", 0xAAAAAA),
    ASHFANG_FOLLOWER("§8Ashfang Follower", 0x555555),
    ASHFANG_ACOLYTE("§9Ashfang Acolyte", 0x5555FF),
    ASHFANG_UNDERLING("§cAshfang Underling", 0xFF5555),
    MAGMA_BOSS("§6Magma Boss", 0xFFAA00),
    MAGMA_GLARE("§cMagma Glare", 0xFF5555),
    WUMPA("§bWumpa", 0x55FFFF),
    DOOMSPIRAL("§5Doomspiral", 0xAA00AA),
    HIDEONLEAF("§aHideonleaf", 0x55FF55),
    HIDEONSUN("§eHideonsun", 0xFFFF55),
    HIDEONFLOOR("§aHideonfloor", 0x55FF55),
    HIDEONWALL("§5Hideonwall", 0xAA00AA);

    private final String label;
    private final int glowColorRGB;

    MobVisualTarget(String label, int glowColorRGB) {
        this.label = label;
        this.glowColorRGB = glowColorRGB;
    }

    public String label() {
        return label;
    }

    /** 色コードを除いた表示名。ネームプレートのように色を別途付ける場合に使う */
    public String plainLabel() {
        return label.replaceAll("§.", "");
    }

    public int glowColorRGB() {
        return glowColorRGB;
    }

    public int tracerColorARGB() {
        return 0xFF000000 | glowColorRGB;
    }

    // 全体トグルが切られていれば、リストに載っていても対象外とする
    public boolean highlight() {
        ModConfig.MobVisualsCategory mv = ModConfig.INSTANCE.mobVisuals;
        return mv.enableHighlight && mv.highlightTargets.contains(this);
    }

    public boolean tracer() {
        ModConfig.MobVisualsCategory mv = ModConfig.INSTANCE.mobVisuals;
        return mv.enableTracer && mv.tracerTargets.contains(this);
    }

    public boolean nameplate() {
        ModConfig.MobVisualsCategory mv = ModConfig.INSTANCE.mobVisuals;
        return mv.enableNameplate && mv.nameplateTargets.contains(this);
    }

    /** どれか1つでも有効なら、そのモブを探す必要がある */
    public boolean anyEnabled() {
        return highlight() || tracer() || nameplate();
    }

    /** 設定の初期値。すべて有効な状態から始める */
    public static List<MobVisualTarget> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
