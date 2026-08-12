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
import net.minecraft.client.render.DrawStyle;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.Map;

public class WorldTextRenderer {

    public static void render(MinecraftClient client, float tickProgress) {
        if (client.player == null) return;

        renderGolemLocationText(client, client.player);
        renderCrimsonBossLocationTexts(client.player);
        renderArachneLocationText(client, client.player);
        renderWumpaWaypoint();
        renderBossTracers(client, tickProgress);
    }


    // Wumpaに敗れた後、戦闘エリアへ戻る小さな穴の目印。
    // Wumpaが湧いている間しか使い道がないので、その間だけ表示する
    private static void renderWumpaWaypoint() {
        if (!ModConfig.INSTANCE.foraging.enableWumpaWaypoint) return;
        if (!GameState.Server.isSafari()) return;
        if (!GameState.CritterSafari.isWumpaSpawned()) return;

        BlockPos pos = ModConstants.WUMPA_REENTER_POS;
        // 枠線ではなくブロック全体を塗りつぶす
        GizmoDrawing.box(pos, DrawStyle.filled(0x8055FFFF)).ignoreOcclusion();
        renderGizmoLabel("§fRe-enter", pos, 0xFFFFFFFF);
    }

    private static void renderGolemLocationText(MinecraftClient client, PlayerEntity player) {
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

        renderGizmoLabel(textToRender, renderPos, textColor);
    }

    private static void renderArachneLocationText(MinecraftClient client, PlayerEntity player) {
        if (!ModConfig.INSTANCE.spidersDen.showArachneWorldText) return;
        if (!GameState.Server.isSpidersDen()) return;

        BlockPos renderPos = ModConstants.ARACHNE_ALTAR_POS;
        boolean inSanctuary = GameState.Arachne.inSanctuary;
        int textColor;
        String textToRender;

        if (inSanctuary && GameState.Arachne.cobwebDetected) {
            // 基準座標に蜘蛛の巣ブロックが存在する = Spawned確定(Sanctuary外はスキャンしないため判定に使わない)
            textColor = 0xFFFF5555;
            textToRender = "§c§lARACHNE §c(Spawned)";
        } else if (GameState.Arachne.isSummoning) {
            textColor = 0xFFFF5555;
            if (GameState.Arachne.awaitingCrystalParticles) {
                textToRender = "§c§lARACHNE §c(...)";
            } else {
                long timeSincePacket = Math.min(System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis, 1000);
                double remainingTicks = Math.max(0, GameState.Arachne.spawnTargetTime - (GameState.Server.lastTimePacket + (timeSincePacket / 50.0)));
                if (remainingTicks > 0) {
                    textToRender = String.format("§c§lARACHNE §c(%.1fs)", remainingTicks / 20.0);
                } else if (inSanctuary) {
                    textToRender = "§c§lARACHNE §e(Soon)";
                } else {
                    // カウントダウン終了時点でSanctuary外にいた場合はSpawned/Killedとする
                    textToRender = "§c§lARACHNE §6(Spawned/Killed)";
                }
            }
        } else if (inSanctuary && GameState.Arachne.arachneMessageSeen) {
            // カウントダウン情報がない状態で「[BOSS] Arachne」を検知した場合の「間もなく」表示
            textColor = 0xFFFF5555;
            textToRender = "§c§lARACHNE §e(Soon)";
        } else if (inSanctuary && GameState.Arachne.downConfirmed) {
            // ARACHNE DOWN!確定済み
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE §a(Ready)";
        } else if (inSanctuary && !GameState.Arachne.webAreaLoaded) {
            // Sanctuary内だが基準座標のチャンクが読み込まれておらず判定できない(稀なエッジケース)
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE §7(Unknown)";
        } else if (inSanctuary) {
            // チャンクは読み込めており、蜘蛛の巣が存在しないと確認できた = Ready
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE §a(Ready)";
        } else if (!GameState.Arachne.everConfirmed) {
            // Sanctuaryに一度もアクセスしておらず状態を確定できたことがない
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE §7(Unknown)";
        } else if (GameState.Arachne.lastConfirmedWasReady) {
            // 直近Sanctuary内で確定した状態がReadyだった場合はエリア外でもReadyを維持する
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE §a(Ready)";
        } else {
            // 直近確定した状態がSpawning/Spawnedだった場合はエリア外ではSpawned/Killedとする
            textColor = 0xFFFF5555;
            textToRender = "§c§lARACHNE §6(Spawned/Killed)";
        }

        renderGizmoLabel(textToRender, renderPos, textColor);
    }

    private static void renderCrimsonBossLocationTexts(PlayerEntity player) {
        if (!ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleWorldText) return;
        boolean isCrimsonIsle = GameState.Server.isCrimsonIsle();
        if (!isCrimsonIsle) return;

        renderCrimsonLabel(player, ModConstants.BLADESOUL_POS,       "§8§lBLADESOUL",       0xFF555555, bladesoulStatus());
        renderCrimsonLabel(player, ModConstants.BARBARIAN_DUKE_X_POS, "§c§lBARBARIAN DUKE X", 0xFFFF5555, regularBossStatus("Barbarian Duke X", GameState.BarbarianDukeX.respawnEndTime, GameState.BarbarianDukeX.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGE_OUTLAW_POS,     "§5§lMAGE OUTLAW",      0xFFAA00AA, regularBossStatus("Mage Outlaw",      GameState.MageOutlaw.respawnEndTime,      GameState.MageOutlaw.isDetected));
        renderCrimsonLabel(player, ModConstants.ASHFANG_POS,         "§7§lASHFANG",          0xFFAAAAAA, regularBossStatus("Ashfang",          GameState.Ashfang.respawnEndTime,          GameState.Ashfang.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGMA_BOSS_POS,      "§6§lMAGMA BOSS",       0xFFFFAA00, magmaBossStatus());
    }

    private static void renderCrimsonLabel(PlayerEntity player, BlockPos base, String nameText, int argbColor, String status) {
        BlockPos renderPos = base.add(0, 2, 0);
        renderGizmoLabel(nameText + " " + status, renderPos, argbColor);
    }

    private static void renderGizmoLabel(String text, BlockPos renderPos, int argbColor) {
        // 距離に比例して拡大し、見かけの大きさを一定に保つ。
        // プレイヤーのtick座標を使うと20回/秒でしかスケールが更新されずカクつくため、
        // フレームごとに補間されるカメラ座標を基準にする
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
        float textScale = (float) Math.max(0.02, cameraPos.distanceTo(Vec3d.ofCenter(renderPos)) * 0.0025);
        GizmoDrawing.blockLabel(text, renderPos, 0, argbColor, textScale * 20);
    }

    private static String regularBossStatus(String bossName, long respawnEnd, boolean isDetected) {
        long remaining = respawnEnd - System.currentTimeMillis();
        if (remaining > 0) {
            long secs = remaining / 1000;
            return String.format("§e(%dm %02ds)", secs / 60, secs % 60);
        }
        if (isDetected) return "§a(Spawned)";
        if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) return "§a(Ready)";
        // リスポーン推定タイマーが動いている間、または範囲内で未検出が続いている間は「いない」
        if (EntityHighlightManager.killedRemainingMs(bossName) > 0
                || EntityHighlightManager.canConfirmAbsence(bossName)) {
            return "§c(Killed)";
        }
        // 範囲外。居たことがある / リスポーン時間を過ぎた のいずれも生死は判別できない
        return EntityHighlightManager.wasSpawnedWhenLastConfirmed(bossName)
                || EntityHighlightManager.wasKilledConfirmed(bossName)
                ? "§6(Spawned/Killed)"
                : "§7(Unknown)";
    }

    private static String bladesoulStatus() {
        return regularBossStatus("Bladesoul", GameState.Bladesoul.respawnEndTime, GameState.Bladesoul.isDetected);
    }

    private static String magmaBossStatus() {
        // サイドバーが読めている間はフェーズをそのまま出す(エリア内でしか読めない)
        // フェーズ行に加え、念のため「Magma Chamber」の行も確認する
        String sp = GameState.MagmaBoss.inArena ? GameState.MagmaBoss.spawnStatus : null;
        if (sp != null) return "§a(" + sp + ")";
        return regularBossStatus("Magma Boss", GameState.MagmaBoss.respawnEndTime, GameState.MagmaBoss.isDetected);
    }

    // 対象と色は tick 側(EntityHighlightManager)で決まっているので、ここでは線を引くだけ。
    // Highlight とは独立した集合を見るため、Highlight を切っていても Tracer 単独で使える
    private static void renderBossTracers(MinecraftClient client, float tickProgress) {
        if (EntityHighlightManager.tracerEntities.isEmpty()) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        // 目線から視線方向へ0.5ブロック前方にオフセット
        // → 一人称時にカメラ原点と始点が重ならず画面中央から線が見える
        Vec3d eyePos = player.getLerpedPos(tickProgress)
                .add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d from = eyePos.add(player.getRotationVec(tickProgress).multiply(0.5));

        for (Map.Entry<Entity, Integer> entry : EntityHighlightManager.tracerEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (entity.isRemoved()) continue;

            // エンティティの補間済み中心位置
            Vec3d to = entity.getLerpedPos(tickProgress).add(0, entity.getHeight() / 2.0, 0);
            GizmoDrawing.line(from, to, entry.getValue(), 4.0f).ignoreOcclusion();
        }
    }

}