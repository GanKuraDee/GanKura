package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間
    private static final long SPAWN_DELAY_SMALL_MS = 18000L;
    // Arachne Crystal (Big) 使用からスポーンまでの固定待機時間
    private static final long SPAWN_DELAY_BIG_MS = 40000L;

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg) {
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CALLING_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Small";
            GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + SPAWN_DELAY_SMALL_MS;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CRYSTAL_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Big";
            GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + SPAWN_DELAY_BIG_MS;
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
        }
    }
}
