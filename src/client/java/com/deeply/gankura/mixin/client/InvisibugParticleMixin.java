package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invisibug は完全に透明なアーマースタンドとして送られてくるため、エンティティの種類では見つけられない。
 * 唯一の手掛かりが本体から出る CRIT パーティクルなので、その座標をここで拾う。
 *
 * このメソッドはネットワークスレッドで呼ばれる。ワールドやエンティティに触ると壊れるので、
 * 座標を控えるだけに留め、実際の照合は tick 側(EntityHighlightManager)で行う。
 */
@Mixin(ClientPlayNetworkHandler.class)
public class InvisibugParticleMixin {

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void gankura$onCritParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!GameState.Server.isMoongladeMarsh()) return;
        if (packet.getParameters().getType() != ParticleTypes.CRIT) return;

        EntityHighlightManager.onCritParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
