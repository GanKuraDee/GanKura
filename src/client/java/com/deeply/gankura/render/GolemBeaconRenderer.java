package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.core.BlockPos;

public class GolemBeaconRenderer {

    private static final int MAX_BUILD_HEIGHT = 319;

    // ★修正1: static の cachedState はマルチスレッド描画でバグるため廃止しました
    private static BlockPos lastRenderPos = null;
    private static String lastStage = null;

    public static void submitBeaconState(LevelRenderState worldState, Camera camera) {
        if (!ModConfig.INSTANCE.theEnd.showGolemWorldLocation_Beacon) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        if (!isTheEnd) return;

        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        if (!isStage4 && !isStage5) return;

        BlockPos basePos = GameState.Player.locationPos;
        BlockPos renderPos = isStage4 ? basePos.offset(0, 1, -2) : basePos.offset(0, 0, -2);
        int color = isStage4 ? 0xFFFFFFFF : 0xFFFF5555;

        // ★修正2: 毎フレーム新しい State を作成し、スレッド間のデータ競合を防ぐ
        BeaconRenderState state = new BeaconRenderState();
        state.blockPos = renderPos;
        state.blockEntityType = net.minecraft.world.level.block.entity.BlockEntityType.BEACON;

        state.sections.clear();
        state.sections.add(new BeaconRenderState.Section(color, MAX_BUILD_HEIGHT));

        // ★修正3: floatの桁落ち（精度低下によるカクつき）対策
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        long gameTime = client.level.getLevelData().getGameTime();

        // Math.floorMod を使って巨大な時間を 24000 の余りに変換し、
        // 小数点以下(partialTicks)が切り捨てられずに滑らかにアニメーションするようにします
        state.animationTime = (Math.floorMod(gameTime, 24000) + partialTicks);

        double dx = camera.position().x - renderPos.getCenter().x;
        double dz = camera.position().z - renderPos.getCenter().z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);

        state.beamRadiusScale = client.player != null && client.player.isScoping() ? 1.0F : Math.max(1.0F, length / 96.0F);

        // 描画キューに追加
        worldState.blockEntityRenderStates.add(state);
    }
}