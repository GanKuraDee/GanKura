package com.deeply.gankura.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseCursorRestoreMixin {
    // Skyblock の Storage 切り替えなど、画面が一瞬だけ閉じてすぐ別の画面が開き直される際に
    // バニラがカーソルを画面中央へリセットしてしまうのを防ぐための猶予時間
    private static final long RAPID_REOPEN_WINDOW_MS = 100L;

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private boolean mouseGrabbed;
    @Shadow @Final private Minecraft minecraft;

    private double gankura$savedXpos;
    private double gankura$savedYpos;
    private long gankura$lastGrabTime = -1L;

    @Inject(method = "grabMouse", at = @At("HEAD"))
    private void gankura$onGrabMouse(CallbackInfo ci) {
        if (!this.mouseGrabbed) {
            // バニラが中央にリセットする直前の、実際のカーソル位置を保存しておく
            this.gankura$savedXpos = this.xpos;
            this.gankura$savedYpos = this.ypos;
            this.gankura$lastGrabTime = System.currentTimeMillis();
        }
    }

    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private void gankura$onReleaseMouse(CallbackInfo ci) {
        if (!this.mouseGrabbed) return;

        boolean rapidReopen = this.gankura$lastGrabTime >= 0
                && (System.currentTimeMillis() - this.gankura$lastGrabTime) < RAPID_REOPEN_WINDOW_MS;

        if (rapidReopen) {
            this.mouseGrabbed = false;
            this.xpos = this.gankura$savedXpos;
            this.ypos = this.gankura$savedYpos;
            Window window = this.minecraft.getWindow();
            InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_NORMAL, this.xpos, this.ypos);
            ci.cancel();
        }
    }
}
