package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;

public class EntityTracerRenderer {

    public static void emitGizmos(Minecraft client) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;
        if (client.player == null) return;
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        Vec3 eyePos = client.gameRenderer.getMainCamera().position();
        Vec3 startPos = eyePos.add(client.player.getLookAngle().scale(0.2));

        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            int color;

            // Crimson Isle ボス（Wither Skeleton を含む）
            CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
            if (boss != null) {
                if (!boss.enableTracer().get()) continue;
                // Bladesoul は Blaze + Wither Skeleton の合体構成。Tracer は Wither Skeleton のみ
                if ("Bladesoul".equals(boss.nameTag()) && !(entity instanceof WitherSkeleton)) continue;
                color = boss.tracerColorARGB();
            }
            // Magma Glare (Magma Boss 配下の MagmaCube)
            else if (EntityHighlightManager.magmaGlareEntities.contains(entity)) {
                if (!ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTracer) continue;
                color = 0xFFFF5555; // 赤
            }
            // Arachne (Broodmother と同じ Spider 型のため instanceof 判定より先に判定する)
            else if (EntityHighlightManager.arachneEntities.contains(entity)) {
                if (!ModConfig.INSTANCE.spidersDen.enableArachneTracer) continue;
                color = 0xFFAA00AA; // 紫
            }
            // Arachne's Brood (Arachne 分裂後の Spider/CaveSpider 系のため instanceof 判定より先に判定する)
            else if (EntityHighlightManager.arachneBroodEntities.contains(entity)) {
                if (!ModConfig.INSTANCE.spidersDen.enableArachneTracer) continue;
                color = 0xFFFF55FF; // 明るい紫
            }
            // Golem / Broodmother / Dragon
            else if (entity instanceof IronGolem) {
                if (!ModConfig.INSTANCE.theEnd.enableGolemTracer) continue;
                color = 0xFFFFAA00;
            } else if (entity instanceof Spider) {
                if (!ModConfig.INSTANCE.spidersDen.enableBroodmotherTracer) continue;
                color = 0xFFFF5555;
            } else if (entity instanceof EnderDragon) {
                if (!ModConfig.INSTANCE.theEnd.enableDragonTracer) continue;
                color = 0xFF000000 | dragonColor(GameState.Dragon.type);
            } else {
                continue; // 該当なし
            }

            Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            GizmoProperties props = Gizmos.addGizmo(
                new LineGizmo(startPos, entityCenter, color, 4.0f)
            );
            props.setAlwaysOnTop();
        }
    }

    private static int dragonColor(String type) {
        if (type == null) return 0xFF55FF;
        return switch (type) {
            case "Protector" -> 0x555555;
            case "Old"       -> 0xAAAAAA;
            case "Unstable"  -> 0xAA00AA;
            case "Young"     -> 0xFFFFFF;
            case "Strong"    -> 0xFF5555;
            case "Wise"      -> 0x55FFFF;
            case "Superior"  -> 0xFFFF55;
            default          -> 0xFF55FF;
        };
    }
}
