package com.deeply.gankura.waypoint;

// ウェイポイントのブロックをどう見せるか
public enum HighlightStyle {
    // 枠線だけ
    OUTLINE("Line"),
    // 半透明の塗りつぶしだけ
    FILL("Fill"),
    // 両方(既定)
    BOTH("Both");

    private final String displayName;

    HighlightStyle(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasOutline() {
        return this != FILL;
    }

    public boolean hasFill() {
        return this != OUTLINE;
    }

    // 設定画面のボタンは押すたびに次の種類へ送る
    public HighlightStyle next() {
        HighlightStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String displayName() {
        return displayName;
    }
}
