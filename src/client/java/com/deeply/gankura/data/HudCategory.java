package com.deeply.gankura.data;

/**
 * HUD の分類。実際に画面へ出る場面が重ならない組み合わせを分けている。
 *
 * GENERAL はどのエリアでも出るもの。それ以外はそのエリアにいる間だけ出るので、
 * エリアが違えば既定位置が重なっていても画面上でぶつかることはない。
 * HUD の移動画面では、この分類で絞り込んで並びを確かめられる。
 */
public enum HudCategory {
    GENERAL("General"),
    THE_END("The End"),
    SPIDERS_DEN("Spider's Den"),
    CRIMSON_ISLE("Crimson Isle"),
    CRITTER_SAFARI("Critter Safari"),
    FISHING("Fishing");

    private final String label;

    HudCategory(String label) {
        this.label = label;
    }

    /** 移動画面のタブに出す名前 */
    public String label() {
        return label;
    }
}
