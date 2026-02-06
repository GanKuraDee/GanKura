package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.scanner.StageScanner;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("NetworkHandler");

    public static void init() {
        // サーバー参加・移動時の処理
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GameState.reset(); // 全状態リセット

            if (!client.isInSingleplayer() && handler.getServerInfo() != null) {
                String ip = handler.getServerInfo().address.toLowerCase();
                if (ip.contains("hypixel.net")) {
                    // LocrawHandlerに処理を委譲
                    LocrawHandler.scheduleLocraw(client);
                }
            }
        });

        // チャット受信時の処理
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String msg = message.getString();

            // 1. locraw解析 (LocrawHandlerに委譲)
            // locrawだった場合は true が返り、チャット欄には表示しない
            if (LocrawHandler.handleMessage(msg)) {
                return false;
            }

            // 2. Stage 5 Spawn Timer 検知 (StageScannerに委譲)
            if (msg.contains(ModConstants.GOLEM_SPAWN_MSG)) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> StageScanner.setStageToSummoned(client));
                return true;
            }

            // 3. 戦闘・統計データの処理 (CombatStatsHandlerに委譲)
            // 開始、終了、データ収集、結果計算のすべてをここで判断
            CombatStatsHandler.handleMessage(msg, MinecraftClient.getInstance());

            return true;
        });
    }
}