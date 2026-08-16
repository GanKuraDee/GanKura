package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.FloorDropHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
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
@Mixin(ClientPlayNetworkHandler.class)
public class FloorDropParticleMixin {

    @Inject(method = "onParticle", at = @At("HEAD"))
    private void gankura$onHappyVillagerParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.foraging.enableFloorDrops) return;
        if (!GameState.Server.isSafari()) return;
        if (packet.getParameters().getType() != ParticleTypes.HAPPY_VILLAGER) return;

        FloorDropHandler.onFloorDropParticle(packet.getX(), packet.getY(), packet.getZ());
    }
}
