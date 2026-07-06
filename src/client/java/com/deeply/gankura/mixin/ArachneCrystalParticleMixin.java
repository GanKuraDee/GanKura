package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Arachne Crystal(Big)使用後に観測されるDUSTパーティクル数を数える。
// 最終的なQuick/Normal判定はArachneHandlerのtickループが閾値と比較して行う
@Mixin(ClientPacketListener.class)
public class ArachneCrystalParticleMixin {

    private static final double ALTAR_RADIUS = 4.0;
    // 実機確認により、対象のDUSTパーティクルは黒(RGB全て0近辺)であることが判明したため色でも絞り込む
    private static final float BLACK_COLOR_EPSILON = 0.05f;

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!GameState.Arachne.awaitingCrystalParticles) return;

        if (packet.getMaxSpeed() != 1f) return;
        if (packet.getParticle().getType() != ParticleTypes.DUST) return;

        Vector3f color = ((DustParticleOptions) packet.getParticle()).getColor();
        if (color.x > BLACK_COLOR_EPSILON || color.y > BLACK_COLOR_EPSILON || color.z > BLACK_COLOR_EPSILON) return;

        BlockPos altar = ModConstants.ARACHNE_ALTAR_POS;
        double dx = packet.getX() - altar.getX();
        double dy = packet.getY() - altar.getY();
        double dz = packet.getZ() - altar.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > ALTAR_RADIUS) return;

        GameState.Arachne.particleBurstCounter++;
    }
}
