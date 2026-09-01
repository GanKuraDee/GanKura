package com.deeply.gankura.data;

// Inventory Button のコマンドを、押した瞬間と離した瞬間のどちらで実行するか
public enum ButtonClickType {
    MOUSE_DOWN, MOUSE_UP;

    @Override
    public String toString() {
        return this == MOUSE_DOWN ? "Mouse Down" : "Mouse Up";
    }
}
