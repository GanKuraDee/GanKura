package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;

/**
 * Boss Corleone のスポーン通知。
 *
 * 見つけているかどうかは EntityHighlightManager が毎tick判定するので、
 * ここではその結果が切り替わった瞬間だけタイトルを出す。
 * Mob Visuals で対象に選んでいなくても通知だけは出せるよう、表示設定とは切り離している。
 */
public class CorleoneHandler {

    public static void update(MinecraftClient client, boolean present) {
        if (GameState.Corleone.isDetected == present) return;

        GameState.Corleone.isDetected = present;
        if (present) announce(client);
    }

    public static void reset() {
        GameState.Corleone.isDetected = false;
    }

    private static void announce(MinecraftClient client) {
        if (!ModConfig.INSTANCE.crystalHollows.enableCorleoneSpawnTitle) return;

        MutableText title = Text.literal("BOSS CORLEONE").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        NotificationUtils.showTitle(client, title, null);
    }
}
