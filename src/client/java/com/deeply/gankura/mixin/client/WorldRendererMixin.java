package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class WorldRendererMixin {

    @Inject(at = @At("TAIL"), method = "render")
    private void renderGolemWaypoint(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress, CallbackInfo ci) {
        if (!ModConfig.showGolemWorldText) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

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
                    textToRender = "§e§lGOLEM §e(Soon)";
                } else {
                    textToRender = "§c§lGOLEM §c(Spawned)";
                }
            }
        }

        Vec3d eyePos = player.getEyePos();
        double distance = eyePos.distanceTo(Vec3d.ofCenter(renderPos));
        float scale = (float) (distance * 0.005);
        float textScale = Math.max(0.02f, Math.min(scale, 0.5f));

        GizmoDrawing.blockLabel(textToRender, renderPos, 0, textColor, textScale * 20);
    }
}