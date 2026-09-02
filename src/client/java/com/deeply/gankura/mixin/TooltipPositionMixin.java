package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.ScrollableTooltipHandler;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ツールチップの縦位置に手を入れる。
 *
 * バニラは説明が画面に収まるように位置を丸めるので、
 * 画面より高い説明は上端で止まり、下の方が読めない。
 * 丸めた後の位置を起点に、ホイールで動かした分を足す
 */
@Mixin(DefaultTooltipPositioner.class)
public class TooltipPositionMixin {

    // 画面の縁に残す余白。バニラが位置を丸めるときと同じ値
    private static final int MARGIN = 4;

    @Inject(method = "positionTooltip", at = @At("RETURN"), cancellable = true)
    private void gankura$moveTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY,
                                     int tooltipWidth, int tooltipHeight,
                                     CallbackInfoReturnable<Vector2ic> cir) {
        if (!GameState.Server.isSkyblock()) return;

        // 画面に収まっている説明はカーソルの脇に出したままにする。
        // 一番上に揃えるのは、頭が切れてしまうものだけでよい
        boolean overflows = tooltipHeight + MARGIN * 2 > screenHeight;
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        boolean fromTop = overflows && config.enableItemTooltipTweaks && config.tooltipFromTop;

        int offset = ScrollableTooltipHandler.offset();
        if (!fromTop && offset == 0) return;

        Vector2ic position = cir.getReturnValue();
        int y = (fromTop ? MARGIN : position.y()) + offset;

        cir.setReturnValue(new Vector2i(position.x(), y));
    }
}
