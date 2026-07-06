package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間
    // GolemやDragonと同様にTPS変動に連動させるため、ミリ秒ではなくTick単位で保持する
    private static final long SPAWN_DELAY_SMALL_TICKS = 360L; // 18s * 20 ticks
    // Arachne Crystal (Big) 使用からスポーンまでの固定待機時間 (Tick単位)
    private static final long SPAWN_DELAY_BIG_TICKS = 800L; // 40s * 20 ticks

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg, Minecraft client) {
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CALLING_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Small";
            if (client.level != null) GameState.Arachne.spawnTargetTime = client.level.getGameTime() + SPAWN_DELAY_SMALL_TICKS;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CRYSTAL_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Big";
            if (client.level != null) GameState.Arachne.spawnTargetTime = client.level.getGameTime() + SPAWN_DELAY_BIG_TICKS;
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
