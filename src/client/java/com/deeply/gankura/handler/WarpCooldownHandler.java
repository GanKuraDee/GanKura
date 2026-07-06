package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

public class WarpCooldownHandler {
    private static final long COOLDOWN_MS = 5000L;
    // 無効なWarp名などサーバーが「Warping...」以外(エラーメッセージ等)を返した場合に、
    // 確認待ち状態のまま固まってしまわないようにするためのタイムアウト
    private static final long CONFIRMATION_TIMEOUT_MS = 3000L;

    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(WarpCooldownHandler::onCommand);
        ClientTickEvents.END_CLIENT_TICK.register(WarpCooldownHandler::onTick);
    }

    private static boolean onCommand(String command) {
        if (!ModConfig.INSTANCE.misc.enableWarpQueue || !isWarpCommand(command)) return true;

        long now = System.currentTimeMillis();
        if (GameState.Warp.awaitingConfirmation || now < GameState.Warp.cooldownEndAt) {
            GameState.Warp.queuedCommand = command;
            return false;
        }

        // 実際のクールダウンはチャットに「Warping...」が表示されてから開始する
        GameState.Warp.awaitingConfirmation = true;
        GameState.Warp.awaitingConfirmationSince = now;
        return true;
    }

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg) {
        if (GameState.Warp.awaitingConfirmation && ModConstants.containsIgnoreCase(unformattedMsg, "warping")) {
            GameState.Warp.awaitingConfirmation = false;
            GameState.Warp.cooldownEndAt = System.currentTimeMillis() + COOLDOWN_MS;
        }
    }

    private static void onTick(MinecraftClient client) {
        // 確認待ちが一定時間続いた場合(無効な引数などで「Warping...」が返ってこなかった場合)は
        // 待機状態を解除し、キューされたコマンドがあれば送信する
        if (GameState.Warp.awaitingConfirmation
                && System.currentTimeMillis() - GameState.Warp.awaitingConfirmationSince > CONFIRMATION_TIMEOUT_MS) {
            GameState.Warp.awaitingConfirmation = false;
            dispatchQueuedCommand(client);
            return;
        }

        if (GameState.Warp.cooldownEndAt == 0 || System.currentTimeMillis() < GameState.Warp.cooldownEndAt) return;

        GameState.Warp.cooldownEndAt = 0;
        dispatchQueuedCommand(client);
    }

    // 再送信は onCommand() を再度通過するため、そちら側で awaitingConfirmation が再度立てられる
    private static void dispatchQueuedCommand(MinecraftClient client) {
        String queued = GameState.Warp.queuedCommand;
        GameState.Warp.queuedCommand = null;
        if (queued != null && client.player != null) {
            client.player.networkHandler.sendChatCommand(queued);
        }
    }

    private static boolean isWarpCommand(String command) {
        return command.equalsIgnoreCase("warp") || command.regionMatches(true, 0, "warp ", 0, 5);
    }
}
