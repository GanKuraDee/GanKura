package com.deeply.gankura.mixin;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.HotspotRadarHandler;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hotspot Radar の軌跡のパーティクルを拾う。
 *
 * 向きや個数が独特なので、その組み合わせで見分けられる。
 * 参考にした2つの Mod で形が違ったので、どちらも拾う。
 *
 * このメソッドはネットワークスレッドで呼ばれるので、座標を控えるだけに留める。
 */
@Mixin(ClientPacketListener.class)
public class HotspotRadarParticleMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void gankura$onRadarParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.fishing.showHotspotGuess) return;

        ParticleType<?> type = packet.getParticle().getType();
        boolean enchant = type == ParticleTypes.ENCHANT && packet.getCount() == 10 && packet.getMaxSpeed() == -2.0f;
        boolean flame = type == ParticleTypes.FLAME && packet.getCount() == 1 && packet.getMaxSpeed() == 0.0f;
        if (!enchant && !flame) return;

        HotspotRadarHandler.onParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
