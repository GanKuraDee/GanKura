package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Wishing Compass が指す先を割り出す。
 *
 * この羅針盤は、使うと目標の方へ向かって粒が一列に飛んでいく。
 * 粒の並びは向きしか教えてくれないので、1回では距離が分からない。
 * そこで離れた2地点から使い、2本の直線が最も近づく点を目標とみなす。
 *
 * 粒は同じ道筋を何度も繰り返して飛ぶので、並びが先頭へ戻ったところで
 * 1周ぶんの終わりとする。先頭とその直前の粒を結んだものが、その回の直線になる。
 *
 * 場所が出たら、そこに建ちうる構造物を絞り込んで名前を付ける。
 * 構造物は区画の境目をまたいで隣の区画へはみ出すことがあるため、
 * 「解のある区画」だけでは決められない。Y の高さと、区画からはみ出せる幅で絞る
 */
public final class WishingCompassHandler {

    private static final String COMPASS_ID = "WISHING_COMPASS";

    // 粒の受付を打ち切るまでの時間。実際にはどれも3.5秒ほどで出揃う
    private static final long PARTICLE_WINDOW_MILLIS = 5000;

    // 使った場所から、1粒目までの隔たりとして認める上限
    private static final double MAX_START_DISTANCE = 9.0;
    // 隣り合う粒の隔たりの上限。これを超えたら、並びが途切れたとみなす
    private static final double MAX_STEP = 0.6;

    // 2回目を使うのに要る、1回目からの隔たり(の2乗)。
    // 近すぎると2本の直線がほとんど重なり、交わる点が定まらない
    private static final double MIN_SEPARATION_SQ = 64.0;

    // 1度の右クリックで useItemOn と useItem の両方を通ることがあるので、
    // これより短い間隔で続いた呼び出しは同じ1回とみなす
    private static final long DOUBLE_USE_MILLIS = 250;

    // 2直線が平行に近いかどうかの境目。向きが揃うほど 0 に近づく
    private static final double PARALLEL_EPSILON = 1.0e-6;

    // Crystal Hollows で壊せるブロックが入る範囲。この外に出た答えは信用しない
    private static final AABB HOLLOWS = new AABB(201, 30, 201, 824, 189, 824);

    // 中央の Nucleus。ここで使うと粒が真っ直ぐ飛ばないので、外で使ってもらう
    private static final AABB NUCLEUS = new AABB(462, 63, 461, 564, 181, 565);

    // Jungle Temple の答えは水晶の場所に出る。入口はそこからこれだけずれている
    private static final Vec3 TEMPLE_DOOR_OFFSET = new Vec3(-57, 36, -21);

    private static final String JUNGLE_KEY_NAME = "Jungle Key";
    private static final String KINGS_SCENT_EFFECT = "King's Scent";
    // Tab に並ぶ水晶の行。まだ手に入れていないものだけがこう書かれている
    private static final String CRYSTAL_MISSING = "Not Found";

    // 絞り込んだ末に複数残ったときの色と名前
    private static final int UNSURE_ARGB = 0xFFAAAAAA;
    private static final String UNSURE_LABEL = "§7Wishing Target";

    // 中央の Nucleus が答えになることもある。ここだけは構造物の一覧に入らない
    private static final int NUCLEUS_ARGB = 0xFFFF55FF;
    private static final String NUCLEUS_LABEL = "§dCrystal Nucleus";

    /**
     * 羅針盤が指しうる構造物。
     *
     * Y の幅は観測されている値に構造物の高さぶんの余裕を持たせたもの。
     * X と Z は、その構造物が本来の区画から隣へはみ出せる限界で、
     * 区画の境目(どちらも 513)に構造物の大きさを足し引きした値になっている。
     * 上限・下限が無い向きは、Crystal Hollows の外壁が抑えるので指定しない
     */
    public enum Target {
        // Magma Fields。区画が全域に広がっているので、高さだけで見分ける
        BAL("§cBal", 0xFFFF5555, "Topaz", 30, 75, 201, 824, 201, 824),
        // Jungle。東(Mithril)と南(Goblin Holdout)へはみ出せる
        ODAWA("§aOdawa", 0xFF55FF55, "Amethyst", 73, 155, 201, 566, 201, 567),
        JUNGLE_TEMPLE("§aJungle Temple", 0xFF55FF55, "Amethyst", 72, 81, 201, 621, 201, 621),
        // Goblin Holdout。東(Precursor)と北(Jungle)へはみ出せる
        KING_YOLKAR("§6King Yolkar", 0xFFFFAA00, "Amber", 82, 168, 201, 572, 456, 824),
        GOBLIN_QUEEN("§eGoblin Queen", 0xFFFFFF55, "Amber", 125, 140, 201, 621, 404, 824),
        // Precursor Remnants。西(Goblin Holdout)と北(Mithril)へはみ出せる
        PRECURSOR_CITY("§fPrecursor City", 0xFFFFFFFF, "Sapphire", 121, 130, 405, 824, 405, 824),
        // Mithril Deposits。西(Jungle)と南(Precursor)へはみ出せる
        MINES_OF_DIVAN("§9Mines of Divan", 0xFF5555FF, "Jade", 97, 102, 404, 824, 201, 621);

        private final String label;
        private final int argb;
        private final String crystal;
        private final int minY;
        private final int maxY;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        Target(String label, int argb, String crystal,
               int minY, int maxY, int minX, int maxX, int minZ, int maxZ) {
            this.label = label;
            this.argb = argb;
            this.crystal = crystal;
            this.minY = minY;
            this.maxY = maxY;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public String label() {
            return label;
        }

        public int argb() {
            return argb;
        }

        private boolean canStandAt(Vec3 pos) {
            return pos.y >= minY && pos.y <= maxY
                    && pos.x >= minX && pos.x <= maxX
                    && pos.z >= minZ && pos.z <= maxZ;
        }
    }

    /** 届いた粒。受け取るのは通信の側なので、控えるだけにして読むのは後回しにする */
    private record Particle(Vec3 pos, long millis) {
    }

    private static final Queue<Particle> pending = new ConcurrentLinkedQueue<>();

    private static Trail first = null;
    private static Trail second = null;
    private static Vec3 solution = null;
    private static List<Target> targets = List.of();
    private static String label = UNSURE_LABEL;
    private static int argb = UNSURE_ARGB;
    // 二重に拾った右クリックを弾くための控え。数え直しても消さない
    private static long lastUseMillis = 0;

    private WishingCompassHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WishingCompassHandler::tick);
    }

    public static void reset() {
        pending.clear();
        first = null;
        second = null;
        solution = null;
        targets = List.of();
        label = UNSURE_LABEL;
        argb = UNSURE_ARGB;
    }

    /** 割り出した場所。まだ出ていなければ null */
    public static Vec3 solution() {
        return ModConfig.INSTANCE.mining.showWishingCompassWaypoint ? solution : null;
    }

    /** 目印に出す名前。絞り切れなかったときは、どれとも決めない名前になる */
    public static String solutionLabel() {
        return label;
    }

    public static int solutionArgb() {
        return argb;
    }

    /**
     * 通信の側から呼ばれる。粒を控えるだけで、読むのは {@link #tick}。
     *
     * 数え終わっているかどうかの読み取りは、控える必要があるかの目安に使うだけ。
     * 取りこぼしても余分に控えるだけで、並びの判定は tick 側でやり直される
     */
    public static void onParticle(double x, double y, double z) {
        if (!isEnabled() || !collecting()) return;

        pending.add(new Particle(new Vec3(x, y, z), System.currentTimeMillis()));
    }

    /** 右クリックした品が Wishing Compass なら、そこを起点にして数え始める */
    public static void onUseItem(ItemStack stack, Minecraft client) {
        if (!isEnabled() || client.player == null) return;
        if (!COMPASS_ID.equals(SkyblockItemId.of(stack))) return;

        long now = System.currentTimeMillis();
        if (now - lastUseMillis < DOUBLE_USE_MILLIS) return;
        lastUseMillis = now;

        if (collecting()) {
            say(client, "§eStill reading the last compass. Wait a moment.");
            return;
        }

        Vec3 used = client.player.position();
        if (NUCLEUS.contains(used)) {
            say(client, "§eUse the compass outside the Nucleus. The particles do not run straight in there.");
            return;
        }

        // 1本目が済んでいれば2本目へ進む。それ以外は、前の答えを捨てて数え直す
        if (first != null && first.done() && second == null) {
            if (first.used.distanceToSqr(used) < MIN_SEPARATION_SQ) {
                say(client, "§eMove further away before using the second compass.");
                return;
            }

            second = new Trail(used, now);
            return;
        }

        reset();
        first = new Trail(used, now);
    }

    private static void tick(Minecraft client) {
        if (!isEnabled()) {
            if (first != null || second != null) reset();
            return;
        }

        Trail trail = second != null ? second : first;
        if (trail == null || trail.done()) {
            pending.clear();
            return;
        }

        Particle particle;
        while ((particle = pending.poll()) != null) {
            trail.accept(particle);
        }

        if (!trail.done()) {
            if (System.currentTimeMillis() > trail.usedMillis + PARTICLE_WINDOW_MILLIS) {
                say(client, "§cNo compass particles arrived. Use the compass again.");
                reset();
            }
            return;
        }

        if (trail == first) {
            say(client, "§eUse the compass again from another spot.");
            return;
        }

        solve(client);
    }

    // まだ粒を待っている最中か
    private static boolean collecting() {
        Trail trail = second != null ? second : first;
        return trail != null && !trail.done();
    }

    private static void solve(Minecraft client) {
        Vec3 found = intersect(first, second);
        if (found == null || !HOLLOWS.contains(found)) {
            say(client, "§cCould not work out where the two compasses cross. Try again.");
            reset();
            return;
        }

        if (NUCLEUS.contains(found)) {
            solution = found;
            targets = List.of();
            label = NUCLEUS_LABEL;
            argb = NUCLEUS_ARGB;
            say(client, "§bWishing Compass §7» " + NUCLEUS_LABEL + " §7(§f" + coords(found) + "§7)");
            return;
        }

        targets = candidatesAt(found, client);
        // 神殿と決まったときだけ、水晶の場所から入口の方へずらす
        boolean single = targets.size() == 1;
        label = single ? targets.getFirst().label() : UNSURE_LABEL;
        argb = single ? targets.getFirst().argb() : UNSURE_ARGB;
        solution = single && targets.getFirst() == Target.JUNGLE_TEMPLE
                ? found.add(TEMPLE_DOOR_OFFSET) : found;

        say(client, "§bWishing Compass §7» " + names() + " §7(§f" + coords(solution) + "§7)");
    }

    // 残った候補を並べる。絞り切れなかったときは "A or B" の形にする
    private static String names() {
        if (targets.isEmpty()) return "§7somewhere unknown";

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) text.append(i == targets.size() - 1 ? " §7or " : "§7, ");
            text.append(targets.get(i).label());
        }

        return text.toString();
    }

    /**
     * 2本の直線が最も近づく点。
     *
     * ワールドの中でぴったり交わることはまず無いので、
     * 互いに最も近づく点を1つずつ求め、その中点を答えとする。
     * どちらも粒の飛んだ向きの先に無ければ、後ろへ延ばした先で近づいただけなので捨てる
     */
    private static Vec3 intersect(Trail a, Trail b) {
        Vec3 da = a.direction();
        Vec3 db = b.direction();
        if (da == null || db == null) return null;

        Vec3 gap = a.head.subtract(b.head);
        double dot = da.dot(db);
        double denominator = 1.0 - dot * dot;
        // 向きが揃っていると、どこで近づくかが定まらない
        if (Math.abs(denominator) < PARALLEL_EPSILON) return null;

        double alongA = (dot * db.dot(gap) - da.dot(gap)) / denominator;
        double alongB = (db.dot(gap) - dot * da.dot(gap)) / denominator;
        if (alongA <= 0 || alongB <= 0) return null;

        Vec3 onA = a.head.add(da.scale(alongA));
        Vec3 onB = b.head.add(db.scale(alongB));
        return onA.add(onB).scale(0.5);
    }

    /**
     * その場所に建ちうる構造物を絞り込む。
     *
     * 構造物は隣の区画へはみ出すことがあるので、場所だけでは1つに決まらない。
     * 手持ちの Jungle Key と King's Scent の効果、それに Tab に出ている水晶の
     * 取得状況を足して削る。それでも複数残ったら、無理に決めずそのまま出す
     */
    private static List<Target> candidatesAt(Vec3 pos, Minecraft client) {
        boolean jungleKey = hasJungleKey(client);
        boolean kingsScent = hasKingsScent(client);

        List<Target> found = new ArrayList<>();
        for (Target target : Target.values()) {
            if (!target.canStandAt(pos)) continue;

            // 鍵を持っていると神殿、持っていなければ村を指す
            if (target == Target.JUNGLE_TEMPLE && !jungleKey) continue;
            if (target == Target.ODAWA && jungleKey) continue;

            // King's Scent が付いていると女王、付いていなければ王を指す
            if (target == Target.GOBLIN_QUEEN && !kingsScent) continue;
            if (target == Target.KING_YOLKAR && kingsScent) continue;

            // 手に入れ済みの水晶は、もう羅針盤の行き先にならない
            if (Boolean.TRUE.equals(hasCrystal(client, target.crystal))) continue;

            found.add(target);
        }

        return List.copyOf(found);
    }

    private static boolean hasJungleKey(Minecraft client) {
        if (client.player == null) return false;

        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (stack.getHoverName().getString().contains(JUNGLE_KEY_NAME)) return true;
        }

        return false;
    }

    /**
     * その水晶を手に入れているか。Tab に行が見当たらなければ null。
     *
     * 分からないときに削ってしまうと本当の行き先まで消えるので、
     * 読めたときだけ絞り込みに使う
     */
    private static Boolean hasCrystal(Minecraft client, String crystal) {
        String line = tabLineContaining(client, crystal + " Crystal");
        if (line == null) return null;

        return !line.contains(CRYSTAL_MISSING);
    }

    // 効果は Tab の一覧に並ぶ。Hypixel は情報欄にも架空のプレイヤーを使っているので、そこも読める
    private static boolean hasKingsScent(Minecraft client) {
        return tabLineContaining(client, KINGS_SCENT_EFFECT) != null;
    }

    private static String tabLineContaining(Minecraft client, String text) {
        if (client.getConnection() == null) return null;

        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            Component name = info.getTabListDisplayName();
            if (name == null) continue;

            String line = name.getString();
            if (line.contains(text)) return line;
        }

        return null;
    }

    private static boolean isEnabled() {
        return ModConfig.INSTANCE.mining.solveWishingCompass && GameState.Server.isCrystalHollows();
    }

    private static String coords(Vec3 pos) {
        return Math.round(pos.x) + " " + Math.round(pos.y) + " " + Math.round(pos.z);
    }

    private static void say(Minecraft client, String message) {
        client.execute(() -> NotificationUtils.sendSystemChat(client, Component.literal(message)));
    }

    /**
     * 1回ぶんの粒の並び。
     *
     * 先頭の粒と、並びが先頭へ戻る直前の粒を控える。
     * この2点を結んだ向きが、その回の指す方角になる
     */
    private static final class Trail {

        private final Vec3 used;
        private final long usedMillis;

        private Vec3 head = null;
        private Vec3 previous = null;
        private Vec3 tail = null;

        private Trail(Vec3 used, long usedMillis) {
            this.used = used;
            this.usedMillis = usedMillis;
        }

        private boolean done() {
            return tail != null;
        }

        private Vec3 direction() {
            if (head == null || tail == null) return null;

            Vec3 delta = tail.subtract(head);
            return delta.lengthSqr() == 0 ? null : delta.normalize();
        }

        private void accept(Particle particle) {
            if (done() || particle.millis() > usedMillis + PARTICLE_WINDOW_MILLIS) return;

            Vec3 pos = particle.pos();

            if (head == null) {
                // 手元から飛び始めた粒だけを1粒目に採る。他所の粒に釣られないための足切り
                if (pos.distanceTo(used) >= MAX_START_DISTANCE) return;

                head = pos;
                previous = pos;
                return;
            }

            // 並びの続き。先へ伸ばしていく
            if (pos.distanceTo(previous) <= MAX_STEP) {
                previous = pos;
                return;
            }

            // 先頭へ戻ってきた。ここまでが1周ぶんなので、直前の粒を終点とする
            if (pos.distanceTo(head) <= MAX_STEP) tail = previous;
        }
    }
}
