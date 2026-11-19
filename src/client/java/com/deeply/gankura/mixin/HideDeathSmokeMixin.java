package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.DeathSmokeFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死んだエンティティから出る白い煙を消す。
 *
 * Hypixel ではこの煙がサーバーからパーティクルとして届くので、受け取る入口で止める。
 * どちらのメソッドも最初にネットワークスレッドで呼ばれ、メインスレッドへ回されてからもう一度呼ばれる。
 * 回し終えた後に差し込んで、メインスレッドでの一度だけを扱う
 */
@Mixin(ClientPacketListener.class)
public class HideDeathSmokeMixin {

    @Inject(method = "handleEntityEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER))
    private void gankura$rememberDeath(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        DeathSmokeFilter.onEntityEvent(packet.getEntity(client.level), packet.getEventId());
    }

    @Inject(method = "handleParticleEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void gankura$hideDeathSmoke(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (DeathSmokeFilter.shouldHide(packet)) ci.cancel();
    }
}
