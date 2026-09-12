package com.deeply.gankura.util;

/**
 * Attribute の画面を開いているか。
 *
 * 未解放の Attribute は "Syphon 1 shard to unlock!" と案内されるが、
 * Heart of the Forest のように別の画面でも "Click to unlock!" と書かれる。
 * ロアの文面だけでは見分けられないので、画面の題でも確かめる
 */
public final class AttributeMenu {

    // 題は "(1/12) Attribute Menu" のようにページ数が頭に付く
    private static final String MENU_TITLE = "Attribute Menu";

    private AttributeMenu() {
    }

    public static boolean isOpen() {
        return MenuTitle.contains(MENU_TITLE);
    }
}
