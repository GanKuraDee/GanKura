package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間 (SkyHanniの実装に準拠、Golem/Dragonと同様にTick数で管理)
    private static final long SPAWN_DELAY_SMALL_TICKS = 380L; // 19秒
    // Arachne Crystal (Big): Quick Spawn / 通常スポーンそれぞれの待機時間(Tick数)
    private static final long QUICK_SPAWN_DELAY_TICKS = 360L; // 18秒
    private static final long NORMAL_SPAWN_DELAY_TICKS = 680L; // 34秒
    // Crystal使用後、DUSTパーティクル数を観測する時間(実時間)。この間の検知数が閾値以下ならQuick Spawnと判定する
    private static final long CRYSTAL_PARTICLE_DETERMINATION_MS = 1000L;
    private static final int QUICK_SPAWN_THRESHOLD = 20;

    public static void register() {
        // 観測時間が経過したら、その間に検知したDUSTパーティクル数(ArachneCrystalParticleMixinが加算)で
        // Quick Spawn(閾値以下)か通常スポーン(閾値超)かを確定する
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (GameState.Arachne.awaitingCrystalParticles
                    && System.currentTimeMillis() - GameState.Arachne.crystalMessageTime > CRYSTAL_PARTICLE_DETERMINATION_MS) {
                long delayTicks = GameState.Arachne.particleBurstCounter <= QUICK_SPAWN_THRESHOLD ? QUICK_SPAWN_DELAY_TICKS : NORMAL_SPAWN_DELAY_TICKS;
                if (client.level != null) GameState.Arachne.spawnTargetTime = client.level.getGameTime() + delayTicks;
                GameState.Arachne.awaitingCrystalParticles = false;
            }
        });
    }

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    // Spawnedの確定はEntityHealthScannerによる蜘蛛の巣ブロックの検知のみで行う
    public static void handleMessage(String unformattedMsg, Minecraft client) {
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CALLING_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.size = "Small";
            GameState.Arachne.awaitingCrystalParticles = false;
            GameState.Arachne.arachneMessageSeen = false;
            if (client.level != null) GameState.Arachne.spawnTargetTime = client.level.getGameTime() + SPAWN_DELAY_SMALL_TICKS;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CRYSTAL_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.size = "Big";
            // Quick Spawn(18秒)か通常スポーン(34秒)かは、観測時間内のDUSTパーティクル数で確定させる(上のtickで判定)
            GameState.Arachne.spawnTargetTime = 0;
            GameState.Arachne.awaitingCrystalParticles = true;
            GameState.Arachne.crystalMessageTime = System.currentTimeMillis();
            GameState.Arachne.particleBurstCounter = 0;
            GameState.Arachne.arachneMessageSeen = false;
            return;
        }

        if (ModConstants.startsWithIgnoreCase(unformattedMsg, ModConstants.ARACHNE_BOSS_MSG_PREFIX)) {
            // 蜘蛛の巣がまだ検知できていない間、間もなくスポーンする合図として使う(カウントダウン中は優先されない)
            GameState.Arachne.arachneMessageSeen = true;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_DOWN_MSG)) {
            GameState.Arachne.isSummoning = false;
            GameState.Arachne.size = null;
            GameState.Arachne.awaitingCrystalParticles = false;
            GameState.Arachne.arachneMessageSeen = false;
        }
    }
}
