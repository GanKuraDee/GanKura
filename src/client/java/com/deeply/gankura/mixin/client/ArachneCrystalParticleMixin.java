package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Arachne Crystal(Big)使用後に観測されるDUSTパーティクル数を数える。
// 最終的なQuick/Normal判定はArachneHandlerのtickループが閾値と比較して行う
@Mixin(ClientPlayNetworkHandler.class)
public class ArachneCrystalParticleMixin {

    private static final double ALTAR_RADIUS = 2.0;
    // 実機確認により、対象のDUSTパーティクルは黒(RGB全て0近辺)であることが判明したため色でも絞り込む
    private static final float BLACK_COLOR_EPSILON = 0.05f;

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!GameState.Arachne.awaitingCrystalParticles) return;

        if (packet.getSpeed() != 1f) return;
        if (packet.getParameters().getType() != ParticleTypes.DUST) return;

        Vector3f color = ((DustParticleEffect) packet.getParameters()).getColor();
        if (color.x > BLACK_COLOR_EPSILON || color.y > BLACK_COLOR_EPSILON || color.z > BLACK_COLOR_EPSILON) return;

        BlockPos altar = ModConstants.ARACHNE_ALTAR_POS;
        double dx = packet.getX() - altar.getX();
        double dy = packet.getY() - altar.getY();
        double dz = packet.getZ() - altar.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > ALTAR_RADIUS) return;

        GameState.Arachne.particleBurstCounter++;
    }
}
