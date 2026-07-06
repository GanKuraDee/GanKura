package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間 (SkyHanniの実装に準拠)
    private static final long SPAWN_DELAY_SMALL_MS = 19000L;

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
            // ArachneCrystalParticleMixin がその場で即座に確定させる
            GameState.Arachne.spawnTargetTime = 0;
            GameState.Arachne.awaitingCrystalParticles = true;
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
