package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間 (SkyHanniの実装に準拠)
    private static final long SPAWN_DELAY_SMALL_MS = 19000L;
    // Arachne Crystal (Big): パーティクルが検知できないまま判定を諦めるまでの待ち時間、
    // およびその場合のQuick Spawnフォールバック待機時間
    // (通常スポーンなら検知後すぐにパーティクルが届くはずなので、これは「一切パーティクルが届かない=Quick」というケースの保険)
    private static final long CRYSTAL_PARTICLE_FALLBACK_MS = 1000L;
    private static final long QUICK_SPAWN_DELAY_MS = 21000L;

    public static void register() {
        // ArachneCrystalParticleMixin が1個もパーティクルを検知できないまま一定時間が経過した場合、Quick Spawnとして確定する
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (GameState.Arachne.awaitingCrystalParticles
                    && System.currentTimeMillis() - GameState.Arachne.crystalMessageTime > CRYSTAL_PARTICLE_FALLBACK_MS) {
                GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + QUICK_SPAWN_DELAY_MS;
                GameState.Arachne.awaitingCrystalParticles = false;
            }
        });
    }

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg) {
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CALLING_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Small";
            GameState.Arachne.awaitingCrystalParticles = false;
            GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + SPAWN_DELAY_SMALL_MS;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CRYSTAL_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Big";
            // Quick Spawn(21秒)か通常スポーン(37秒)かは、直後に届く1個のパーティクルパケットのcountを見て
            // ArachneCrystalParticleMixin がその場で即座に確定させる。
            // 一定時間パーティクルが検知できなければ上のtickでQuickにフォールバックする
            GameState.Arachne.spawnTargetTime = 0;
            GameState.Arachne.awaitingCrystalParticles = true;
            GameState.Arachne.crystalMessageTime = System.currentTimeMillis();
            return;
        }

        if (GameState.Arachne.isSummoning
                && (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_SPAWN_MSG)
                        || ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_SPAWN_BIG_MSG))) {
            GameState.Arachne.hasSpawned = true;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_DOWN_MSG)) {
            GameState.Arachne.isReady = true;
            GameState.Arachne.isSummoning = false;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.size = null;
            GameState.Arachne.awaitingCrystalParticles = false;
        }
    }
}
