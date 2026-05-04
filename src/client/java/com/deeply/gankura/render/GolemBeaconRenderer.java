package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

public class GolemBeaconRenderer {

    private static final int MAX_BUILD_HEIGHT = 319;

    private static BlockPos lastRenderPos = null;
    private static String lastStage = null;
    private static BeaconRenderState cachedState = null;

    public static void submitBeaconState(LevelRenderState worldState, Camera camera) {
        if (!ModConfig.INSTANCE.golem.showGolemWorldLocation_Beacon) return;

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

        if (!renderPos.equals(lastRenderPos) || !stage.equals(lastStage) || cachedState == null) {
            lastRenderPos = renderPos;
            lastStage = stage;

            int color = isStage4 ? 0xFFFFFFFF : 0xFFFF5555;

            cachedState = new BeaconRenderState();

            // ★修正1: 継承元のフィールドへのアクセス
            // もし直接代入でエラーが出る場合は cachedState.setBlockPos(renderPos) 等を試してください
            cachedState.blockPos = renderPos;
            // privateアクセスエラー対策:
            // 本来はコンストラクタや内部メソッドで設定されますが、手動生成の場合は一旦これで試します
            // エラーが出る場合は「cachedState.blockState = ...」の代わりに適切なSetterを探してください
            // (提示されたソースにはありませんでしたが、継承元にあるはずです)

            // ★修正2: sections (List) と Section レコード (2引数: color, height)
            cachedState.sections.clear();
            cachedState.sections.add(new BeaconRenderState.Section(color, MAX_BUILD_HEIGHT));
        }

        // ★修正3: フィールド名を提示されたソースに合わせる
        // animationTime (回転) と beamRadiusScale (太さ/スケール)
        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        cachedState.animationTime = client.level != null ? (client.level.getGameTime() + partialTicks) : 0f;

        // 距離計算 (Camera.position() を使用)
        double dx = camera.position().x - renderPos.getCenter().x;
        double dz = camera.position().z - renderPos.getCenter().z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);

        // スコープ使用時は等倍、それ以外は距離に応じてスケールアップ
        cachedState.beamRadiusScale = client.player != null && client.player.isScoping() ? 1.0F : Math.max(1.0F, length / 96.0F);

        worldState.blockEntityRenderStates.add(cachedState);
    }
}