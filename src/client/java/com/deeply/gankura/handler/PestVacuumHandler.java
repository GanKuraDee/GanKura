package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.CurveSolver;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Vacuum を左クリックしたときに走る粒から、次の害虫の居場所を割り出す。
 *
 * Hypixel は害虫の方へ向かって粒を一列に飛ばすが、途中で出し終わってしまうので、
 * 見えている途中までの並びに曲線を当てはめ、その先を伸ばして終点を求める。
 * 仕組みは Hotspot Radar と同じなので、当てはめは {@link CurveSolver} を使い回す。
 *
 * 粒はネットワークの側で届くので、そこでは座標を控えるだけにして、
 * 当てはめは tick 側で行う。
 *
 * 害虫が畑のどこにいるか分からないときは、Hypixel は区画の真ん中を指してくる。
 * それは「この区画のどこか」という意味でしかないので、色を分けて出す
 */
public final class PestVacuumHandler {

    // Vacuum は Skymart のものも Infini のものも、id にこの語が入る
    private static final String VACUUM_ID_PART = "VACUUM";

    // 左クリックしてから粒を受け付ける間(ミリ秒)。実際には1秒ほどで出揃う
    private static final long PARTICLE_WINDOW_MILLIS = 5000;
    // 使ってすぐは、まだ近さで消さない(ミリ秒)。
    // 足元付近が答えのときに、出た瞬間に消えてしまわないようにする
    private static final long MIN_SHOW_MILLIS = 1000;
    // これだけ近づいたら、もう目で探せるので消す(ブロック)。
    // 害虫は宙に浮いていることがあるので、高さは見ない
    private static final double ARRIVE_DISTANCE = 8.0;

    // ネットワークの側から溜まる座標の上限。取りこぼしても次の粒で拾える
    private static final int MAX_PENDING = 256;

    // 同じ場所の粒とみなす隔たり(ブロック)。
    // Hypixel は同じ座標の粒を、間を空けて何度も送ってくる。
    // 曲線には点の並び順をそのまま使うので、重なりを入れると傾きが狂い、
    // 答えが遥か彼方へ飛ぶ
    private static final double SAME_POINT = 0.01;

    // 畑は 96 ブロックの区画が 5×5 に並んでいて、真ん中の区画が原点にあたる。
    // 区画の中心は 96 の倍数の座標になる
    private static final int PLOT_SIZE = 96;
    private static final int PLOT_LIMIT = PLOT_SIZE * 2;
    // 区画の中心ちょうどかどうかの許容(ブロック)
    private static final double PLOT_MIDDLE_EPSILON = 1.0;

    // 畑の外に出た答えは、粒を読み違えたということなので捨てる(ブロック)
    private static final double GARDEN_EDGE = PLOT_LIMIT + PLOT_SIZE;
    private static final double GARDEN_MIN_Y = -64.0;
    private static final double GARDEN_MAX_Y = 256.0;

    // 高さの補正は要らない。Hotspot の輪と違い、粒は害虫そのものを指している
    private static final CurveSolver solver = new CurveSolver(0.0);
    private static final ConcurrentLinkedQueue<double[]> pending = new ConcurrentLinkedQueue<>();
    // 当てはめに使った点。重なった粒を弾くために控えておく
    private static final List<Vec3> accepted = new ArrayList<>();

    private static long usedMillis = 0;
    private static Vec3 guess = null;
    private static boolean plotMiddle = false;

    private PestVacuumHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PestVacuumHandler::tick);
    }

    public static void reset() {
        pending.clear();
        accepted.clear();
        solver.clear();
        usedMillis = 0;
        guess = null;
        plotMiddle = false;
    }

    /** 割り出した害虫の居場所。まだ出ていなければ null */
    public static Vec3 guess() {
        return isEnabled() ? guess : null;
    }

    /** その答えが区画の真ん中か。真ん中なら「この区画のどこか」という意味しかない */
    public static boolean isPlotMiddle() {
        return plotMiddle;
    }

    /** 今、粒を読んでいる最中か。ネットワークの側の足切りに使う */
    public static boolean isReading() {
        return isEnabled() && collecting();
    }

    /** 読んでいる間の粒を隠すか */
    public static boolean hidesParticles() {
        return ModConfig.INSTANCE.farming.garden.hidePestVacuumParticles;
    }

    /**
     * 左クリックした瞬間に呼ばれる。Vacuum を持っていれば、そこから読み始める。
     *
     * しゃがんでいる間は吸い込みではなく別の操作なので、何もしない
     */
    public static void onAttack(Minecraft client) {
        if (!isEnabled() || client.player == null) return;
        if (client.player.isShiftKeyDown()) return;
        if (!isHoldingVacuum(client)) return;

        reset();
        usedMillis = System.currentTimeMillis();
        solver.start(client.player.getEyePosition());
    }

    /** ネットワークの側から呼ばれる。座標を控えるだけにとどめる */
    public static void onParticle(double x, double y, double z) {
        if (pending.size() >= MAX_PENDING) return;

        pending.add(new double[] {x, y, z});
    }

    private static void tick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) {
            if (usedMillis != 0 || guess != null) reset();
            return;
        }

        double[] particle;
        while ((particle = pending.poll()) != null) {
            if (!collecting()) continue;

            Vec3 pos = new Vec3(particle[0], particle[1], particle[2]);
            // 並びから離れた粒は、他所のものとみなして捨てる。
            // 読み直すと手元の粒を採り損ねるので、始めからやり直しはしない
            if (!solver.isConnected(pos) || isRepeat(pos)) continue;

            accepted.add(pos);
            solver.addPoint(pos);
            update(client);
        }

        if (usedMillis == 0) return;

        long shown = System.currentTimeMillis() - usedMillis;
        if (shown > showMillis()) {
            reset();
            return;
        }

        // 目の前まで来たら、もう目印は要らない
        if (guess != null && shown > MIN_SHOW_MILLIS && horizontalDistance(client, guess) < ARRIVE_DISTANCE) {
            reset();
        }
    }

    // 当てはめ直した答えを控える。畑から外れた答えは、読み違えとみて採らない
    private static void update(Minecraft client) {
        Vec3 solved = solver.solved();
        if (solved == null || !inGarden(solved)) return;

        guess = solved;
        plotMiddle = isPlotMiddle(solved);
    }

    // 粒の受付中か
    private static boolean collecting() {
        return usedMillis != 0 && System.currentTimeMillis() - usedMillis <= PARTICLE_WINDOW_MILLIS;
    }

    /**
     * すでに採った点と同じ場所か。
     *
     * 直前とだけ比べたのでは足りない。Hypixel は同じ座標を間を空けて送ってくるうえ、
     * 粒の処理はネットワークと本体の両方の筋を通るので、同じものが二度届く
     */
    private static boolean isRepeat(Vec3 pos) {
        for (Vec3 point : accepted) {
            if (point.distanceToSqr(pos) < SAME_POINT * SAME_POINT) return true;
        }
        return false;
    }

    private static boolean inGarden(Vec3 pos) {
        return Math.abs(pos.x) <= GARDEN_EDGE && Math.abs(pos.z) <= GARDEN_EDGE
                && pos.y >= GARDEN_MIN_Y && pos.y <= GARDEN_MAX_Y;
    }

    // 区画の中心を指しているか。中心は 96 の倍数の座標にある
    private static boolean isPlotMiddle(Vec3 pos) {
        return onPlotMiddleAxis(pos.x) && onPlotMiddleAxis(pos.z);
    }

    private static boolean onPlotMiddleAxis(double value) {
        if (Math.abs(value) > PLOT_LIMIT + PLOT_MIDDLE_EPSILON) return false;

        double offset = Math.abs(value - Math.round(value / PLOT_SIZE) * (double) PLOT_SIZE);
        return offset <= PLOT_MIDDLE_EPSILON;
    }

    private static double horizontalDistance(Minecraft client, Vec3 pos) {
        Vec3 player = client.player.position();
        double dx = player.x - pos.x;
        double dz = player.z - pos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static long showMillis() {
        return ModConfig.INSTANCE.farming.garden.pestVacuumSeconds * 1000L;
    }

    private static boolean isHoldingVacuum(Minecraft client) {
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty()) return false;

        String id = SkyblockItemId.of(held);
        return id != null && id.contains(VACUUM_ID_PART);
    }

    private static boolean isEnabled() {
        return ModConfig.INSTANCE.farming.garden.showPestVacuumWaypoint && GameState.Server.isGarden();
    }
}
