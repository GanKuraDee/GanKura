package com.deeply.gankura.mixin;

import com.deeply.gankura.render.EntityTracerRenderer;
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

    /**
     * Minecraft 26.1.2 仕様:
     * DebugRenderer クラスでは render メソッドが廃止され、emitGizmos に統合されました。
     *
     * メソッドシグネチャ (引数):
     * Frustum frustum, double camX, double camY, double camZ, float partialTicks
     */
    @Inject(at = @At("TAIL"), method = "emitGizmos")
    private void onEmitGizmos(Frustum frustum, double camX, double camY, double camZ, float partialTicks, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        WorldTextRenderer.render(client);
        EntityTracerRenderer.emitGizmos(client);
    }
}