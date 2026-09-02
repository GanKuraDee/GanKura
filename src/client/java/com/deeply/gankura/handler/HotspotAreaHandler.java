package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.HotspotPerk;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.ScoreboardUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hotspot の範囲を把握する。
 *
 * 中心は "HOTSPOT" という名前のアーマースタンド。
 * 半径は、縁をなぞるパーティクルまでの距離から見積もる。
 * ペットなどが出す別のパーティクルでぶれないよう、一番多く見た値を採る。
 *
 * SkyOcean の HotspotAPI を参考にしている。
 */
public class HotspotAreaHandler {

    private static final String HOTSPOT_NAME = "HOTSPOT";
    // 中心を探し直す間隔(tick)
    private static final int SCAN_INTERVAL_TICKS = 10;
    // 中心の下をこの段数まで見て、水面・溶岩面の高さを拾う
    private static final int SURFACE_SEARCH_DEPTH = 3;
    // ネットワークスレッドから溜まる座標の上限
    private static final int MAX_PENDING = 512;
    // 取っておく問いかけの数
    private static final int MAX_OFFERS = 32;
    // 知らせた場所を覚えておく数と、同じ Hotspot とみなす距離(ブロック)
    private static final int MAX_OFFERED_SPOTS = 64;
    private static final double SAME_SPOT_DISTANCE = 4.0;
    // その場所の Hotspot が見えなくなってから、忘れるまでの猶予(ミリ秒)。
    // アーマースタンドは読み直しで一瞬消えるので、すぐには忘れない
    private static final long FORGET_GRACE_MS = 60_000;

    // 見つけたときのタイトルの出し方(tick)
    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 40;
    // 知らせる音。Sea Creature のスポーン通知と揃えている
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.0f;

    private static final Random RANDOM = new Random();

    private static final Map<Long, Hotspot> hotspots = new HashMap<>();
    // 見つけたと知らせた Hotspot。ウェイポイントと Tracer がここを見る
    private static final List<Found> found = new ArrayList<>();
    // チャットに出した問いかけ。古いボタンを押されても困らないよう取っておく
    private static final Map<Integer, Offer> offers = new LinkedHashMap<>();
    private static int nextOfferId;
    // 一度知らせた場所。Hotspot の読み直しで知らせ直さないよう、Hotspot 自体とは別に覚えておく
    private static final Deque<Told> offeredSpots = new ArrayDeque<>();
    // ロビーを移ったのを見分けるための、前回のサーバー
    private static String lastServerId = "";
    private static final ConcurrentLinkedQueue<double[]> pending = new ConcurrentLinkedQueue<>();
    private static int ticks;

    /** 中心と半径が分かっている Hotspot */
    public record Circle(Vec3 center, double radius, int argb) {
    }

    /** 見つけたと知らせた Hotspot。expiresAt を過ぎたら印を消す */
    public record Found(Vec3 center, HotspotPerk perk, long expiresAt) {
    }

    // ボタンを押されたときに送る中身。Hotspot が消えても送れるよう写しを持つ
    private record Offer(Vec3 center, HotspotPerk perk) {
    }

    // 知らせた場所と、その時刻。湧き直しに気づけるよう時刻も持つ
    private record Told(Vec3 pos, long millis) {
    }

    private static final class Hotspot {
        private Vec3 center;
        // 見積もった半径ごとの回数。一番多いものを使う
        private final Map<Double, Integer> votes = new HashMap<>();
        private double radius;
        private HotspotPerk perk = HotspotPerk.UNKNOWN;
        private boolean seen;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HotspotAreaHandler::tick);
    }

    /** ネットワークスレッドから呼ばれる。座標を控えるだけにとどめる */
    public static void onParticle(double x, double y, double z) {
        if (pending.size() >= MAX_PENDING) return;
        pending.add(new double[] {x, y, z});
    }

    /** パーティクルを消して、代わりに円を出す設定か */
    public static boolean shouldHideParticles() {
        return ModConfig.INSTANCE.fishing.showHotspotCircle
                && ModConfig.INSTANCE.fishing.hideHotspotParticles;
    }

    // 円を出すか、見つけたことを知らせるか。どちらも使わないなら探す必要がない
    private static boolean tracking() {
        return ModConfig.INSTANCE.fishing.showHotspotCircle
                || ModConfig.INSTANCE.fishing.shareHotspot || alerting();
    }

    // 見つけたときに何か出す設定になっているか
    private static boolean alerting() {
        ModConfig.FishingCategory config = ModConfig.INSTANCE.fishing;
        return config.showHotspotFoundTitle || config.playHotspotFoundSound
                || config.showHotspotFoundWaypoint || config.showHotspotFoundTracer;
    }

    /** 印を出している Hotspot。時間切れのものは含まない */
    public static List<Found> found() {
        return found;
    }

    /** 描ける状態の Hotspot を返す */
    public static List<Circle> circles() {
        if (!ModConfig.INSTANCE.fishing.showHotspotCircle) return List.of();

        List<Circle> circles = new ArrayList<>();
        for (Hotspot hotspot : hotspots.values()) {
            if (hotspot.center == null || hotspot.radius <= 0) continue;
            circles.add(new Circle(hotspot.center, hotspot.radius, hotspot.perk.argb()));
        }
        return circles;
    }

    /** パーティクルを探す範囲。島ごとに Hotspot の大きさが違う */
    private static double maxRadius() {
        if (GameState.Server.isCrimsonIsle() || GameState.Server.isTorrhusCanyon()) return 25.0;
        if (GameState.Server.isJerrysWorkshop() || GameState.Server.isLotusAtoll()) return 16.0;
        return 9.0;
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null
                || !tracking()) {
            reset();
            return;
        }

        // サーバーが変われば別の島。同じ座標でも別の Hotspot なので覚え直す
        if (!lastServerId.equals(GameState.Server.id)) {
            lastServerId = GameState.Server.id;
            reset();
        }

        if (++ticks >= SCAN_INTERVAL_TICKS) {
            ticks = 0;
            scanCenters(client);
        }

        double[] particle;
        double max = maxRadius();
        while ((particle = pending.poll()) != null) {
            vote(particle[0], particle[2], max);
        }

        found.removeIf(spot -> System.currentTimeMillis() >= spot.expiresAt());
        forgetGoneSpots();

        offerFound(client);
    }

    private static void reset() {
        hotspots.clear();
        found.clear();
        offers.clear();
        offeredSpots.clear();
        pending.clear();
    }

    // 中心を探し直す。消えた Hotspot はここで落ちる
    private static void scanCenters(Minecraft client) {
        for (Hotspot hotspot : hotspots.values()) hotspot.seen = false;

        // 効果の行を探すのにも使うので、名前付きのスタンドを一度だけ集める
        List<ArmorStand> named = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand stand && stand.getCustomName() != null) named.add(stand);
        }

        for (ArmorStand stand : named) {
            if (!HOTSPOT_NAME.equals(stand.getCustomName().getString().trim())) continue;

            long key = key(stand.getX(), stand.getZ());
            Hotspot hotspot = hotspots.computeIfAbsent(key, k -> new Hotspot());
            hotspot.center = surfacePos(client, stand);
            hotspot.perk = perkOf(named, stand);
            hotspot.seen = true;
        }

        hotspots.values().removeIf(hotspot -> !hotspot.seen);
    }

    /**
     * Hotspot の効果を、すぐ下のアーマースタンドから読む。
     *
     * 同じ X/Z に立っていて、1ブロック以内下にあるものを目印にする
     */
    private static HotspotPerk perkOf(List<ArmorStand> named, ArmorStand center) {
        for (ArmorStand stand : named) {
            if (stand == center) continue;
            if (stand.getX() != center.getX() || stand.getZ() != center.getZ()) continue;
            if (stand.getY() >= center.getY() || center.getY() - stand.getY() > 1.0) continue;

            String text = ScoreboardUtils.stripColor(
                    ScoreboardUtils.toLegacyString(stand.getCustomName())).trim();
            return HotspotPerk.of(text);
        }
        return HotspotPerk.UNKNOWN;
    }

    /**
     * 狙っている効果の Hotspot を見つけたら、共有するかをチャットで尋ねる。
     *
     * 近づくのを待たず、アーマースタンドが見えた時点で尋ねる。
     * 読み直されても聞き直さないよう、同じ場所につき1度だけ。
     * 実際に送るのはボタンを押したときだけで、自動では送らない
     */
    private static void offerFound(Minecraft client) {
        if (!ModConfig.INSTANCE.fishing.shareHotspot && !alerting()) return;

        for (Hotspot hotspot : hotspots.values()) {
            if (hotspot.center == null || alreadyOffered(hotspot.center)) continue;
            if (!ModConfig.INSTANCE.fishing.sharedHotspotPerks.contains(hotspot.perk)) continue;

            announce(client, hotspot);
            return;
        }
    }

    /**
     * 見つけたことを知らせる。
     *
     * 同じ場所につき1度だけなので、どの知らせ方も揃ってここから出す
     */
    private static void announce(Minecraft client, Hotspot hotspot) {
        offeredSpots.addLast(new Told(hotspot.center, System.currentTimeMillis()));
        if (offeredSpots.size() > MAX_OFFERED_SPOTS) offeredSpots.removeFirst();

        ModConfig.FishingCategory config = ModConfig.INSTANCE.fishing;
        if (config.shareHotspot) offer(client, hotspot);

        if (config.showHotspotFoundTitle) {
            NotificationUtils.showTitle(client,
                    Component.literal("§d§lHOTSPOT"),
                    Component.literal(hotspot.perk.toString()),
                    TITLE_FADE, TITLE_STAY, TITLE_FADE);
        }

        if (config.playHotspotFoundSound) {
            NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SOUND_VOLUME, SOUND_PITCH);
        }

        if (config.showHotspotFoundWaypoint || config.showHotspotFoundTracer) {
            found.add(new Found(hotspot.center, hotspot.perk,
                    System.currentTimeMillis() + config.hotspotFoundSeconds * 1000L));
        }
    }

    // Hypixel がアーマースタンドを作り直しても同じものと見れるよう、位置で見る
    private static boolean alreadyOffered(Vec3 center) {
        for (Told spot : offeredSpots) {
            if (samePlace(spot.pos(), center)) return true;
        }
        return false;
    }

    /**
     * 知らせた場所を忘れる。
     *
     * その場所の Hotspot が消えてしばらく経ってから忘れることで、
     * 同じ場所に湧き直したときはもう一度知らせる。
     * Hotspot が立っている間は覚えたままなので、読み直しでは知らせ直さない
     */
    private static void forgetGoneSpots() {
        long now = System.currentTimeMillis();
        offeredSpots.removeIf(spot -> now - spot.millis() > FORGET_GRACE_MS && !stillThere(spot.pos()));
    }

    // その場所に今も Hotspot があるか
    private static boolean stillThere(Vec3 pos) {
        for (Hotspot hotspot : hotspots.values()) {
            if (hotspot.center != null && samePlace(hotspot.center, pos)) return true;
        }
        return false;
    }

    private static boolean samePlace(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz <= SAME_SPOT_DISTANCE * SAME_SPOT_DISTANCE;
    }

    // ボタン付きの問いかけを出す。押されたら share(id) に戻ってくる
    private static void offer(Minecraft client, Hotspot hotspot) {
        int id = nextOfferId++;
        offers.put(id, new Offer(hotspot.center, hotspot.perk));
        if (offers.size() > MAX_OFFERS) offers.remove(offers.keySet().iterator().next());

        MutableComponent button = Component.literal(" [Share]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/gankura sharehotspot " + id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                        "Click to send the coordinates to " + ModConfig.INSTANCE.fishing.hotspotShareChannel))));

        NotificationUtils.sendSystemChat(client, Component.empty()
                .append(Component.literal("HOTSPOT ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                // 円の色と揃えると、どの Hotspot の話か見分けやすい
                .append(Component.literal(hotspot.perk.plainLabel())
                        .withStyle(style -> style.withColor(hotspot.perk.argb() & 0xFFFFFF)))
                .append(Component.literal(" " + coords(hotspot.center)).withStyle(ChatFormatting.GRAY))
                .append(button));
    }

    /** チャットのボタンから呼ばれる。ここで初めて座標を送る */
    public static void share(int id) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Offer offer = offers.remove(id);
        if (offer == null) {
            NotificationUtils.sendSystemChat(client, Component.literal(
                    "This hotspot can no longer be shared.").withStyle(ChatFormatting.RED));
            return;
        }

        client.player.connection.sendCommand(shareMessage(offer));
    }

    private static String shareMessage(Offer offer) {
        return ModConfig.INSTANCE.fishing.hotspotShareChannel.command()
                + " " + coords(offer.center()) + " | " + offer.perk().plainLabel() + " | " + antiSpam();
    }

    private static String coords(Vec3 center) {
        return String.format("x: %d, y: %d, z: %d",
                Math.round(center.x), Math.round(center.y), Math.round(center.z));
    }

    // 同じ文面を繰り返して弾かれないよう、毎回変わる短い印を付ける
    private static String antiSpam() {
        return Integer.toString(RANDOM.nextInt(0x1000), 16);
    }

    /**
     * 円を描く高さ。中心の下にある水面・溶岩面に合わせる。
     *
     * 見つからなければ、アーマースタンドの高さをそのまま使う
     */
    private static Vec3 surfacePos(Minecraft client, ArmorStand stand) {
        BlockPos base = BlockPos.containing(stand.getX(), stand.getY(), stand.getZ());
        for (int i = 0; i < SURFACE_SEARCH_DEPTH; i++) {
            BlockPos pos = base.below(i);
            FluidState fluid = client.level.getFluidState(pos);
            if (fluid.isEmpty()) continue;

            return new Vec3(stand.getX(), pos.getY() + fluid.getHeight(client.level, pos) + 0.1, stand.getZ());
        }
        return new Vec3(stand.getX(), stand.getY(), stand.getZ());
    }

    // 一番近い Hotspot に、そのパーティクルまでの距離を半径として投票する
    private static void vote(double x, double z, double max) {
        Hotspot best = null;
        double bestDistance = max * max;
        for (Hotspot hotspot : hotspots.values()) {
            if (hotspot.center == null) continue;

            double dx = x - hotspot.center.x;
            double dz = z - hotspot.center.z;
            double distance = dx * dx + dz * dz;
            if (distance > bestDistance) continue;

            bestDistance = distance;
            best = hotspot;
        }
        if (best == null) return;

        // 0.5 刻みに丸めてから数える。細かいぶれを同じ値にまとめるため
        double radius = Math.round(Math.sqrt(bestDistance) * 2) / 2.0;
        int count = best.votes.merge(radius, 1, Integer::sum);

        Integer bestCount = best.votes.get(best.radius);
        if (bestCount == null || count >= bestCount) best.radius = radius;
    }

    private static long key(double x, double z) {
        return ((long) Math.floor(x) << 32) ^ (int) Math.floor(z);
    }
}
