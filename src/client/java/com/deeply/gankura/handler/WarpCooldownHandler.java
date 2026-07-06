package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

public class WarpCooldownHandler {
    private static final long COOLDOWN_MS = 5000L;

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
        return true;
    }

    // NetworkHandler のチャットディスパッチャーから呼ばれる
    public static void handleMessage(String unformattedMsg) {
        if (GameState.Warp.awaitingConfirmation
                && (unformattedMsg.contains("Warping...") || unformattedMsg.contains("Warping you to your Skyblock Island..."))) {
            GameState.Warp.awaitingConfirmation = false;
            GameState.Warp.cooldownEndAt = System.currentTimeMillis() + COOLDOWN_MS;
        }
    }

    private static void onTick(MinecraftClient client) {
        if (GameState.Warp.cooldownEndAt == 0 || System.currentTimeMillis() < GameState.Warp.cooldownEndAt) return;

        GameState.Warp.cooldownEndAt = 0;
        String queued = GameState.Warp.queuedCommand;
        GameState.Warp.queuedCommand = null;
        // 再送信は onCommand() を再度通過するため、そちら側で awaitingConfirmation が再度立てられる
        if (queued != null && client.player != null) {
            client.player.networkHandler.sendChatCommand(queued);
        }
    }

    private static boolean isWarpCommand(String command) {
        return command.equalsIgnoreCase("warp") || command.regionMatches(true, 0, "warp ", 0, 5);
    }
}
