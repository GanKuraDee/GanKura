package com.deeply.gankura.handler;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.CurveSolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hotspot Radar を使ったときに走るパーティクルから、Hotspot の場所を推測する。
 *
 * パーティクルはネットワークスレッドで届くので、座標を控えるだけにして
 * 実際の計算は tick 側で行う。FloorDropHandler と同じ作り。
 */
public class HotspotRadarHandler {

    // 右クリックを見定めるアイテム名
    private static final String RADAR_NAME = "Hotspot Radar";
    // 最後のパーティクルから、軌跡を続きとみなす間(tick)
    private static final int TRAIL_TICKS = 40;
    // 推測した場所を覚えておく間(tick)。約2分
    private static final int KEEP_TICKS = 2400;
    // これだけ近づいたら、もう見えているので消す(ブロック)
    private static final double ARRIVE_DISTANCE = 8.0;
    // ネットワークスレッドから溜まる座標の上限。
    // 取りこぼしても次のパーティクルで拾える
    private static final int MAX_PENDING = 256;

    private static final CurveSolver solver = new CurveSolver();
    private static final ConcurrentLinkedQueue<double[]> pending = new ConcurrentLinkedQueue<>();

    private static int trailTicks;
    private static int keepTicks;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HotspotRadarHandler::tick);
    }

    /** ネットワークスレッドから呼ばれる。座標を控えるだけにとどめる */
    public static void onParticle(double x, double y, double z) {
        if (pending.size() >= MAX_PENDING) return;
        pending.add(new double[] {x, y, z});
    }

    /** 推測した Hotspot の場所。分かっていなければ null */
    public static Vec3d guess() {
        if (!ModConfig.INSTANCE.fishing.showHotspotGuess) return null;
        return solver.solved();
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            pending.clear();
            solver.clear();
            return;
        }

        boolean holding = isHoldingRadar(client);

        double[] particle;
        while ((particle = pending.poll()) != null) {
            Vec3d pos = new Vec3d(particle[0], particle[1], particle[2]);

            if (trailTicks == 0) {
                // 新しい軌跡。レーダーを持っている間に来たものだけ拾う
                if (!holding) continue;
                solver.start(client.player.getEyePos());
            } else if (!solver.isConnected(pos)) {
                // 前の軌跡から離れている。撃ち直したとみて始めからやり直す
                solver.start(client.player.getEyePos());
            }

            solver.addPoint(pos);
            trailTicks = TRAIL_TICKS;
            keepTicks = KEEP_TICKS;
        }

        if (trailTicks > 0) trailTicks--;
        if (keepTicks > 0 && --keepTicks == 0) solver.clear();

        // 目の前に来たら、推測はもう要らない。
        // 軌跡は残しておき、続きのパーティクルで求め直せるようにする
        Vec3d solved = solver.solved();
        if (solved != null && solved.distanceTo(client.player.getEntityPos()) < ARRIVE_DISTANCE) {
            solver.forgetSolved();
        }
    }

    private static boolean isHoldingRadar(MinecraftClient client) {
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return false;

        String name = held.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "");
        return name.contains(RADAR_NAME);
    }
}
