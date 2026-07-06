package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Arachne Crystal(Big)使用後に観測されるDUSTパーティクル数を数える。
// 最終的なQuick/Normal判定はArachneHandlerのtickループが閾値と比較して行う
@Mixin(ClientPlayNetworkHandler.class)
public class ArachneCrystalParticleMixin {

    private static final double ALTAR_RADIUS = 4.0;

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!GameState.Arachne.awaitingCrystalParticles) return;

        if (packet.getSpeed() != 1f) return;
        if (packet.getParameters().getType() != ParticleTypes.DUST) return;

        BlockPos altar = ModConstants.ARACHNE_ALTAR_POS;
        double dx = packet.getX() - altar.getX();
        double dy = packet.getY() - altar.getY();
        double dz = packet.getZ() - altar.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > ALTAR_RADIUS) return;

        GameState.Arachne.particleBurstCounter++;
    }
}
