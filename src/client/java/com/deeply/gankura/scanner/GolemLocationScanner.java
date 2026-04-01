package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class GolemLocationScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            scan(client);
        });
    }

    private static void scan(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        // エリアチェック
        boolean isTargetMap = ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode)
                || ModConstants.MAP_THE_END.equals(GameState.Server.map);
        if (!isTargetMap) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        // Stage 4/5 以外なら場所リセット
        if (!isStage4 && !isStage5) {
            if (!"None".equals(GameState.Player.locationName)) {
                GameState.Player.locationName = "None";
                GameState.Player.locationPos = null;
            }
            return;
        }

        // 既に特定済みの場合は維持 (リセット防止)
        if (!"None".equals(GameState.Player.locationName)) return;

        int yOffset = isStage5 ? 1 : 0;

        for (ModConstants.GolemSpot spot : ModConstants.GOLEM_SPOTS) {
            BlockPos targetPos = spot.pos().up(yOffset);
            if (client.world.getBlockState(targetPos).getBlock() == Blocks.SANDSTONE_STAIRS) {
                GameState.Player.locationName = spot.name();
                GameState.Player.locationPos = targetPos; // 座標も保存
                break;
            }
        }
    }
}