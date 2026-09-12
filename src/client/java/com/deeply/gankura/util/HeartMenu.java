package com.deeply.gankura.util;

/**
 * Heart of the Forest か Heart of the Mountain の画面を開いているか。
 *
 * どちらも項目の書き方が同じなので、まとめて扱う。
 * 状態の行("ENABLED" など)もレベルの書き方も他の画面と紛れ得るので、
 * これらの画面を開いているときだけ読む
 */
public final class HeartMenu {

    private static final String[] MENU_TITLES = {"Heart of the Forest", "Heart of the Mountain"};

    private HeartMenu() {
    }

    public static boolean isOpen() {
        for (String title : MENU_TITLES) {
            if (MenuTitle.contains(title)) return true;
        }
        return false;
    }
}
