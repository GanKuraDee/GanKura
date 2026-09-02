package com.deeply.gankura.data;

/** 溶岩を水の見た目にする範囲 */
public enum LavaTextureArea {
    CRIMSON_ISLE("Crimson Isle"),
    EVERYWHERE("Everywhere");

    private final String label;

    LavaTextureArea(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
