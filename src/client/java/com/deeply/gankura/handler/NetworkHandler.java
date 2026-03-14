package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
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
            GameState.resetAll(); // 全状態リセット
            PetHandler.reset(); // サーバー移動時にペットのスキャン状態をリセット

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

            // アクションバーのメッセージの場合はアーマースタックを解析して終了
            if (overlay) {
                ArmorStackHandler.handleActionBar(message);
                return true;
            }

            // サーバーリブート検知 (一番最初の方で判定する)
            ServerRestartHandler.handleChat(unformattedMsg, MinecraftClient.getInstance());

            if (LocrawHandler.handleMessage(msg)) {
                return false;
            }

            // ペットの処理
            PetHandler.handleMessage(message);

            // 1.5. Dragon Spawn 検知 (DragonHandlerに委譲)
            if (DragonHandler.handleMessage(msg, MinecraftClient.getInstance())) {
                return true;
            }

            // 2. Stage 5 Spawn Timer 検知
            // ★修正: TabListScanner から GolemHandler に呼び出し先を変更！
            if (msg.contains(ModConstants.GOLEM_SPAWN_MSG)) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> GolemHandler.setStageToSummoned(client));
                return true;
            }

            // 3. 戦闘・統計データの処理 (GolemHandlerに委譲)
            // 開始、終了、データ収集、結果計算のすべてをここで判断
            GolemHandler.handleMessage(msg, MinecraftClient.getInstance());

            return true;
        });
    }
}