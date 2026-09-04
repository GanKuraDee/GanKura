package com.deeply.gankura.util;

import java.util.Locale;

/** コインの額の書き方をまとめたもの */
public final class CoinText {

    // 短く書くときの区切りと、その単位
    private static final double BILLION = 1_000_000_000.0;
    private static final double MILLION = 1_000_000.0;
    private static final double THOUSAND = 1_000.0;

    private CoinText() {
    }

    /**
     * @param shortNumbers K・M・B を使って短く書くか。
     *                     そのままの桁で書く場合は、Bazaar に合わせて小数第1位まで添える
     */
    public static String format(double price, boolean shortNumbers) {
        if (shortNumbers) {
            if (price >= BILLION) return trim(price / BILLION) + "B";
            if (price >= MILLION) return trim(price / MILLION) + "M";
            if (price >= THOUSAND) return trim(price / THOUSAND) + "K";
        }

        return String.format(Locale.US, "%,.1f", price);
    }

    // 小数を1桁だけ残す。ちょうどの値は "1.0" ではなく "1" にする
    private static String trim(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
