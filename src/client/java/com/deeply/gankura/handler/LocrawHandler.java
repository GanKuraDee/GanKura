package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

// ★復活: Timerを使うためのインポート
import java.util.Timer;
import java.util.TimerTask;

public class LocrawHandler {

    // サーバー接続時にlocrawを送信するスケジュール
    public static void scheduleLocraw(MinecraftClient client) {
        // ★修正: 0秒だと早すぎてFabricがエラーを吐くため、0.5秒(500ms)だけ待機させる
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.networkHandler.sendChatCommand("locraw");
                    }
                });
            }
        }, 500); // ここが 500 になっています
    }

    // チャットメッセージがlocrawのJSONか判定し、処理する
    public static boolean handleMessage(String msg) {
        if (msg.startsWith("{") && msg.contains("\"server\":")) {
            try {
                JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                if (json.has("server")) {
                    String newId = json.get("server").getAsString();

                    // サーバーが変わっていたらリセット
                    if (!newId.equals(GameState.Server.id)) {
                        GameState.Server.id = newId;
                        GameState.Golem.reset();
                    }

                    // 情報を更新
                    if (json.has("gametype")) GameState.Server.gametype = json.get("gametype").getAsString();
                    if (json.has("mode")) GameState.Server.mode = json.get("mode").getAsString();
                    if (json.has("map")) GameState.Server.map = json.get("map").getAsString();

                    return true; // 処理したのでtrue（チャット欄には表示しない）
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}