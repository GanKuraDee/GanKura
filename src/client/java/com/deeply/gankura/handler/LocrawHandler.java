package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.util.Timer;
import java.util.TimerTask;

public class LocrawHandler {

    // サーバー接続時にlocrawを送信するスケジュール
    public static void scheduleLocraw(MinecraftClient client) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (client.player != null) {
                    client.execute(() -> client.player.networkHandler.sendChatCommand("locraw"));
                }
            }
        }, 1000);
    }

    // チャットメッセージがlocrawのJSONか判定し、処理する
    public static boolean handleMessage(String msg) {
        if (msg.startsWith("{") && msg.contains("\"server\":")) {
            try {
                JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                if (json.has("server")) {
                    String newId = json.get("server").getAsString();

                    // サーバーが変わっていたらリセット
                    if (!newId.equals(GameState.serverId)) {
                        GameState.serverId = newId;
                        GameState.resetGolemStatus();
                    }

                    // 情報を更新
                    if (json.has("gametype")) GameState.gametype = json.get("gametype").getAsString();
                    if (json.has("mode")) GameState.mode = json.get("mode").getAsString();
                    if (json.has("map")) GameState.map = json.get("map").getAsString();

                    return true; // 処理したのでtrue（チャット欄には表示しない）
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}