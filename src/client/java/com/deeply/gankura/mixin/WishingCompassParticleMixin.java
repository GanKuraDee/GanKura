package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.WishingCompassHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wishing Compass が目標へ向かって飛ばす粒を拾う。
 *
 * 使われているのは村人の緑の粒で、目標の方角へ一列に並んで飛んでいく。
 * このメソッドはネットワークスレッドで呼ばれるので、座標を控えるだけに留める
 */
@Mixin(ClientPacketListener.class)
public class WishingCompassParticleMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void gankura$onCompassParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (packet.getParticle().getType() != ParticleTypes.HAPPY_VILLAGER) return;

        WishingCompassHandler.onParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
