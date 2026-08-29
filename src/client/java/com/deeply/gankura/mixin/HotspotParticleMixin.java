package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.HotspotAreaHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hotspot の縁をなぞるパーティクルを拾う。
 *
 * 水の Hotspot は決まった色の DUST、溶岩の Hotspot は SMOKE で描かれている。
 * 円で置き換える設定のときは、ここでパケットごと止めて元の描画を消す。
 *
 * このメソッドはネットワークスレッドで呼ばれるので、座標を控えるだけに留める。
 */
@Mixin(ClientPacketListener.class)
public class HotspotParticleMixin {

    // 水の Hotspot の縁に使われている色
    private static final Vector3f WATER_COLOR = new Vector3f(1.0f, 0.4117647f, 0.7058824f);
    private static final float COLOR_EPSILON = 0.01f;

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void gankura$onHotspotParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!com.deeply.gankura.data.ModConfig.INSTANCE.fishing.showHotspotCircle) return;
        if (!isHotspotParticle(packet)) return;

        HotspotAreaHandler.onParticle(packet.getX(), packet.getY(), packet.getZ());
        if (HotspotAreaHandler.shouldHideParticles()) ci.cancel();
    }

    private static boolean isHotspotParticle(ClientboundLevelParticlesPacket packet) {
        // 溶岩の Hotspot。色の無いパーティクルなので個数で見分ける
        if (com.deeply.gankura.data.GameState.Server.isCrimsonIsle()) {
            return packet.getParticle().getType() == ParticleTypes.SMOKE
                    && (packet.getCount() == 5 || packet.getCount() == 2);
        }

        if (packet.getParticle().getType() != ParticleTypes.DUST) return false;
        if (packet.getCount() != 0 || packet.getMaxSpeed() != 1f || packet.getXDist() != 1f) return false;

        Vector3f color = ((DustParticleOptions) packet.getParticle()).getColor();
        return Math.abs(color.x - WATER_COLOR.x) < COLOR_EPSILON
                && Math.abs(color.y - WATER_COLOR.y) < COLOR_EPSILON
                && Math.abs(color.z - WATER_COLOR.z) < COLOR_EPSILON;
    }
}
