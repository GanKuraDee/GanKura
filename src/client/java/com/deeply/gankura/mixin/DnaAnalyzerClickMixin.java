package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.DnaAnalyzerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DNA Analyzer を触っている間の押下に手を入れる。
 *
 * 盤の脇に閉じるボタンが並んでいて、勢いよく解いていると押してしまう。
 * また普通の押下では品を一度持ち上げてしまい、往復ぶん待たされるので、
 * 持ち上げの起きない中クリックに置き換えて送る
 */
@Mixin(AbstractContainerScreen.class)
public class DnaAnalyzerClickMixin {

    // 中クリックの押下番号
    private static final int MIDDLE_BUTTON = 2;

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void gankura$onDnaAnalyzerClick(Slot slot, int slotId, int buttonNum, ContainerInput input,
                                            CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!DnaAnalyzerHandler.inMenu(screen.getTitle().getString())) return;

        if (DnaAnalyzerHandler.blocksClick(slotId)) {
            ci.cancel();
            return;
        }
        if (input == ContainerInput.CLONE || !DnaAnalyzerHandler.usesMiddleClick()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null || client.player == null) return;

        ci.cancel();
        client.gameMode.handleContainerInput(((AbstractContainerScreen<?>) screen).getMenu().containerId,
                slotId, MIDDLE_BUTTON, ContainerInput.CLONE, client.player);
    }
}
