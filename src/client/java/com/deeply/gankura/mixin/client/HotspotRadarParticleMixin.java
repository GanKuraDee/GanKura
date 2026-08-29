package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.HotspotRadarHandler;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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
@Mixin(ClientPlayNetworkHandler.class)
public class HotspotRadarParticleMixin {

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void gankura$onRadarParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.fishing.showHotspotGuess) return;

        ParticleType<?> type = packet.getParameters().getType();
        boolean enchant = type == ParticleTypes.ENCHANT && packet.getCount() == 10 && packet.getSpeed() == -2.0f;
        boolean flame = type == ParticleTypes.FLAME && packet.getCount() == 1 && packet.getSpeed() == 0.0f;
        if (!enchant && !flame) return;

        HotspotRadarHandler.onParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
