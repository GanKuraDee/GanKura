package com.deeply.gankura.handler;

import com.deeply.gankura.data.ModConfig; // ★追加
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;

public class DragonHandler {

    public static boolean handleMessage(String msg, MinecraftClient client) {
        // ドラゴンのスポーンメッセージかどうかをチェック
        Matcher matcher = ModConstants.DRAGON_SPAWN_PATTERN.matcher(msg);

        if (matcher.find()) {
            String dragonType = matcher.group(1);

            // ★変更: 設定でオンになっている場合のみ通知を表示する
            if (ModConfig.enableDragonAlerts) {
                client.execute(() -> NotificationUtils.showDragonSpawnAlert(client, dragonType));
            }

            return true; // 設定がオフでも、メッセージ自体はドラゴンのものなので true で処理完了とする
        }

        return false;
    }
}