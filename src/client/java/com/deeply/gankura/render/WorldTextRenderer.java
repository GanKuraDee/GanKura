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
        renderCrimsonBossWaypoints(client.player);
        renderArachneWaypoint(client, client.player);
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

        Vec3 eyePos = player.getEyePosition();
        double distance = eyePos.distanceTo(Vec3.atCenterOf(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);

        renderGizmoLabel(textToRender, renderPos, textColor, textScale);
    }

    private static void renderArachneWaypoint(Minecraft client, Player player) {
        if (!ModConfig.INSTANCE.spidersDen.showArachneWorldText) return;
        if (!ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map)) return;

        BlockPos renderPos = ModConstants.ARACHNE_ALTAR_POS;
        boolean inSanctuary = GameState.Arachne.inSanctuary;
        int textColor;
        String textToRender;

        if (GameState.Arachne.cobwebDetected) {
            // 基準座標に蜘蛛の巣ブロックが存在する = Spawned確定
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
                } else {
                    textToRender = inSanctuary ? "§c§lARACHNE §e(Soon)" : "§c§lARACHNE §6(Ready/Spawning)";
                }
            }
        } else if (GameState.Arachne.arachneMessageSeen) {
            // カウントダウン情報がない状態で「[BOSS] Arachne」を検知した場合の「間もなく」表示
            textColor = 0xFFFF5555;
            textToRender = "§c§lARACHNE §e(Soon)";
        } else if (GameState.Arachne.downConfirmed) {
            // ARACHNE DOWN!確定済み。次のCalling/Crystalまではチャンク未読み込みでもReadyを信頼する
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE";
        } else if (!GameState.Arachne.webAreaLoaded) {
            // 基準座標が遠すぎてチャンクが読み込まれておらず、蜘蛛の巣の有無を判定できない
            textColor = 0xFFAAAAAA;
            textToRender = "§7§lARACHNE §7(Unknown) §7(Go to Arachne's Sanctuary!)";
        } else {
            // チャンクは読み込めており、蜘蛛の巣が存在しないと確認できた = Ready
            textColor = 0xFFAA00AA;
            textToRender = "§5§lARACHNE";
        }

        Vec3 eyePos = player.getEyePosition();
        double distance = eyePos.distanceTo(Vec3.atCenterOf(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);

        renderGizmoLabel(textToRender, renderPos, textColor, textScale);
    }

    private static void renderCrimsonBossWaypoints(Player player) {
        if (!ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleWorldText) return;
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        if (!isCrimsonIsle) return;

        renderCrimsonLabel(player, ModConstants.BLADESOUL_POS,        "§8§lBLADESOUL",        0xFF555555, bladesoulStatus());
        renderCrimsonLabel(player, ModConstants.BARBARIAN_DUKE_X_POS, "§c§lBARBARIAN DUKE X", 0xFFFF5555, regularBossStatus(GameState.BarbarianDukeX.respawnEndTime, GameState.BarbarianDukeX.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGE_OUTLAW_POS,      "§5§lMAGE OUTLAW",       0xFFAA00AA, regularBossStatus(GameState.MageOutlaw.respawnEndTime,      GameState.MageOutlaw.isDetected));
        renderCrimsonLabel(player, ModConstants.ASHFANG_POS,          "§7§lASHFANG",           0xFFAAAAAA, regularBossStatus(GameState.Ashfang.respawnEndTime,          GameState.Ashfang.isDetected));
        renderCrimsonLabel(player, ModConstants.MAGMA_BOSS_POS,       "§6§lMAGMA BOSS",        0xFFFFAA00, magmaBossStatus());
    }

    private static void renderCrimsonLabel(Player player, BlockPos base, String nameText, int argbColor, String status) {
        BlockPos renderPos = base.offset(0, 2, 0);
        Vec3 eyePos = player.getEyePosition();
        double distance = eyePos.distanceTo(Vec3.atCenterOf(renderPos));
        float textScale = (float) Math.max(0.02, distance * 0.0025);
        renderGizmoLabel(nameText + " " + status, renderPos, argbColor, textScale);
    }

    private static void renderGizmoLabel(String text, BlockPos renderPos, int argbColor, float textScale) {
        TextGizmo.Style style = TextGizmo.Style.forColorAndCentered(argbColor)
                .withScale(textScale * 20.0F);
        Vec3 pos = new Vec3(renderPos.getX() + 0.5, renderPos.getY() + 1.5, renderPos.getZ() + 0.5);
        GizmoProperties properties = Gizmos.billboardText(text, pos, style);
        properties.setAlwaysOnTop();
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
}
