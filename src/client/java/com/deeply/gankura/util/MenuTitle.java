package com.deeply.gankura.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * 今開いている収納画面の題を見る。
 *
 * ロアの文面は画面をまたいで似ることがあるので、
 * どの画面のものか決めかねるときは題でも確かめる。
 * 今の画面を持っている場所は版で違うので、そこもここに閉じ込めておく
 */
public final class MenuTitle {

    private MenuTitle() {
    }

    /** 収納画面を開いていて、その題が渡した言葉を含むか */
    public static boolean contains(String text) {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof AbstractContainerScreen<?>
                && screen.getTitle().getString().contains(text);
    }
}
