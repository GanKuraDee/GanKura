package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * 長いツールチップを、ホイールで上下に動かせるようにする。
 *
 * SkyBlock の説明は画面に収まりきらないことがあり、
 * そのままだと下の方が読めない。
 * ずらす量は {@link com.deeply.gankura.mixin.TooltipPositionMixin} が
 * 実際の描き出し位置に足す
 */
public final class ScrollableTooltipHandler {

    // ホイール1目盛りで動かす量(ピクセル)。説明のおよそ1行分
    private static final int SCROLL_STEP = 10;

    // 今どれだけずらしているか
    private static int offset = 0;
    // ずらしている対象。別のアイテムに移ったら元に戻すために覚えておく
    private static Slot scrolledSlot = null;

    private ScrollableTooltipHandler() {
    }

    public static int offset() {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        return config.enableItemTooltipTweaks && config.enableScrollableTooltips ? offset : 0;
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;

            ScreenMouseEvents.allowMouseScroll(screen).register(
                    (ignored, mouseX, mouseY, horizontal, vertical) -> onScroll(container, vertical));
            ScreenEvents.remove(screen).register(ignored -> reset());
        });

        // 別のアイテムに移ったら元の位置に戻す。ホイールを回さなくても切り替わるので毎tick見る
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (offset == 0) return;

            Slot hovered = hoveredSlot(client.screen);
            if (hovered != scrolledSlot) reset();
        });
    }

    /**
     * @return ホイールの動きを画面へ渡すなら true。ツールチップを動かしたときだけ止める
     */
    private static boolean onScroll(AbstractContainerScreen<?> screen, double vertical) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableItemTooltipTweaks || !config.enableScrollableTooltips) return true;
        if (!GameState.Server.isSkyblock() || vertical == 0) return true;

        // 説明が出ていないときは、いつも通りの操作に任せる
        Slot hovered = hoveredSlot(screen);
        if (hovered == null || !hovered.hasItem()) return true;

        if (hovered != scrolledSlot) {
            scrolledSlot = hovered;
            offset = 0;
        }
        // ホイールを下に回すと、説明の下の方が見えるように上へ動かす。
        // 感覚は人によるので、逆向きにもできるようにしてある
        int direction = ModConfig.INSTANCE.interfaceSettings.invertTooltipScroll ? 1 : -1;
        offset += direction * (int) Math.signum(vertical) * SCROLL_STEP;
        return false;
    }

    private static Slot hoveredSlot(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return null;

        return ((ContainerScreenAccessor) screen).gankura$getHoveredSlot();
    }

    private static void reset() {
        offset = 0;
        scrolledSlot = null;
    }
}
