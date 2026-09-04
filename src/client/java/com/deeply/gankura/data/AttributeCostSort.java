package com.deeply.gankura.data;

/** Attribute の値段の一覧を、どちらの値段で並べるか */
public enum AttributeCostSort {
    INSTANT("Instant"),
    ORDER("Order");

    private final String label;

    AttributeCostSort(String label) {
        this.label = label;
    }

    /** もう一方。箱の見出しを押したときに入れ替える */
    public AttributeCostSort other() {
        return this == INSTANT ? ORDER : INSTANT;
    }

    @Override
    public String toString() {
        return label;
    }
}
