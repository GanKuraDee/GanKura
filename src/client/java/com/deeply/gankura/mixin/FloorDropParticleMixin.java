package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.FloorDropHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Floor Drop はブロックではなく ItemDisplay の重なりでできているため、地形からは見つけられない。
 * 湧いた瞬間に出る HAPPY_VILLAGER のパーティクルが唯一の手掛かりなので、その座標をここで拾う。
 *
 * このメソッドはネットワークスレッドで呼ばれる。ワールドやエンティティに触ると壊れるので、
 * 座標を控えるだけに留め、実際の照合は tick 側(FloorDropHandler)で行う。
 */
@Mixin(ClientPacketListener.class)
public class FloorDropParticleMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void gankura$onHappyVillagerParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.foraging.enableFloorDrops) return;
        if (!GameState.Server.isSafari()) return;
        if (packet.getParticle().getType() != ParticleTypes.HAPPY_VILLAGER) return;

        FloorDropHandler.onFloorDropParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
