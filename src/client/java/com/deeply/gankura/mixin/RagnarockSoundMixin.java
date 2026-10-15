package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.RagnarockHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * サーバーが鳴らす音を、Ragnarock の詠唱が終わった合図として拾う。
 *
 * このメソッドは最初にネットワークスレッドで呼ばれ、メインスレッドへ回されてからもう一度呼ばれる。
 * 回し終えた後に差し込んで、メインスレッドでの一度だけを受け取る
 */
@Mixin(ClientPacketListener.class)
public class RagnarockSoundMixin {

    @Inject(method = "handleSoundEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER))
    private void gankura$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        RagnarockHandler.onSound(packet);
    }
}
