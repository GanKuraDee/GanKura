package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos; // 26.1.2 での新しい描画クラス

public class WorldTextRenderer {

    public static void render(Minecraft client) {
        if (client.player == null) return;
        renderGolemWaypoint(client, client.player);
    }

    private static void renderGolemWaypoint(Minecraft client, Player player) {
        if (!ModConfig.INSTANCE.theEnd.showGolemWorldLocation_Text) return;
        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return;

        String stage = GameState.Golem.stage;
        boolean isStage4 = ModConstants.STAGE_AWAKENING.equals(stage);
        boolean isStage5 = ModConstants.STAGE_SUMMONED.equals(stage);

        if (!isStage4 && !isStage5) return;

        BlockPos basePos = GameState.Player.locationPos;
        BlockPos renderPos = isStage4 ? basePos.offset(0, 1, -2) : basePos.offset(0, 0, -2);
        int textColor;
        String textToRender;

        if (isStage4) {
            textColor = 0xFFFFFFFF;
            textToRender = "§f§lGOLEM";
        } else {
            textColor = 0xFFFF5555;
            long timeSincePacket = System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis;
            double estimatedServerTime = GameState.Server.lastTimePacket + (Math.min(timeSincePacket, 1000) / 50.0);
            double remainingTicks = Math.max(0, GameState.Golem.stage5TargetTime - estimatedServerTime);

            if (remainingTicks > 0) {
                textToRender = String.format("§c§lGOLEM §c(%.1fs)", remainingTicks / 20.0);
            } else {
                textToRender = (!GameState.Golem.hasRisen && !"None".equals(GameState.Player.locationName))
                        ? "§c§lGOLEM §e(Soon)" : "§c§lGOLEM §c(Spawned)";
            }
        }

        Vec3 eyePos = player.getEyePosition();
        double distance = eyePos.distanceTo(Vec3.atCenterOf(renderPos));

        // スケール計算
        float textScale = (float) Math.max(0.02f, Math.min(distance * 0.005, 0.5f));

        /*
         * 26.1.2 仕様: Gizmos クラスを使用した描画
         * メソッド名: billboardText (カメラを常に正対するテキスト)
         * 引数構成は環境により多少異なりますが、一般的に以下が標準です。
         */
        TextGizmo.Style style = TextGizmo.Style.forColorAndCentered(textColor)
                .withScale(textScale * 20.0F);

        // 座標を作成
        Vec3 pos = new Vec3(renderPos.getX() + 0.5, renderPos.getY() + 1.5, renderPos.getZ() + 0.5);

        // ギズモを追加
        GizmoProperties properties = Gizmos.billboardText(textToRender, pos, style);

        // 「壁越しに描画」を有効にする
        properties.setAlwaysOnTop();
    }
}