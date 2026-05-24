package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public class WorldTextRenderer {

    public static void render(MinecraftClient client, float tickProgress) {
        if (client.player == null) return;

        renderGolemWaypoint(client, client.player);
        renderBossTracers(client, tickProgress);
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

    private static void renderBossTracers(MinecraftClient client, float tickProgress) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        // 目線から視線方向へ0.5ブロック前方にオフセット
        // → 一人称時にカメラ原点と始点が重ならず画面中央から線が見える
        Vec3d eyePos = player.getLerpedPos(tickProgress)
                .add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d from = eyePos.add(player.getRotationVec(tickProgress).multiply(0.5));

        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            int color;
            if (entity instanceof IronGolemEntity) {
                if (!ModConfig.INSTANCE.golem.enableGolemTracer) continue;
                color = 0xFFFFAA00;
            } else if (entity instanceof SpiderEntity) {
                if (!ModConfig.INSTANCE.broodmother.enableBroodmotherTracer) continue;
                color = 0xFFFF5555;
            } else if (entity instanceof EnderDragonEntity) {
                if (!ModConfig.INSTANCE.dragon.enableDragonTracer) continue;
                color = 0xFFFF55FF;
            } else {
                continue;
            }

            // エンティティの補間済み中心位置
            Vec3d to = entity.getLerpedPos(tickProgress).add(0, entity.getHeight() / 2.0, 0);
            GizmoDrawing.line(from, to, color, 4.0f).ignoreOcclusion();
        }
    }
}