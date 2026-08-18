package com.deeply.gankura.gui;

import java.util.List;
import java.util.Locale;

// 色見本。16進を打たなくても選べるように用意しているだけの並び
public final class ColorPalette {

    private static final List<Integer> ENTRIES = List.of(
            0x00FFFF, 0xFF5555, 0xFFA000, 0xFFFF55, 0x55FF55, 0x00AA00, 0x55FFFF, 0x5555FF,
            0xAA00AA, 0xFF55FF, 0xFFAACC, 0x8B5A2B, 0xFFFFFF, 0xAAAAAA, 0x101010);

    private ColorPalette() {
    }

    public static List<Integer> colors() {
        return ENTRIES;
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    // RRGGBB / #RRGGBB / 短縮形の RGB を受け付ける。色として読めなければ null
    public static Integer parse(String value) {
        String text = value.trim();

        if (text.startsWith("#")) text = text.substring(1);

        if (text.length() == 3) {
            StringBuilder expanded = new StringBuilder(6);
            for (int i = 0; i < 3; i++) {
                expanded.append(text.charAt(i)).append(text.charAt(i));
            }
            text = expanded.toString();
        }

        if (text.length() != 6) return null;

        try {
            return Integer.parseInt(text, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
