package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.handler.FishingBobberTracker;
import com.deeply.gankura.handler.HotspotAreaHandler;
import com.deeply.gankura.handler.HotspotRadarHandler;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.handler.FloorDropHandler;
import com.deeply.gankura.scanner.BeeNestScanner;
import com.deeply.gankura.util.DevHooks;
import com.deeply.gankura.waypoint.Waypoint;
import com.deeply.gankura.waypoint.WaypointData;
import com.deeply.gankura.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos; // 26.1.2 での新しい描画クラス

public class WorldTextRenderer {

    // Floor Drop の塗りつぶし色と、その上に出すラベルの色
    private static final int FLOOR_DROP_COLOR = 0x8055FF55;
    private static final int FLOOR_DROP_LABEL_COLOR = 0xFF55FF55;
    // ミツバチの巣の塗りつぶし色と、その上に出すラベルの色
    private static final int BEE_NEST_COLOR = 0x80FFFF55;
    private static final int BEE_NEST_LABEL_COLOR = 0xFFFFFF55;
    // 投げてからの経過秒を浮きの上に出すときの、高さ(ブロック)と色。
    // Hypixel が浮きのすぐ上に出しているカウントダウンと重ならないよう、その上に出す
    private static final double CAST_TIMER_LABEL_HEIGHT = 1.2;
    private static final int CAST_TIMER_LABEL_COLOR = 0xFF55FF55;
    // 自分で置いたウェイポイントの線の太さと、名前の色。表示を打ち切る距離(ブロック)
    private static final float WAYPOINT_LINE_WIDTH = 2.0F;
    private static final int WAYPOINT_LABEL_COLOR = 0xFFFFFFFF;
    private static final double WAYPOINT_MAX_DISTANCE = 384.0;

    // Hotspot の範囲を示す円の太さ・分割数と、描くのをやめる距離(ブロック)。
    // 色は効果の種類ごとに変わるので HotspotAreaHandler が持つ
    private static final float HOTSPOT_CIRCLE_WIDTH = 2.0f;
    private static final int HOTSPOT_CIRCLE_SEGMENTS = 64;
    private static final double HOTSPOT_CIRCLE_MAX_DISTANCE = 128.0;

    // Hotspot Radar の推測地点に使う色と太さ
    public static final int HOTSPOT_COLOR = 0xFFFF55FF;
    private static final int HOTSPOT_FILL_COLOR = 0x40FF55FF;
    private static final float HOTSPOT_LINE_WIDTH = 3.0f;
    // 見つけた Hotspot の枠の塗り。色は効果ごとに変わるので、濃さだけ決めておく
    private static final int HOTSPOT_FOUND_FILL_ALPHA = 0x40000000;

    public static void render(Minecraft client) {
        if (client.player == null) return;
        renderGolemLocationText();
        renderCrimsonBossLocationTexts();
        renderArachneLocationText();
        renderWumpaWaypoint();
        renderTikiWaypoints(client);
        renderFloorDrops();
        renderBeeNests();
        renderCustomWaypoints(client);
        renderCastTimer(client);
        renderHotspotGuess(client);
        renderHotspotFound(client);
        renderHotspotCircles(client);
        // 開発中の一時的な機能。配布ビルドでは何も登録されていない
        DevHooks.renderWorld(client);
    }

    /**
     * Hotspot の範囲を円で示す。
     *
     * 円の描画は無いので、短い線をつないで多角形にする。
     * 分割数を十分に取れば、見た目は円と区別が付かない
     */
    private static void renderHotspotCircles(Minecraft client) {
        for (HotspotAreaHandler.Circle circle : HotspotAreaHandler.circles()) {
            if (client.player.position().distanceTo(circle.center()) > HOTSPOT_CIRCLE_MAX_DISTANCE) continue;

            renderCircle(circle.center(), circle.radius(), circle.argb());
        }
    }

    private static void renderCircle(Vec3 center, double radius, int argb) {
        Vec3 previous = null;
        for (int i = 0; i <= HOTSPOT_CIRCLE_SEGMENTS; i++) {
            double angle = Math.TAU * i / HOTSPOT_CIRCLE_SEGMENTS;
            Vec3 point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

            if (previous != null) drawSegment(previous, point, argb);
            previous = point;
        }
    }

    private static void drawSegment(Vec3 from, Vec3 to, int argb) {
            GizmoProperties segment = Gizmos.addGizmo(new LineGizmo(from, to, argb,
                    HOTSPOT_CIRCLE_WIDTH));
            segment.setAlwaysOnTop();
    }

    // Hotspot Radar から推測した場所に印を出す
    private static void renderHotspotGuess(Minecraft client) {
        Vec3 guess = HotspotRadarHandler.guess();
        if (guess == null) return;

        int distance = (int) Math.round(client.player.position().distanceTo(guess));
        // この描画は1行分なので、改行せずに並べる
        renderGizmoLabelAt("§d§lHOTSPOT §e" + distance + "m", guess.add(0, 1.5, 0), HOTSPOT_COLOR);

        // 推測なので、居場所が分かるようブロック1つ分の枠も出す
        GizmoProperties box = Gizmos.cuboid(BlockPos.containing(guess),
                GizmoStyle.strokeAndFill(HOTSPOT_COLOR, HOTSPOT_LINE_WIDTH, HOTSPOT_FILL_COLOR));
        box.setAlwaysOnTop();
    }

    // 狙っている効果の Hotspot を見つけたとき、しばらくの間その場所に印を出す
    private static void renderHotspotFound(Minecraft client) {
        if (!ModConfig.INSTANCE.fishing.showHotspotFoundWaypoint) return;

        for (HotspotAreaHandler.Found spot : HotspotAreaHandler.found()) {
            int distance = (int) Math.round(client.player.position().distanceTo(spot.center()));
            // 円や Tracer と同じく、効果の色で塗り分ける
            int argb = spot.perk().argb();
            renderGizmoLabelAt("§d§lHOTSPOT " + spot.perk() + " §e" + distance + "m",
                    spot.center().add(0, 1.5, 0), argb);

            GizmoProperties box = Gizmos.cuboid(BlockPos.containing(spot.center()),
                    GizmoStyle.strokeAndFill(argb, HOTSPOT_LINE_WIDTH,
                            (argb & 0xFFFFFF) | HOTSPOT_FOUND_FILL_ALPHA));
            box.setAlwaysOnTop();
        }
    }

    // 投げている浮きの上に、投げてからの経過秒を出す
    private static void renderCastTimer(Minecraft client) {
        if (!ModConfig.INSTANCE.fishing.showCastTimer) return;

        FishingHook hook = FishingBobberTracker.bobber(client);
        if (hook == null) return;

        double seconds = FishingBobberTracker.elapsedSeconds();
        if (seconds < 0) return;

        Vec3 pos = hook.position().add(0, CAST_TIMER_LABEL_HEIGHT, 0);
        renderGizmoLabelAt(String.format("%.1fs", seconds), pos, CAST_TIMER_LABEL_COLOR);
    }

    // 自分で置いたウェイポイント。今いるエリアに登録されているものだけを出す
    private static void renderCustomWaypoints(Minecraft client) {
        if (!GameState.Server.isSkyblock()) return;

        WaypointManager manager = WaypointManager.getInstance();
        WaypointData data = manager.data();
        if (!data.enabled) return;

        String area = WaypointManager.currentArea();
        java.util.List<Waypoint> waypoints = manager.waypoints(area);
        if (waypoints.isEmpty()) return;

        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();

        for (Waypoint waypoint : waypoints) {
            if (!waypoint.isEnabled()) continue;
            if (!manager.isGroupEnabled(area, waypoint.getGroup())) continue;

            BlockPos pos = waypoint.pos();
            // 遠すぎるものは点にしかならないので描かない
            if (cameraPos.distanceToSqr(Vec3.atCenterOf(pos))
                    > WAYPOINT_MAX_DISTANCE * WAYPOINT_MAX_DISTANCE) continue;

            GizmoProperties box = Gizmos.cuboid(pos, waypointStyle(waypoint));
            box.setAlwaysOnTop();

            if (data.showNames && !waypoint.getName().isBlank()) {
                renderGizmoLabel(waypoint.getName(), pos, WAYPOINT_LABEL_COLOR);
            }
        }
    }

    // 枠線だけ・塗りつぶしだけ・両方の3種類を Gizmo の指定へ移す
    private static GizmoStyle waypointStyle(Waypoint waypoint) {
        int outline = 0xFF000000 | waypoint.getColor();
        int fill = (waypoint.getFillAlpha() << 24) | waypoint.getColor();

        if (!waypoint.getStyle().hasFill()) return GizmoStyle.stroke(outline, WAYPOINT_LINE_WIDTH);
        if (!waypoint.getStyle().hasOutline()) return GizmoStyle.fill(fill);
        return GizmoStyle.strokeAndFill(outline, WAYPOINT_LINE_WIDTH, fill);
    }

    // 地面に落ちている採取物。見つけた場所を塗りつぶし、Re-enter や Tiki と同じくラベルを添える
    private static void renderFloorDrops() {
        if (!FloorDropHandler.isActive()) return;

        for (BlockPos pos : FloorDropHandler.positions()) {
            GizmoProperties box = Gizmos.cuboid(pos, GizmoStyle.fill(FLOOR_DROP_COLOR));
            box.setAlwaysOnTop();
            renderGizmoLabel("§aFloor Drop", pos, FLOOR_DROP_LABEL_COLOR);
        }
    }

    // Forest Biome のミツバチの巣。Floor Drop と同じく塗りつぶしとラベルで示す
    private static void renderBeeNests() {
        if (!BeeNestScanner.isActive()) return;

        for (BlockPos pos : BeeNestScanner.positions()) {
            GizmoProperties box = Gizmos.cuboid(pos, GizmoStyle.fill(BEE_NEST_COLOR));
            box.setAlwaysOnTop();
            renderGizmoLabel("§eBee Nest", pos, BEE_NEST_LABEL_COLOR);
        }
    }


    // Wumpaに敗れた後、戦闘エリアへ戻る小さな穴の目印。
    // Wumpaが湧いている間しか使い道がないので、その間だけ表示する
    private static void renderWumpaWaypoint() {
        if (!ModConfig.INSTANCE.foraging.enableWumpaWaypoint) return;
        if (!GameState.Server.isSafari()) return;
        if (!GameState.CritterSafari.isWumpaSpawned()) return;

        BlockPos pos = ModConstants.WUMPA_REENTER_POS;
        // 枠線ではなくブロック全体を塗りつぶす
        GizmoProperties box = Gizmos.cuboid(pos, GizmoStyle.fill(0x8055FFFF));
        box.setAlwaysOnTop();
        renderGizmoLabel("§fRe-enter", pos, 0xFFFFFFFF);
    }

    // Tiki 系のスポーン地点。Tiki はここにあるオブジェを完成させないと出現しないため、
    // 湧いているかどうかに関わらず常に目印を出す
    private static void renderTikiWaypoints(Minecraft client) {
        if (!ModConfig.INSTANCE.foraging.enableTikiWaypoints) return;
        // Tiki 系は Torrhus Canyon と Torrhus Heights のどちらにも湧く
        if (!GameState.Server.isTorrhusCanyon() && !GameState.Server.isTorrhusHeights()) return;
        if (client.player == null) return;

        for (BlockPos pos : ModConstants.TIKI_SPAWN_POSITIONS) {
            // 枠線だと遠くで見えづらいので、Re-enter の目印と同じく塗りつぶしにする
            GizmoProperties box = Gizmos.cuboid(pos, GizmoStyle.fill(0x80FFAA00));
            box.setAlwaysOnTop();
            renderGizmoLabel("§6Tiki", pos, 0xFFFFAA00);
        }
    }

    // Stage 4/5 のときに、ワールド上テキストを出しているブロックと色。それ以外は null。
    // Tracer からも同じ場所・同じ色を使えるよう切り出してある
    public record GolemAnchor(BlockPos pos, int argb) {}

    public static GolemAnchor golemAnchor() {
        if (!GameState.Server.isTheEnd()) return null;
        if (GameState.Player.locationPos == null || "None".equals(GameState.Player.locationName)) return null;

        BlockPos basePos = GameState.Player.locationPos;
        String stage = GameState.Golem.stage;
        if (ModConstants.STAGE_AWAKENING.equals(stage)) {
            return new GolemAnchor(basePos.offset(0, 1, -2), 0xFFFFFFFF);
        }
        if (ModConstants.STAGE_SUMMONED.equals(stage)) {
            return new GolemAnchor(basePos.offset(0, 0, -2), 0xFFFF5555);
        }
        return null;
    }

    // ワールド上テキストを描く位置。ブロックの中心から少し上
    public static Vec3 labelPos(BlockPos renderPos) {
        return new Vec3(renderPos.getX() + 0.5, renderPos.getY() + 1.5, renderPos.getZ() + 0.5);
    }

    private static void renderGolemLocationText() {
        if (!ModConfig.INSTANCE.combat.theEnd.showGolemWorldLocation_Text) return;

        GolemAnchor anchor = golemAnchor();
        if (anchor == null) return;

        String textToRender;
        if (ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage)) {
            textToRender = "§f§lGOLEM";
            if (GameState.Golem.stage4StartTime > 0) {
                long secs = (System.currentTimeMillis() - GameState.Golem.stage4StartTime) / 1000;
                String col = secs >= 480 ? "§c" : (secs >= 240 ? "§e" : "§f");
                textToRender += String.format(" %s(%dm %ds)", col, secs / 60, secs % 60);
            }
        } else {
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

        renderGizmoLabel(textToRender, anchor.pos(), anchor.argb());
    }

    private static void renderArachneLocationText() {
        if (!ModConfig.INSTANCE.combat.spidersDen.showArachneWorldText) return;
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

    private static void renderCrimsonBossLocationTexts() {
        if (!ModConfig.INSTANCE.combat.crimsonIsle.showCrimsonIsleWorldText) return;
        boolean isCrimsonIsle = GameState.Server.isCrimsonIsle();
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

    public static void renderGizmoLabel(String text, BlockPos renderPos, int argbColor) {
        renderGizmoLabelAt(text, labelPos(renderPos), argbColor);
    }

    // 動くものに付けるラベル。位置は呼び出し側が決める
    public static void renderGizmoLabelAt(String text, Vec3 pos, int argbColor) {
        // 距離に比例して拡大し、見かけの大きさを一定に保つ。
        // プレイヤーのtick座標を使うと20回/秒でしかスケールが更新されずカクつくため、
        // フレームごとに補間されるカメラ座標を基準にする
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
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
