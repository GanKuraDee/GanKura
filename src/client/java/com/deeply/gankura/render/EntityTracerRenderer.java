package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Camera;
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
        if (client.player == null) return;
        if (!GameState.Server.isSkyblock()) return;

        // Stage 4/5 の End Stone Protector は、まだ湧いていないので指す相手のエンティティが無い。
        // 代わりにワールド上へ出しているテキストと同じ位置・同じ色で線を引く
        WorldTextRenderer.GolemAnchor golem = ModConfig.INSTANCE.theEnd.showGolemWorldLocation_Tracer
                ? WorldTextRenderer.golemAnchor()
                : null;
        if (EntityHighlightManager.tracerEntities.isEmpty() && golem == null) return;

        // 三人称ではカメラがプレイヤーの後方へ離れるため、カメラ位置を始点にすると
        // 線が背後から伸びているように見えるので、その場合だけプレイヤーの目の位置を使う。
        //
        // 一人称ではカメラ位置をそのまま始点にする。しゃがみ中のカメラは
        // 目の高さへ毎フレーム少しずつ近づく形で補間されており、getEyePosition() の
        // 高さとは一致しない。その差がそのまま線の向きのずれになるため、
        // カメラを基準にすることで始点を画面中央に固定する。
        // 始点がカメラ原点と重なって線が見えなくなる分だけ視線方向へずらす
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 basePos = camera.isDetached() ? client.player.getEyePosition(partialTicks) : camera.position();
        Vec3 startPos = basePos.add(client.player.getViewVector(partialTicks).scale(0.2));

        if (golem != null) {
            GizmoProperties golemProps = Gizmos.addGizmo(
                new LineGizmo(startPos, WorldTextRenderer.labelPos(golem.pos()), golem.argb(), 4.0f)
            );
            golemProps.setAlwaysOnTop();
        }

        for (Map.Entry<Entity, Integer> entry : EntityHighlightManager.tracerEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (entity.isRemoved()) continue;

            Vec3 entityCenter = entity.getPosition(partialTicks).add(0, EntityHighlightManager.renderAnchorHeight(entity), 0);
            GizmoProperties props = Gizmos.addGizmo(
                new LineGizmo(startPos, entityCenter, entry.getValue(), 4.0f)
            );
            props.setAlwaysOnTop();
        }
    }
}
