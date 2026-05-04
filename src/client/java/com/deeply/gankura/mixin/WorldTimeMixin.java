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

    /**
     * Minecraft 26.1.2 における時間同期パケットの処理
     * method: handleSetTime (ClientboundSetTimePacket を受け取るメソッド)
     */
    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {

        // ★修正: getGameTime() -> gameTime()
        // Record 型のため、Getter ではなくフィールド名そのままのメソッドを呼びます
        GameState.Server.lastTimePacket = packet.gameTime();
        GameState.Server.lastPacketArrivalMillis = System.currentTimeMillis();

    }
}