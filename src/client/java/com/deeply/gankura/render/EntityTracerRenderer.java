package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;

public class EntityTracerRenderer {

    public static void emitGizmos(Minecraft client) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;
        if (client.player == null) return;
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        Vec3 eyePos = client.gameRenderer.getMainCamera().position();
        // カメラ位置をそのまま使うと射影後の w_clip=0 になり GPU がラインを破棄する。
        // プレイヤーの視線方向へ 0.2 ブロックずらして近平面より手前を確保する。
        Vec3 startPos = eyePos.add(client.player.getLookAngle().scale(0.2));

        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            if (entity instanceof IronGolem   && !ModConfig.INSTANCE.golem.enableGolemTracer)             continue;
            if (entity instanceof Spider       && !ModConfig.INSTANCE.broodmother.enableBroodmotherTracer) continue;
            if (entity instanceof EnderDragon  && !ModConfig.INSTANCE.dragon.enableDragonTracer)           continue;

            Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            GizmoProperties props = Gizmos.addGizmo(
                new LineGizmo(startPos, entityCenter, getTracerColor(entity), LineGizmo.DEFAULT_WIDTH)
            );
            props.setAlwaysOnTop();
        }
    }

    private static int getTracerColor(Entity entity) {
        if (entity instanceof IronGolem)   return 0xFFFFAA00;
        if (entity instanceof Spider)      return 0xFFFF5555;
        if (entity instanceof EnderDragon) return 0xFFFF55FF;
        return 0xFFFFFFFF;
    }
}
