package com.deeply.mixin.client;

import com.deeply.gankura.data.GameState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class WorldTimeMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("RETURN"))
    private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        // time() を使用する
        GameState.lastServerTimePacket = packet.time();
        GameState.lastServerPacketArrivalMillis = System.currentTimeMillis();
    }
}