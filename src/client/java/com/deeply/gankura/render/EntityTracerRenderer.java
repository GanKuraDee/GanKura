package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

// 対象と色は tick 側(EntityHighlightManager)で決まっているので、ここでは線を引くだけ。
// Highlight とは独立した集合を見るため、Highlight を切っていても Tracer 単独で使える
public class EntityTracerRenderer {

    public static void emitGizmos(Minecraft client, float partialTicks) {
        if (EntityHighlightManager.tracerEntities.isEmpty()) return;
        if (client.player == null) return;
        if (!GameState.Server.isSkyblock()) return;

        // 三人称ではカメラがプレイヤーの後方へ離れるため、カメラ位置を始点にすると
        // 線が背後から伸びているように見える。視点に関係なくプレイヤーの目の位置を始点にし、
        // 一人称でカメラ原点と重なって線が見えなくなる分だけ視線方向へずらす
        Vec3 eyePos = client.player.getEyePosition(partialTicks);
        Vec3 startPos = eyePos.add(client.player.getViewVector(partialTicks).scale(0.2));

        for (Map.Entry<Entity, Integer> entry : EntityHighlightManager.tracerEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (entity.isRemoved()) continue;

            Vec3 entityCenter = entity.getPosition(partialTicks).add(0, entity.getBbHeight() / 2.0, 0);
            GizmoProperties props = Gizmos.addGizmo(
                new LineGizmo(startPos, entityCenter, entry.getValue(), 4.0f)
            );
            props.setAlwaysOnTop();
        }
    }
}
