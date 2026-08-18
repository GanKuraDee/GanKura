package com.deeply.gankura.waypoint;

import net.minecraft.core.BlockPos;

// エリア内の1ブロックを指す目印。JSONへそのまま書き出すのでフィールドは素の型で持つ
public class Waypoint {

    // 既定の色(シアン)。アルファは持たず、塗りつぶしの濃さは fillAlpha で別に指定する
    public static final int DEFAULT_COLOR = 0x00FFFF;
    // 既定のグループ。空文字が「グループ分けしていない状態」を表す
    public static final String DEFAULT_GROUP = "";
    public static final int DEFAULT_FILL_ALPHA = 90;

    private String name = "";
    private int x;
    private int y;
    private int z;
    private int color = DEFAULT_COLOR;
    private boolean enabled = true;
    private String group = DEFAULT_GROUP;
    private HighlightStyle style = HighlightStyle.BOTH;
    // 塗りつぶしの不透明度 (0-255)
    private int fillAlpha = DEFAULT_FILL_ALPHA;

    public Waypoint() {
    }

    public Waypoint(String name, int x, int y, int z, int color, String group) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.group = group;
    }

    public static Waypoint of(String name, BlockPos pos, String group) {
        return new Waypoint(name, pos.getX(), pos.getY(), pos.getZ(), DEFAULT_COLOR, group);
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getColor() {
        return color & 0xFFFFFF;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroup() {
        return group == null ? DEFAULT_GROUP : group;
    }

    public void setGroup(String group) {
        this.group = group == null ? DEFAULT_GROUP : group;
    }

    public int getFillAlpha() {
        return Math.clamp(fillAlpha, 0, 255);
    }

    public void setFillAlpha(int fillAlpha) {
        this.fillAlpha = Math.clamp(fillAlpha, 0, 255);
    }

    // 古いファイルや手書きのJSONでは style が欠けていることがあるので既定値へ倒す
    public HighlightStyle getStyle() {
        return style == null ? HighlightStyle.BOTH : style;
    }

    public void setStyle(HighlightStyle style) {
        this.style = style;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }
}
