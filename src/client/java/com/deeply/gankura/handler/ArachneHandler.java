package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ArachneHandler {
    // Arachne's Calling (Small) 使用からスポーンまでの固定待機時間 (SkyHanniの実装に準拠)
    private static final long SPAWN_DELAY_SMALL_MS = 19000L;
    // Arachne Crystal (Big): Quick Spawn / 通常スポーンそれぞれの待機時間
    private static final long QUICK_SPAWN_DELAY_MS = 21000L;
    private static final long NORMAL_SPAWN_DELAY_MS = 37000L;
    // Crystal使用後、DUSTパーティクル数を観測する時間。この間の検知数が閾値以下ならQuick Spawnと判定する
    private static final long CRYSTAL_PARTICLE_DETERMINATION_MS = 1000L;
    private static final int QUICK_SPAWN_THRESHOLD = 20;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Sanctuary外でCrystalを検知しUnknown扱いにしていた場合、実際にSanctuaryへ入ったタイミングで
            // そこからパーティクル観測を開始する(それまでは観測できないため)
            if (GameState.Arachne.unknownBigSpawn && GameState.Arachne.inSanctuary) {
                GameState.Arachne.unknownBigSpawn = false;
                GameState.Arachne.awaitingCrystalParticles = true;
                GameState.Arachne.crystalMessageTime = System.currentTimeMillis();
                GameState.Arachne.particleBurstCounter = 0;
            }

            // 観測時間が経過したら、その間に検知したDUSTパーティクル数(ArachneCrystalParticleMixinが加算)で
            // Quick Spawn(閾値以下)か通常スポーン(閾値超)かを確定する
            if (GameState.Arachne.awaitingCrystalParticles
                    && System.currentTimeMillis() - GameState.Arachne.crystalMessageTime > CRYSTAL_PARTICLE_DETERMINATION_MS) {
                long delay = GameState.Arachne.particleBurstCounter <= QUICK_SPAWN_THRESHOLD ? QUICK_SPAWN_DELAY_MS : NORMAL_SPAWN_DELAY_MS;
                GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + delay;
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
            GameState.Arachne.unknownBigSpawn = false;
            GameState.Arachne.spawnTargetTime = System.currentTimeMillis() + SPAWN_DELAY_SMALL_MS;
            return;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.ARACHNE_CRYSTAL_MSG)) {
            GameState.Arachne.isSummoning = true;
            GameState.Arachne.hasSpawned = false;
            GameState.Arachne.isReady = false;
            GameState.Arachne.size = "Big";
            if (GameState.Arachne.inSanctuary) {
                // Sanctuary内で検知できた場合のみ、DUSTパーティクルの観測でQuick/Normalを確定できる
                GameState.Arachne.spawnTargetTime = 0;
                GameState.Arachne.awaitingCrystalParticles = true;
                GameState.Arachne.crystalMessageTime = System.currentTimeMillis();
                GameState.Arachne.particleBurstCounter = 0;
                GameState.Arachne.unknownBigSpawn = false;
            } else {
                // Sanctuary外ではパーティクルを観測できないため、Sanctuaryに入るまでUnknown扱いにする
                GameState.Arachne.spawnTargetTime = 0;
                GameState.Arachne.awaitingCrystalParticles = false;
                GameState.Arachne.unknownBigSpawn = true;
            }
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
            GameState.Arachne.unknownBigSpawn = false;
        }
    }
}
