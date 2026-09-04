package com.deeply.gankura.data;

/** Bazaar の値段のうち、ツールチップに出すもの */
public enum BazaarPriceType {
    INSTANT_BUY("Instant Buy"),
    INSTANT_SELL("Instant Sell"),
    BOTH("Both");

    private final String label;

    BazaarPriceType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
