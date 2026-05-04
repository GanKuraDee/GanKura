package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class WorldTimeMixin {

    // ★変更: onWorldTimeUpdate -> handleSetTime
    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {

        // ★変更: time() -> getGameTime()
        // ※もしマイクラ内の「1日の時刻（0〜24000）」が必要な場合は getDayTime() を使用してください。
        GameState.Server.lastTimePacket = packet.getGameTime();
        GameState.Server.lastPacketArrivalMillis = System.currentTimeMillis();

    }
}