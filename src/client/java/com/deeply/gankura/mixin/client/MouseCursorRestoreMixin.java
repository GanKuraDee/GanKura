package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MinecraftClient#setScreen は「画面が閉じて別の画面が開く」全ての経路(コンテナの直接差し替えも含む)が
// 最終的に必ず通る場所なので、Storage/Accessories の切り替えのように lockCursor/unlockCursor を
// 経由しないケースでもここでカーソル位置を確実に復元できる
@Mixin(MinecraftClient.class)
public class MouseCursorRestoreMixin {
    // Skyblock の Storage / Accessories などで画面が素早く閉じて別の画面が開き直される際に、
    // バニラがカーソルを画面中央へリセットしてしまうのを防ぐための猶予時間
    private static final long RAPID_REOPEN_WINDOW_MS = 100L;

    @Shadow @Final private Window window;
    @Shadow @Final public Mouse mouse;

    private long gankura$lastSetScreenTime = -1L;
    private double gankura$savedX;
    private double gankura$savedY;
    private boolean gankura$shouldRestore;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void gankura$beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.misc.enableCursorRestoreOnRapidReopen) {
            this.gankura$shouldRestore = false;
            return;
        }

        long now = System.currentTimeMillis();
        this.gankura$shouldRestore = this.gankura$lastSetScreenTime >= 0
                && (now - this.gankura$lastSetScreenTime) < RAPID_REOPEN_WINDOW_MS;
        if (!this.gankura$shouldRestore) {
            // 素早い切り替えの連鎖でなければ、現在の実際のカーソル位置を新しい基準として記録する
            this.gankura$savedX = this.mouse.getX();
            this.gankura$savedY = this.mouse.getY();
        }
        this.gankura$lastSetScreenTime = now;
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void gankura$afterSetScreen(Screen screen, CallbackInfo ci) {
        if (this.gankura$shouldRestore && screen != null) {
            InputUtil.setCursorParameters(this.window, InputUtil.GLFW_CURSOR_NORMAL, this.gankura$savedX, this.gankura$savedY);
            MouseAccessor accessor = (MouseAccessor) this.mouse;
            accessor.gankura$setX(this.gankura$savedX);
            accessor.gankura$setY(this.gankura$savedY);
        }
    }
}
