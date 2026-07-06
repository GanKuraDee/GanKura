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

// Arachne Crystal(Big)使用後、DUSTパーティクルが現れるかどうかで
// 通常スポーン(37秒)かQuick Spawn(21秒、パーティクルが一切現れない)かを判定する
@Mixin(ClientPacketListener.class)
public class ArachneCrystalParticleMixin {

    private static final long NORMAL_SPAWN_DELAY_MS = 37000L;
    private static final double ALTAR_RADIUS = 30.0;

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

        // パーティクルを検知できた = 通常スポーン確定
        GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + NORMAL_SPAWN_DELAY_MS;
        GameState.Arachne.awaitingCrystalParticles = false;
    }
}
