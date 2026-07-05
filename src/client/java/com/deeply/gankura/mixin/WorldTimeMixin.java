package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class WorldTimeMixin {

    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        long newGameTime = packet.gameTime();
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

        // ★修正: 新システム「WorldClock」の Map から時間を抽出する
        if (packet.clockUpdates() != null && !packet.clockUpdates().isEmpty()) {
            // Hypixel 等では基本的に1つの時計（オーバーワールドの時計）しか送られてこないため、
            // Map の最初の値（ClockNetworkState）を取得します。
            ClockNetworkState state = packet.clockUpdates().values().iterator().next();

            // ---------------------------------------------------------
            // 【重要】ここで state. の後に続くメソッド名を補完で探してください！
            // ---------------------------------------------------------
            // おそらく state.time()、state.dayTime()、state.timeOfDay() のいずれかです。
            // 以下の「???」を、IntelliJのサジェストで出てくる long 型のメソッドに置き換えてください。

            GameState.Server.dayTime = Math.abs(state.totalTicks()); // ← ※暫定的に time() としています
        }
    }
}