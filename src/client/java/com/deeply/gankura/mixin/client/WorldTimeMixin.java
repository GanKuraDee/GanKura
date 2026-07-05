package com.deeply.gankura.mixin.client;

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
        long newGameTime = packet.time();
        long now = System.currentTimeMillis();

        // TPS推定: サーバーは通常1Tickにつき1回このパケットを送るため、
        // 実際の経過時間(1秒間)に対して経過したTick数からTPSを逆算する
        if (GameState.Server.tpsWindowStartMillis == 0) {
            GameState.Server.tpsWindowStartMillis = now;
            GameState.Server.tpsWindowStartTicks = newGameTime;
        } else {
            long elapsedMillis = now - GameState.Server.tpsWindowStartMillis;
            long elapsedTicks = newGameTime - GameState.Server.tpsWindowStartTicks;
            if (elapsedTicks < 0) {
                // ワールド移動直後などTickカウンターが不連続になった場合はウィンドウを取り直す
                GameState.Server.tpsWindowStartMillis = now;
                GameState.Server.tpsWindowStartTicks = newGameTime;
            } else if (elapsedMillis >= 1000) {
                GameState.Server.tps = Math.min(20.0, elapsedTicks * 1000.0 / elapsedMillis);
                GameState.Server.tpsWindowStartMillis = now;
                GameState.Server.tpsWindowStartTicks = newGameTime;
            }
        }

        GameState.Server.lastTimePacket = newGameTime;
        GameState.Server.lastPacketArrivalMillis = now;
    }
}