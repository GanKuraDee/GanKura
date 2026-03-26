package com.deeply.gankura.mixin.client;

import com.deeply.gankura.render.WorldTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class WorldTextRenderMixin {

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress, CallbackInfo ci) {
        // 実際のワールド描画処理は、専用のレンダラークラスに丸投げする
        WorldTextRenderer.render(MinecraftClient.getInstance());
    }
}