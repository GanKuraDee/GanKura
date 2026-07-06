package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Arachne Crystal(Big)使用後に観測されるDUSTパーティクル数を数える。
// 最終的なQuick/Normal判定はArachneHandlerのtickループが閾値と比較して行う
@Mixin(ClientPacketListener.class)
public class ArachneCrystalParticleMixin {

    private static final double ALTAR_RADIUS = 4.0;

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!GameState.Arachne.awaitingCrystalParticles) return;

        if (packet.getMaxSpeed() != 1f) return;
        if (packet.getParticle().getType() != ParticleTypes.DUST) return;

        BlockPos altar = ModConstants.ARACHNE_ALTAR_POS;
        double dx = packet.getX() - altar.getX();
        double dy = packet.getY() - altar.getY();
        double dz = packet.getZ() - altar.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > ALTAR_RADIUS) return;

        GameState.Arachne.particleBurstCounter++;
    }
}
