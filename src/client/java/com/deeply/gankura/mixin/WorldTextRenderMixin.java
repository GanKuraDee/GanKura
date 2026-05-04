package com.deeply.gankura.mixin;

import com.deeply.gankura.render.WorldTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class WorldTextRenderMixin {

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress, CallbackInfo ci) {
        // 実際のワールド描画処理は、専用のレンダラークラスに丸投げする
        WorldTextRenderer.render(Minecraft.getInstance());
    }
}