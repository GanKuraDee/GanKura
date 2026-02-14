package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class LocationScanner {
    // private static int tickCounter = 0; // 削除

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ★変更: 制限を撤廃し、毎Tick実行して最速検知を目指す
            // if (tickCounter++ < 10) return;
            // tickCounter = 0;
            scan(client);
        });
    }

    private static void scan(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        // エリアチェック
        boolean isTargetMap = ModConstants.MODE_COMBAT_3.equals(GameState.mode)
                || ModConstants.MAP_THE_END.equals(GameState.map);
        if (!isTargetMap) return;

        String stage = GameState.golemStage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        // Stage 4/5 以外なら場所リセット
        if (!isStage4 && !isStage5) {
            if (!"None".equals(GameState.locationName)) {
                GameState.locationName = "None";
                GameState.locationPos = null;
            }
            return;
        }

        // 既に特定済みの場合は維持 (リセット防止)
        if (!"None".equals(GameState.locationName)) return;

        int yOffset = isStage5 ? 1 : 0;

        for (ModConstants.GolemSpot spot : ModConstants.GOLEM_SPOTS) {
            BlockPos targetPos = spot.pos().up(yOffset);
            if (client.world.getBlockState(targetPos).getBlock() == Blocks.SANDSTONE_STAIRS) {
                GameState.locationName = spot.name();
                GameState.locationPos = targetPos; // 座標も保存
                break;
            }
        }
    }
}