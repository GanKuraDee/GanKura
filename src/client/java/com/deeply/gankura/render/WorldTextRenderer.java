package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos; // 26.1.2 での新しい描画クラス

public class WorldTextRenderer {

    public static void render(Minecraft client) {
        if (client.player == null) return;
        renderGolemLocationText();
        renderCrimsonBossLocationTexts();
        renderArachneLocationText();
    }

    private static void renderGolemLocationText() {
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
            if (GameState.Golem.stage4StartTime > 0) {
                long secs = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                String col = secs >= 480 ? "§c" : (secs >= 240 ? "§e" : "§f");
                textToRender += String.format(" %s(%dm %ds)", col, secs / 60, secs % 60);
            }
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

        renderGizmoLabel(textToRender, renderPos, textColor);
    }

    private static void renderArachneLocationText() {
        if (!ModConfig.INSTANCE.spidersDen.showArachneWorldText) return;
        if (!ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map)) return;

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

    private static void renderCrimsonBossLocationTexts() {
        if (!ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleWorldText) return;
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        if (!isCrimsonIsle) return;

        renderCrimsonLabel(ModConstants.BLADESOUL_POS,        "§8§lBLADESOUL",        0xFF555555, bladesoulStatus());
        renderCrimsonLabel(ModConstants.BARBARIAN_DUKE_X_POS, "§c§lBARBARIAN DUKE X", 0xFFFF5555, regularBossStatus("Barbarian Duke X", GameState.BarbarianDukeX.respawnEndTime, GameState.BarbarianDukeX.isDetected));
        renderCrimsonLabel(ModConstants.MAGE_OUTLAW_POS,      "§5§lMAGE OUTLAW",       0xFFAA00AA, regularBossStatus("Mage Outlaw",      GameState.MageOutlaw.respawnEndTime,      GameState.MageOutlaw.isDetected));
        renderCrimsonLabel(ModConstants.ASHFANG_POS,          "§7§lASHFANG",           0xFFAAAAAA, regularBossStatus("Ashfang",          GameState.Ashfang.respawnEndTime,          GameState.Ashfang.isDetected));
        renderCrimsonLabel(ModConstants.MAGMA_BOSS_POS,       "§6§lMAGMA BOSS",        0xFFFFAA00, magmaBossStatus());
    }

    private static void renderCrimsonLabel(BlockPos base, String nameText, int argbColor, String status) {
        BlockPos renderPos = base.offset(0, 2, 0);
        renderGizmoLabel(nameText + " " + status, renderPos, argbColor);
    }

    private static void renderGizmoLabel(String text, BlockPos renderPos, int argbColor) {
        Vec3 pos = new Vec3(renderPos.getX() + 0.5, renderPos.getY() + 1.5, renderPos.getZ() + 0.5);

        // 距離に比例して拡大し、見かけの大きさを一定に保つ。
        // プレイヤーのtick座標を使うと20回/秒でしかスケールが更新されずカクつくため、
        // フレームごとに補間されるカメラ座標を基準にする
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
        float textScale = (float) Math.max(0.02, cameraPos.distanceTo(pos) * 0.0025);

        TextGizmo.Style style = TextGizmo.Style.forColorAndCentered(argbColor)
                .withScale(textScale * 20.0F);
        GizmoProperties properties = Gizmos.billboardText(text, pos, style);
        properties.setAlwaysOnTop();
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
        // サイドバーが読めている間はフェーズをそのまま出す(エリア内でしか読めない)。
        // フェーズ行に加え、念のため「Magma Chamber」の行も確認する
        String sp = GameState.MagmaBoss.inArena ? GameState.MagmaBoss.spawnStatus : null;
        if (sp != null) return "§a(" + sp + ")";
        return regularBossStatus("Magma Boss", GameState.MagmaBoss.respawnEndTime, GameState.MagmaBoss.isDetected);
    }
}
