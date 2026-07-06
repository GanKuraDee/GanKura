package com.deeply.gankura.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseCursorRestoreMixin {
    // Skyblock の Storage 切り替えなど、画面が一瞬だけ閉じてすぐ別の画面が開き直される際に
    // バニラがカーソルを画面中央へリセットしてしまうのを防ぐための猶予時間
    private static final long RAPID_REOPEN_WINDOW_MS = 100L;

    @Shadow private double x;
    @Shadow private double y;
    @Shadow private boolean cursorLocked;
    @Shadow @Final private MinecraftClient client;

    private double gankura$savedX;
    private double gankura$savedY;
    private long gankura$lastLockTime = -1L;

    @Inject(method = "lockCursor", at = @At("HEAD"))
    private void gankura$onLockCursor(CallbackInfo ci) {
        if (!this.cursorLocked) {
            // バニラが中央にリセットする直前の、実際のカーソル位置を保存しておく
            this.gankura$savedX = this.x;
            this.gankura$savedY = this.y;
            this.gankura$lastLockTime = System.currentTimeMillis();
        }
    }

    @Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true)
    private void gankura$onUnlockCursor(CallbackInfo ci) {
        if (!this.cursorLocked) return;

        boolean rapidReopen = this.gankura$lastLockTime >= 0
                && (System.currentTimeMillis() - this.gankura$lastLockTime) < RAPID_REOPEN_WINDOW_MS;

        if (rapidReopen) {
            this.cursorLocked = false;
            this.x = this.gankura$savedX;
            this.y = this.gankura$savedY;
            Window window = this.client.getWindow();
            InputUtil.setCursorParameters(window, InputUtil.GLFW_CURSOR_NORMAL, this.x, this.y);
            ci.cancel();
        }
    }
}
