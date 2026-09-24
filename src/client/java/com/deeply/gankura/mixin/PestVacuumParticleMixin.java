package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PestVacuumHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vacuum が害虫の方へ飛ばす粒を拾う。
 *
 * 道筋になるのは怒った村人の粒で、これを繋いだ先に害虫がいる。
 * 一緒に出るエンチャント台の粒は道筋ではないが、
 * 隠す設定のときは目障りなので、こちらも消す。
 *
 * このメソッドはネットワークスレッドで呼ばれるので、座標を控えるだけに留める
 */
@Mixin(ClientPacketListener.class)
public class PestVacuumParticleMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void gankura$onPestVacuumParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!PestVacuumHandler.isReading()) return;

        ParticleType<?> type = packet.particle().getType();
        boolean trail = type == ParticleTypes.ANGRY_VILLAGER
                && packet.count() == 1 && packet.xMaxSpeed() == 0.0f && isSpotOn(packet);
        boolean sparkle = type == ParticleTypes.ENCHANT
                && packet.count() == 10 && packet.xMaxSpeed() == -2.0f && isSpotOn(packet);
        if (!trail && !sparkle) return;

        if (trail) PestVacuumHandler.onParticle(packet.x(), packet.y(), packet.z());
        if (PestVacuumHandler.hidesParticles()) ci.cancel();
    }

    // ばらけずに1点から出ているか。同じ種類でも、散らして出す粒とは別物になる
    private static boolean isSpotOn(ClientboundLevelParticlesPacket packet) {
        return packet.xDist() == 0.0f && packet.yDist() == 0.0f && packet.zDist() == 0.0f;
    }
}
