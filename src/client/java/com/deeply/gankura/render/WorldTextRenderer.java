package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public class WorldTextRenderer {

    public static void render(MinecraftClient client, float tickProgress) {
        if (client.player == null) return;

        renderGolemWaypoint(client, client.player);
        renderCrimsonBossWaypoints(client.player);
        renderArachneWaypoint(client, client.player);
        renderBossTracers(client, tickProgress);
    }

    private static void renderGolemWaypoint(MinecraftClient client, PlayerEntity player) {
        if (!ModConfig.INSTANCE.theEnd.showGolemWorldLocation_Text) return;

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
            if (GameState.Golem.stage4StartTime > 0) {
                long secs = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                String col = secs >= 480 ? "§c" : (secs >= 240 ? "§e" : "§f");
                textToRender += String.format(" %s(%dm %ds)", col, secs / 60, secs % 60);
            }
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
                    textToRender = "§c§lGOLEM §e(Soon)";
                } else {
                    textToRender = "§c§lGOLEM §c(Spawned)";
                }
            }
        }

        Vec3d eyePos = player.getEyePos();
        double distance = eyePos.distanceTo(Vec3d.ofCenter(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);

        GizmoDrawing.blockLabel(textToRender, renderPos, 0, textColor, textScale * 20);
    }

    private static void renderArachneWaypoint(MinecraftClient client, PlayerEntity player) {
        if (!ModConfig.INSTANCE.spidersDen.showArachneWorldText) return;
        if (!ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map)) return;

        BlockPos renderPos = ModConstants.ARACHNE_ALTAR_POS;
        boolean inSanctuary = GameState.Arachne.inSanctuary;
        int textColor;
        String textToRender;

        if (GameState.Arachne.isReady) {
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE";
        } else if (GameState.Arachne.hasSpawned) {
            textColor = 0xFFFF5555;
            textToRender = inSanctuary ? "§c§lARACHNE §c(Spawned)" : "§c§lARACHNE §6(Spawned/Killed)";
        } else if (GameState.Arachne.isSummoning) {
            textColor = 0xFFFF5555;
            if (GameState.Arachne.awaitingCrystalParticles) {
                textToRender = "§c§lARACHNE §c(...)";
            } else {
                long timeSincePacket = Math.min(System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis, 1000);
                double remainingTicks = Math.max(0, GameState.Arachne.spawnTargetTime - (GameState.Server.lastTimePacket + (timeSincePacket / 50.0)));
                if (remainingTicks > 0) {
                    textToRender = String.format("§c§lARACHNE §c(%.1fs)", remainingTicks / 20.0);
                } else {
                    textToRender = inSanctuary ? "§c§lARACHNE §e(Soon)" : "§c§lARACHNE §6(Spawned/Killed)";
                }
            }
        } else if (inSanctuary && GameState.Arachne.isDetected) {
            textColor = 0xFFFF5555;
            textToRender = "§c§lARACHNE §c(Spawned)";
        } else {
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE";
        }

        Vec3d eyePos = player.getEyePos();
        double distance = eyePos.distanceTo(Vec3d.ofCenter(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);

        GizmoDrawing.blockLabel(textToRender, renderPos, 0, textColor, textScale * 20);
    }

    private static void renderCrimsonBossWaypoints(PlayerEntity player) {
        if (!ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleWorldText) return;
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        if (!isCrimsonIsle) return;

        renderCrimsonLabel(player, ModConstants.BLADESOUL_POS,       "§8§lBLADESOUL",       0xFF555555, bladesoulStatus());
        renderCrimsonLabel(player, ModConstants.BARBARIAN_DUKE_X_POS, "§c§lBARBARIAN DUKE X", 0xFFFF5555, regularBossStatus(GameState.BarbarianDukeX.respawnEndTime, GameState.BarbarianDukeX.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGE_OUTLAW_POS,     "§5§lMAGE OUTLAW",      0xFFAA00AA, regularBossStatus(GameState.MageOutlaw.respawnEndTime,      GameState.MageOutlaw.isDetected));
        renderCrimsonLabel(player, ModConstants.ASHFANG_POS,         "§7§lASHFANG",          0xFFAAAAAA, regularBossStatus(GameState.Ashfang.respawnEndTime,          GameState.Ashfang.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGMA_BOSS_POS,      "§6§lMAGMA BOSS",       0xFFFFAA00, magmaBossStatus());
    }

    private static void renderCrimsonLabel(PlayerEntity player, BlockPos base, String nameText, int argbColor, String status) {
        BlockPos renderPos = base.add(0, 2, 0);
        Vec3d eyePos = player.getEyePos();
        double distance = eyePos.distanceTo(Vec3d.ofCenter(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);
        GizmoDrawing.blockLabel(nameText + " " + status, renderPos, 0, argbColor, textScale * 20);
    }

    private static String regularBossStatus(long respawnEnd, boolean isDetected) {
        long remaining = respawnEnd - System.currentTimeMillis();
        if (remaining > 0) {
            long secs = remaining / 1000;
            return String.format("§e(%dm %02ds)", secs / 60, secs % 60);
        }
        if (isDetected) return "§a(Spawned)";
        if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) return "§a(Ready)";
        return "§7(Unknown)";
    }

    private static String bladesoulStatus() {
        return regularBossStatus(GameState.Bladesoul.respawnEndTime, GameState.Bladesoul.isDetected);
    }

    private static String magmaBossStatus() {
        String sp = GameState.MagmaBoss.spawnStatus;
        if (sp != null) return "§a(" + sp + ")";
        long respawnEnd = GameState.MagmaBoss.respawnEndTime;
        long remaining = respawnEnd - System.currentTimeMillis();
        if (remaining > 0) {
            long secs = remaining / 1000;
            return String.format("§e(%dm %02ds)", secs / 60, secs % 60);
        }
        if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) return "§a(Ready)";
        return "§7(Unknown)";
    }

    private static void renderBossTracers(MinecraftClient client, float tickProgress) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        // 目線から視線方向へ0.5ブロック前方にオフセット
        // → 一人称時にカメラ原点と始点が重ならず画面中央から線が見える
        Vec3d eyePos = player.getLerpedPos(tickProgress)
                .add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d from = eyePos.add(player.getRotationVec(tickProgress).multiply(0.5));

        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            int color;
            if (entity instanceof IronGolemEntity) {
                if (!ModConfig.INSTANCE.theEnd.enableGolemTracer) continue;
                color = 0xFFFFAA00;
            } else if (EntityHighlightManager.arachneEntities.contains(entity)) {
                // Arachne は Broodmother と同じ SpiderEntity 型のため、instanceof 判定より先に判定する
                if (!ModConfig.INSTANCE.spidersDen.enableArachneTracer) continue;
                color = 0xFFAA00AA;
            } else if (EntityHighlightManager.arachneBroodEntities.contains(entity)) {
                // Arachne's Brood も同じ SpiderEntity(CaveSpiderEntity) 系のため、instanceof 判定より先に判定する
                if (!ModConfig.INSTANCE.spidersDen.enableArachneTracer) continue;
                color = 0xFFFF55FF;
            } else if (entity instanceof SpiderEntity) {
                if (!ModConfig.INSTANCE.spidersDen.enableBroodmotherTracer) continue;
                color = 0xFFFF5555;
            } else if (entity instanceof EnderDragonEntity) {
                if (!ModConfig.INSTANCE.theEnd.enableDragonTracer) continue;
                color = 0xFF000000 | dragonTracerColor(GameState.Dragon.type);
            } else if (entity instanceof MagmaCubeEntity
                    && EntityHighlightManager.magmaGlareEntities.contains(entity)) {
                if (!ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTracer) continue;
                color = 0xFFFF5555;
            } else {
                CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
                if (boss == null || !boss.enableTracer().get()) continue;
                // Bladesoul は Wither Skeleton のみにトレーサーを表示（本体は Glow のみ）
                if ("Bladesoul".equals(boss.nameTag()) && !(entity instanceof WitherSkeletonEntity)) continue;
                color = boss.tracerColorARGB();
            }

            // エンティティの補間済み中心位置
            Vec3d to = entity.getLerpedPos(tickProgress).add(0, entity.getHeight() / 2.0, 0);
            GizmoDrawing.line(from, to, color, 4.0f).ignoreOcclusion();
        }
    }

    private static int dragonTracerColor(String type) {
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