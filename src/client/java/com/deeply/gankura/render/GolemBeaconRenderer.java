package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.block.entity.state.BeaconBlockEntityRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.util.math.BlockPos;

public class GolemBeaconRenderer {

    private static final int MAX_BUILD_HEIGHT = 319;

    public static void submitBeaconState(WorldRenderState worldState, Camera camera) {
        // ★追加: World Location DisplayがOFFならビーコンも描画しない
        if (!ModConfig.INSTANCE.golem.showGolemWorldLocation_Beacon) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        if (!isTheEnd) return;

        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        if (!isStage4 && !isStage5) return;

        BlockPos basePos = GameState.Player.locationPos;
        BlockPos renderPos;

        if (isStage4) {
            renderPos = basePos.add(0, 1, -2);
        } else {
            renderPos = basePos.add(0, 0, -2);
        }

        int color = isStage4 ? 0xFFFFFFFF : 0xFFFF5555;

        BeaconBlockEntityRenderState state = new BeaconBlockEntityRenderState();
        state.pos = renderPos;
        state.blockState = Blocks.BEACON.getDefaultState();
        state.type = BlockEntityType.BEACON;
        state.lightmapCoordinates = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        state.crumblingOverlay = null;

        state.beamRotationDegrees = client.world != null ? Math.floorMod(client.world.getTime(), 40) + client.getRenderTickCounter().getTickProgress(true) : 0f;
        state.beamSegments.add(new BeaconBlockEntityRenderState.BeamSegment(color, MAX_BUILD_HEIGHT));

        float length = (float) camera.getCameraPos().subtract(renderPos.toCenterPos()).horizontalLength();
        state.beamScale = client.player != null && client.player.isUsingSpyglass() ? 1.0F : Math.max(1.0F, length / 96.0F);

        worldState.blockEntityRenderStates.add(state);
    }
}