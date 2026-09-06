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

        // TPS推定: このパケットは 20 Tick おきに送られてくる(実測で確認済み)。
        // つまり1通ごとに「20 Tick 進むのに何ミリ秒かかったか」がそのまま分かるので、
        // 一定時間ぶんを溜めて平均する必要はない。溜めるとその間だけ推定が遅れて古くなる
        GameState.Server.updateTps(newGameTime, now);

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