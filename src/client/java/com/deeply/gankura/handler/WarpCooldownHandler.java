package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

public class WarpCooldownHandler {
    private static final long COOLDOWN_MS = 5000L;

    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(WarpCooldownHandler::onCommand);
        ClientTickEvents.END_CLIENT_TICK.register(WarpCooldownHandler::onTick);
    }

    private static boolean onCommand(String command) {
        if (!ModConfig.INSTANCE.misc.enableWarpQueue || !isWarpCommand(command)) return true;

        long now = System.currentTimeMillis();
        if (now < GameState.Warp.cooldownEndAt) {
            GameState.Warp.queuedCommand = command;
            return false;
        }

        GameState.Warp.cooldownEndAt = now + COOLDOWN_MS;
        return true;
    }

    private static void onTick(Minecraft client) {
        if (GameState.Warp.cooldownEndAt == 0 || System.currentTimeMillis() < GameState.Warp.cooldownEndAt) return;

        GameState.Warp.cooldownEndAt = 0;
        String queued = GameState.Warp.queuedCommand;
        GameState.Warp.queuedCommand = null;
        // 再送信は onCommand() を再度通過するため、そちら側で次のクールダウンが開始される
        if (queued != null && client.player != null) {
            client.player.connection.sendCommand(queued);
        }
    }

    private static boolean isWarpCommand(String command) {
        return command.equalsIgnoreCase("warp") || command.regionMatches(true, 0, "warp ", 0, 5);
    }
}
