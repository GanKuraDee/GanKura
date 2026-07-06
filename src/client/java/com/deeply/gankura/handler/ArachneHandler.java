package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;

public class ArachneHandler {
    // Arachne's Calling 使用からスポーンまでの固定待機時間
    private static final long SPAWN_DELAY_MS = 18000L;

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg) {
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CALLING_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = null;
            GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + SPAWN_DELAY_MS;
            return;
        }

        if (GameState.Arachne.isSummoning && ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_SPAWN_MSG)) {
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
