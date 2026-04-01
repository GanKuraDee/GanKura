package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public class WorldTextRenderer {

    // Mixinからはこのメソッドだけが呼ばれる
    public static void render(MinecraftClient client) {
        if (client.player == null) return;

        // 今後、描画要素が増えたらここに足していくだけ！
        // 例: renderBroodmotherWaypoint(client, client.player);
        renderGolemWaypoint(client, client.player);
    }

    private static void renderGolemWaypoint(MinecraftClient client, PlayerEntity player) {
        if (!ModConfig.INSTANCE.golem.showGolemWorldLocation_Text) return;

        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        if (!isStage4 && !isStage5) return;

        BlockPos basePos = GameState.Player.locationPos;
        BlockPos renderPos;
        int textColor;
        String textToRender;

        if (isStage4) {
            renderPos = basePos.add(0, 1, -2);
            textColor = 0xFFFFFFFF;
            textToRender = "§f§lGOLEM";
        } else {
            renderPos = basePos.add(0, 0, -2);
            textColor = 0xFFFF5555;

            long timeSincePacket = System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis;
            if (timeSincePacket > 1000) {
                timeSincePacket = 1000;
            }
            double estimatedServerTime = GameState.Server.lastTimePacket + (timeSincePacket / 50.0);
            double remainingTicks = GameState.Golem.stage5TargetTime - estimatedServerTime;

            if (remainingTicks < 0) remainingTicks = 0;

            if (remainingTicks > 0) {
                textToRender = String.format("§c§lGOLEM §c(%.1fs)", remainingTicks / 20.0);
            } else {
                if (!GameState.Golem.hasRisen && !"None".equals(GameState.Player.locationName)) {
                    textToRender = "§c§lGOLEM §e(Soon)";
                } else {
                    textToRender = "§c§lGOLEM §c(Spawned)";
                }
            }
        }

        Vec3d eyePos = player.getEyePos();
        double distance = eyePos.distanceTo(Vec3d.ofCenter(renderPos));
        float textScale = (float) (distance * 0.005);
        textScale = Math.max(0.02f, Math.min(textScale, 0.5f));

        GizmoDrawing.blockLabel(textToRender, renderPos, 0, textColor, textScale * 20);
    }
}