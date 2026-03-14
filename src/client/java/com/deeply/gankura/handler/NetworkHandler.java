package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
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
            GameState.resetAll();
            PetHandler.reset();

            if (!client.isInSingleplayer() && handler.getServerInfo() != null) {
                String ip = handler.getServerInfo().address.toLowerCase();
                if (ip.contains("hypixel.net")) {
                    LocrawHandler.scheduleLocraw(client);
                }
            }
        });

        // チャット受信時の処理 (純粋なルーター/ディスパッチャー)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // 1. アクションバーのメッセージは専用ハンドラーへ
            if (overlay) {
                ArmorStackHandler.handleActionBar(message);
                return true;
            }

            String msg = message.getString();
            String unformattedMsg = msg.replaceAll("§[0-9a-fk-or]", "");
            MinecraftClient client = MinecraftClient.getInstance();

            // 2. システム・インフラ系 (チャットを非表示にする可能性があるもの)
            if (LocrawHandler.handleMessage(msg)) {
                return false; // locrawのJSONデータは画面に出さず隠す
            }

            // 3. 各ドメイン(機能)への純粋な委譲
            // NetworkHandler自身は、メッセージの中身が何なのか一切気にせず担当者に投げるだけ！
            ServerRestartHandler.handleChat(unformattedMsg, client);
            PetHandler.handleMessage(message);

            if (DragonHandler.handleMessage(msg, client)) {
                return true;
            }

            // Golemに関する全てのチャット処理を委譲
            GolemHandler.handleMessage(msg, client);

            return true;
        });
    }
}