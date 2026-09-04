package com.deeply.gankura.util;

import com.deeply.gankura.data.ModConfig;

/**
 * 枠に敷く色の濃さを、設定に合わせて決める。
 *
 * 塗りはアイテムの下に敷くので、濃くするほど品の背が塗りつぶされ、
 * 薄くするほど元のスロットの地の色が透けて見える
 */
public final class HighlightColor {

    private static final int FULL_ALPHA = 0xFF;
    private static final int FULL_PERCENT = 100;
    private static final int RGB_MASK = 0x00FFFFFF;

    private HighlightColor() {
    }

    /** 設定の濃さを載せた色。渡すのは 0xRRGGBB の並び */
    public static int tint(int rgb) {
        int percent = Math.min(Math.max(ModConfig.INSTANCE.interfaceSettings.highlightOpacity, 0), FULL_PERCENT);
        int alpha = percent * FULL_ALPHA / FULL_PERCENT;

        return (alpha << 24) | (rgb & RGB_MASK);
    }
}
