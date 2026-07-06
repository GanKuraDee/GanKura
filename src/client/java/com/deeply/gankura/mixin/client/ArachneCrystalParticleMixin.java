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

// Arachne Crystal(Big)使用直後に発生するパーティクルのバーストを観測し、
// Quick Spawn(21秒)か通常スポーン(37秒)かを判定する(SkyHanniのArachneSpawnTimerを移植)
@Mixin(ClientPlayNetworkHandler.class)
public class ArachneCrystalParticleMixin {

    private static final long GRACE_PERIOD_MS = 3000L;
    private static final long BURST_WINDOW_MS = 60L;
    private static final int QUICK_SPAWN_THRESHOLD = 20;
    private static final long QUICK_SPAWN_DELAY_MS = 21000L;
    private static final long NORMAL_SPAWN_DELAY_MS = 37000L;
    private static final double ALTAR_RADIUS = 30.0;

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!GameState.Arachne.awaitingCrystalParticles) return;

        long now = System.currentTimeMillis();
        if (now - GameState.Arachne.crystalMessageTime < GRACE_PERIOD_MS) return;

        if (GameState.Arachne.particleBurstCounter == 0 && GameState.Arachne.particleBurstStartTime == 0) {
            GameState.Arachne.particleBurstStartTime = now;
        }

        if (now - GameState.Arachne.particleBurstStartTime > BURST_WINDOW_MS) {
            long delay = GameState.Arachne.particleBurstCounter <= QUICK_SPAWN_THRESHOLD ? QUICK_SPAWN_DELAY_MS : NORMAL_SPAWN_DELAY_MS;
            GameState.Arachne.spawnTargetTime = now + delay;
            GameState.Arachne.awaitingCrystalParticles = false;
            return;
        }

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
