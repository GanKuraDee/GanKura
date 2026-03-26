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

    // 3D描画の限界高度
    private static final int MAX_BUILD_HEIGHT = 319;

    public static void submitBeaconState(WorldRenderState worldState, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        // ジ・エンドにいるかチェック
        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        if (!isTheEnd) return;

        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        if (!isStage4 && !isStage5) return;

        BlockPos basePos = GameState.Player.locationPos;
        BlockPos renderPos;

        // テキストの位置(WorldTextRenderer)と完全に同期
        if (isStage4) {
            renderPos = basePos.add(0, 1, -2);
        } else {
            renderPos = basePos.add(0, 0, -2);
        }

        // Stage 4なら白(0xFFFFFFFF), Stage 5なら赤(0xFFFF5555)のARGBカラーコード
        int color = isStage4 ? 0xFFFFFFFF : 0xFFFF5555;

        // =======================================================
        // ★ 1.21.11 最新のRenderStateアプローチ (Yarn環境に完全適応)
        // =======================================================
        BeaconBlockEntityRenderState state = new BeaconBlockEntityRenderState();

        // Yarn環境の正しい変数名で代入
        state.pos = renderPos;
        state.blockState = Blocks.BEACON.getDefaultState(); // publicなので直接代入可能！
        state.type = BlockEntityType.BEACON;
        state.lightmapCoordinates = LightmapTextureManager.MAX_LIGHT_COORDINATE; // FULL_BRIGHT
        state.crumblingOverlay = null; // 破壊されていない状態

        // ビームのアニメーション時間（流れる速度と回転）
        state.beamRotationDegrees = client.world != null ? Math.floorMod(client.world.getTime(), 40) + client.getRenderTickCounter().getTickProgress(true) : 0f;

        // ビームのセグメント（色と高さ）を追加
        state.beamSegments.add(new BeaconBlockEntityRenderState.BeamSegment(color, MAX_BUILD_HEIGHT));

        // Mixinから受け取ったカメラ位置を元に距離を計算し、ビームの太さを最適化
        float length = (float) camera.getCameraPos().subtract(renderPos.toCenterPos()).horizontalLength();
        state.beamScale = client.player != null && client.player.isUsingSpyglass() ? 1.0F : Math.max(1.0F, length / 96.0F);

        // マイクラ本体の描画キューに「偽ビーコン」を追加！
        worldState.blockEntityRenderStates.add(state);
    }
}