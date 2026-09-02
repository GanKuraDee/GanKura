package com.deeply.gankura.mixin;

import com.deeply.gankura.data.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Gui#setScreen は「画面が閉じて別の画面が開く」全ての経路(コンテナの直接差し替えも含む)が
// 最終的に必ず通る場所なので、Storage/Accessories の切り替えのように grabMouse/releaseMouse を
// 経由しないケースでもここでカーソル位置を確実に復元できる
@Mixin(Gui.class)
public class MouseCursorRestoreMixin {
    // Skyblock の Storage / Accessories などで画面が素早く閉じて別の画面が開き直される際に、
    // バニラがカーソルを画面中央へリセットしてしまうのを防ぐための猶予時間
    private static final long RAPID_REOPEN_WINDOW_MS = 100L;

    @Shadow @Final private Minecraft minecraft;

    private long gankura$lastSetScreenTime = -1L;
    private double gankura$savedX;
    private double gankura$savedY;
    private boolean gankura$shouldRestore;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void gankura$beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.interfaceSettings.enableCursorRestoreOnRapidReopen) {
            this.gankura$shouldRestore = false;
            return;
        }

        long now = System.currentTimeMillis();
        this.gankura$shouldRestore = this.gankura$lastSetScreenTime >= 0
                && (now - this.gankura$lastSetScreenTime) < RAPID_REOPEN_WINDOW_MS;
        if (!this.gankura$shouldRestore) {
            // 素早い切り替えの連鎖でなければ、現在の実際のカーソル位置を新しい基準として記録する
            this.gankura$savedX = this.minecraft.mouseHandler.xpos();
            this.gankura$savedY = this.minecraft.mouseHandler.ypos();
        }
        this.gankura$lastSetScreenTime = now;
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void gankura$afterSetScreen(Screen screen, CallbackInfo ci) {
        if (this.gankura$shouldRestore && screen != null) {
            Window window = this.minecraft.getWindow();
            InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_NORMAL, this.gankura$savedX, this.gankura$savedY);
            MouseHandlerAccessor accessor = (MouseHandlerAccessor) this.minecraft.mouseHandler;
            accessor.gankura$setXpos(this.gankura$savedX);
            accessor.gankura$setYpos(this.gankura$savedY);
        }
    }
}
