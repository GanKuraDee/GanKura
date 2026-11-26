package com.deeply.gankura.data;

import java.util.List;

/**
 * スポーンアラートを出す Dragon の種類。
 * 設定画面のドラッグリスト(Add/ゴミ箱)で選択する。
 * ドラッグリストは toString() を表示に使うため、ラベルには色コードをそのまま入れている。
 * 色は showDragonSpawnAlert のタイトル色に合わせている。
 */
public enum DragonAlertType {
    PROTECTOR("§8", "Protector"),
    OLD("§7", "Old"),
    UNSTABLE("§5", "Unstable"),
    YOUNG("§f", "Young"),
    STRONG("§c", "Strong"),
    WISE("§b", "Wise"),
    SUPERIOR("§e", "Superior");

    private final String colorCode;
    private final String label;
    private final String typeName;

    DragonAlertType(String colorCode, String typeName) {
        this.colorCode = colorCode;
        this.label = colorCode + typeName;
        this.typeName = typeName;
    }

    public String colorCode() {
        return colorCode;
    }

    public String typeName() {
        return typeName;
    }

    /** GameState.Dragon.type や DRAGON DOWN! の文字列に対応する種類を返す。未知の種類は null */
    public static DragonAlertType fromTypeName(String typeName) {
        if (typeName == null) return null;
        for (DragonAlertType type : values()) {
            if (type.typeName.equalsIgnoreCase(typeName)) return type;
        }
        return null;
    }

    /** 設定の初期値。すべて有効な状態から始める */
    public static List<DragonAlertType> defaults() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return label;
    }
}
