package com.deeply.gankura.data;

/** Attribute をどこまで上げる分の値段を出すか */
public enum AttributeCostTarget {
    NEXT_TIER("Next Tier"),
    MAX_TIER("Max Tier");

    private final String label;

    AttributeCostTarget(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
