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
            PetHandler.reset(); // ★追加: サーバー移動時にペットのスキャン状態をリセット

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
            // 処理しやすいように装飾コードを除いた文字列を作る
            String unformattedMsg = msg.replaceAll("§[0-9a-fk-or]", "");

            // ★追加: アクションバーのメッセージの場合はアーマースタックを解析して終了
            if (overlay) {
                // ★変更: unformattedMsg ではなく、生の message(Textオブジェクト) を渡す！
                ArmorStackHandler.handleActionBar(message);
                return true;
            }

            if (LocrawHandler.handleMessage(msg)) {
                return false;
            }

            // ★変更: 色が消える前の「message(Textオブジェクト)」を直接渡す！
            PetHandler.handleMessage(message);

            // ★追加: 1.5. Dragon Spawn 検知 (DragonHandlerに委譲)
            if (DragonHandler.handleMessage(msg, MinecraftClient.getInstance())) {
                return true;
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