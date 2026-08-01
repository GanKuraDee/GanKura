package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

public class EntityHighlightManager {

    // Magma Boss の戦闘エリア半径(ブロック)。ワールドテキストの基準座標 MAGMA_BOSS_POS を中心とする
    private static final double MAGMA_ARENA_RADIUS = 60.0;
    // バニラで自然発生する MagmaCube の最大サイズ(1/2/4 のいずれか)。
    // Magma Boss 本体と Magma Glare はこれより大きいため、超えているものだけを候補にすれば
    // Sea Creature や Unstable Magma など同じ MagmaCube 型のモブと区別できる
    private static final int MAGMA_MAX_NATURAL_SIZE = 4;

    // Bladesoul / Ashfang の探索範囲(ブロック)。各ボスのワールドテキスト基準座標を中心とした直方体
    private static final double BLADESOUL_AREA_XZ = 35.0;
    private static final double ASHFANG_AREA_XZ = 30.0;
    private static final double CRIMSON_AREA_Y = 5.0;
    // Wither Skeleton と Blaze を「同一のBladesoul本体」とみなす距離。
    // 水平方向はほぼ重なるが高さは数ブロックずれるため、縦だけ別枠で緩く判定する
    private static final double BLADESOUL_PAIR_XZ = 3.0;
    private static final double BLADESOUL_PAIR_Y = 6.0;
    // Ashfang の本体を構成する Blaze の数。この数だけ揃って初めて「存在する」と判定する
    private static final int ASHFANG_BLAZE_COUNT = 2;

    // Barbarian Duke X / Mage Outlaw はプレイヤーエンティティ型のボス。
    // これらのプレイヤー名は一意なので、名前の照合だけで実在の他プレイヤーと区別できる。
    // ネームタグ(ArmorStand)より追跡範囲が広く、遠距離でも存在を判定できる(画面上の表示名とは異なる名前)
    private static final String BARBARIAN_ENTITY_NAME = "DukeBarb";
    private static final String MAGE_OUTLAW_ENTITY_NAME = "Mage Outlaw";

    public static final Set<Entity> highlightedEntities = new HashSet<>();
    public static final Map<Entity, CrimsonBossEntry> crimsonBossEntities = new HashMap<>();
    public static final Set<Entity> magmaGlareEntities = new HashSet<>();
    public static final Set<Entity> arachneEntities = new HashSet<>();
    public static final Set<Entity> arachneBroodEntities = new HashSet<>();
    // ネームプレート表示対象 → 表示文字列。Glowingとは独立して有効化できるよう別管理とし、
    // 毎tick作り直すことで削除済みエンティティの掃除を不要にしている
    public static final Map<Entity, String> nameplateEntities = new LinkedHashMap<>();
    // Ashfangの本体は2体のBlazeで構成されるが、Tracerは1本だけ出したいので、
    // 基準座標(ASHFANG_POS)に近い方の1体を描画対象として保持する
    public static Entity ashfangTracerTarget = null;

    // 「スポーン地点のチャンクは読み込まれているのに未検出」という状態が始まった時刻(ボス名 → epoch ms)
    private static final Map<String, Long> absenceSince = new HashMap<>();
    // 未検出がこの時間続いた場合のみ「いない」と確定する(4tick)。
    // Bladesoul のペア判定や Ashfang の2体判定は1〜2tick外れることがあるため、その分だけ吸収する
    private static final long ABSENCE_CONFIRM_DELAY_MS = 200L;
    // 判定できる範囲にいた間、最後に確定した状態が「いた」だったボス。
    // 範囲外に出た後は生死を判別できないため、この状態を引き継いで表示に使う
    // (Arachne の everConfirmed / lastConfirmedWasReady と同じ考え方)
    private static final Set<String> lastConfirmedSpawned = new HashSet<>();
    // 「いない」と確定した時刻(ボス名 → epoch ms)。範囲外に出ても保持し、
    // リスポーン推定タイマーを継続させる。再検出できたら破棄する
    private static final Map<String, Long> killedConfirmedAt = new HashMap<>();
    // Nether Boss のリスポーン間隔(CrimsonDropHandler のタイマーと同じ2分)
    private static final long RESPAWN_INTERVAL_MS = 2 * 60 * 1000L;

    // Ashfang のフォロワー3種（ハイライト・トレーサーの個別設定付き）
    public static final List<CrimsonBossEntry> ASHFANG_FOLLOWERS = List.of(
        new CrimsonBossEntry("Ashfang Follower",
            0x555555, 0xFF555555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangFollowerHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangFollowerTracer,
            () -> false,
            () -> false,
            () -> GameState.AshfangFollower.isDetected,
            d -> GameState.AshfangFollower.isDetected = d,
            () -> null,
            h -> {}),
        new CrimsonBossEntry("Ashfang Acolyte",
            0x5555FF, 0xFF5555FF,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangAcolyteHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangAcolyteTracer,
            () -> false,
            () -> false,
            () -> GameState.AshfangAcolyte.isDetected,
            d -> GameState.AshfangAcolyte.isDetected = d,
            () -> null,
            h -> {}),
        new CrimsonBossEntry("Ashfang Underling",
            0xFF5555, 0xFFFF5555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangUnderlingHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangUnderlingTracer,
            () -> false,
            () -> false,
            () -> GameState.AshfangUnderling.isDetected,
            d -> GameState.AshfangUnderling.isDetected = d,
            () -> null,
            h -> {})
    );

    public static final List<CrimsonBossEntry> CRIMSON_BOSSES = List.of(
        new CrimsonBossEntry("Bladesoul",
            0x555555, 0xFF555555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBladesoulHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBladesoulTracer,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBladesoulNameplate,
            () -> ModConfig.INSTANCE.crimsonIsle.showBladesoulNameplateHealth,
            () -> GameState.Bladesoul.isDetected,
            d -> GameState.Bladesoul.isDetected = d,
            () -> GameState.Bladesoul.health,
            h -> GameState.Bladesoul.health = h),
        new CrimsonBossEntry("Barbarian Duke X",
            0xFF5555, 0xFFFF5555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBarbarianHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBarbarianTracer,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBarbarianNameplate,
            () -> ModConfig.INSTANCE.crimsonIsle.showBarbarianNameplateHealth,
            () -> GameState.BarbarianDukeX.isDetected,
            d -> GameState.BarbarianDukeX.isDetected = d,
            () -> GameState.BarbarianDukeX.health,
            h -> GameState.BarbarianDukeX.health = h),
        new CrimsonBossEntry("Mage Outlaw",
            0xAA00AA, 0xFFAA00AA,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMageOutlawHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMageOutlawTracer,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMageOutlawNameplate,
            () -> ModConfig.INSTANCE.crimsonIsle.showMageOutlawNameplateHealth,
            () -> GameState.MageOutlaw.isDetected,
            d -> GameState.MageOutlaw.isDetected = d,
            () -> GameState.MageOutlaw.health,
            h -> GameState.MageOutlaw.health = h),
        new CrimsonBossEntry("Ashfang",
            0xAAAAAA, 0xFFAAAAAA,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangTracer,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangNameplate,
            () -> ModConfig.INSTANCE.crimsonIsle.showAshfangNameplateHealth,
            () -> GameState.Ashfang.isDetected,
            d -> GameState.Ashfang.isDetected = d,
            () -> GameState.Ashfang.health,
            h -> GameState.Ashfang.health = h),
        new CrimsonBossEntry("Magma Boss",
            0xFFAA00, 0xFFFFAA00,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMagmaBossHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTracer,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMagmaBossNameplate,
            () -> ModConfig.INSTANCE.crimsonIsle.showMagmaBossNameplateHealth,
            () -> GameState.MagmaBoss.isDetected,
            d -> GameState.MagmaBoss.isDetected = d,
            () -> GameState.MagmaBoss.health,
            h -> GameState.MagmaBoss.health = h)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> updateHighlights(client));
    }

    // Crimson Isle 各ボスのスポーン地点(ワールドテキストの基準座標)
    public static BlockPos spawnPosOf(String bossName) {
        return switch (bossName) {
            case "Bladesoul"        -> ModConstants.BLADESOUL_POS;
            case "Barbarian Duke X" -> ModConstants.BARBARIAN_DUKE_X_POS;
            case "Mage Outlaw"      -> ModConstants.MAGE_OUTLAW_POS;
            case "Ashfang"          -> ModConstants.ASHFANG_POS;
            case "Magma Boss"       -> ModConstants.MAGMA_BOSS_POS;
            default                 -> null;
        };
    }

    // 「検出できない = 存在しない」と確定できるか。
    //
    // 判定の土台は canObserveSpawnPoint()。ボスの探索範囲のチャンクがすべて読み込まれている場合のみ、
    // 「検出できない = 存在しない」と言い切れる。
    //
    // ただし検出が1tickだけ外れるといったちらつきがあるため、
    // 未検出の状態が ABSENCE_CONFIRM_DELAY_MS 続いた場合にのみ確定する
    public static boolean canConfirmAbsence(String bossName) {
        Long since = absenceSince.get(bossName);
        return since != null && System.currentTimeMillis() - since >= ABSENCE_CONFIRM_DELAY_MS;
    }

    // 判定できる範囲にいた間、最後に確定した状態が「いた」だったか。
    // 範囲外では撃破されたかどうか分からないため、Arachne と同じく Spawned/Killed 扱いにする
    public static boolean wasSpawnedWhenLastConfirmed(String bossName) {
        return lastConfirmedSpawned.contains(bossName);
    }

    // 「いない」と確定済みか。範囲外に出ても保持される
    public static boolean wasKilledConfirmed(String bossName) {
        return killedConfirmedAt.containsKey(bossName);
    }

    // Killed 確定からリスポーン間隔までの残り時間(ms)。確定していない、または経過済みなら 0。
    // 撃破メッセージを見ていない場合のリスポーン推定に使う
    public static long killedRemainingMs(String bossName) {
        Long at = killedConfirmedAt.get(bossName);
        if (at == null) return 0L;
        return Math.max(0L, at + RESPAWN_INTERVAL_MS - System.currentTimeMillis());
    }

    // Magma Boss の戦闘エリア(基準座標を中心とした球)の内側にいる MagmaCube を返す。
    // 立方体で絞ってから球で判定し、角のぶんを取りこぼさないようにしている
    private static List<MagmaCube> magmaCubesInArena(Minecraft client, Predicate<MagmaCube> filter) {
        Vec3 center = Vec3.atCenterOf(ModConstants.MAGMA_BOSS_POS);
        AABB box = new AABB(
                center.x - MAGMA_ARENA_RADIUS, center.y - MAGMA_ARENA_RADIUS, center.z - MAGMA_ARENA_RADIUS,
                center.x + MAGMA_ARENA_RADIUS, center.y + MAGMA_ARENA_RADIUS, center.z + MAGMA_ARENA_RADIUS);
        return client.level.getEntitiesOfClass(MagmaCube.class, box,
                e -> e.distanceToSqr(center) <= MAGMA_ARENA_RADIUS * MAGMA_ARENA_RADIUS && filter.test(e));
    }

    // 検出に使っている探索範囲(XZ半径)。この範囲のチャンクがすべて読み込まれていれば、
    // 「検出できない = 本当にいない」と言い切れる
    private static int confirmRadiusOf(String bossName) {
        return switch (bossName) {
            case "Magma Boss"           -> (int) Math.ceil(MAGMA_ARENA_RADIUS);
            case "Bladesoul"            -> (int) Math.ceil(BLADESOUL_AREA_XZ);
            case "Ashfang"              -> (int) Math.ceil(ASHFANG_AREA_XZ);
            // プレイヤー型ボスは名前で読み込み済み全体から探すため、スポーン地点のチャンクだけ確認する
            default                     -> 0;
        };
    }

    // ボスの探索範囲がすべて観測できている状態か。
    // 一部でも未読込だと、そこにボスが居ても検出できないため「いない」と確定してはいけない。
    //
    // 範囲判定の hasChunksAt() は内部で hasChunk(int,int) を呼ぶが、
    // ClientLevel はこれを常に true で返すため使えない。
    // チャンクマネージャを経由する isLoaded(BlockPos) を各チャンクに対して個別に確認する
    private static boolean canObserveSpawnPoint(Minecraft client, String bossName) {
        BlockPos spawnPos = spawnPosOf(bossName);
        if (client.level == null || spawnPos == null) return false;

        // Magma Boss はサイドバーの表示だけで判定できる。
        // 「Magma Chamber」の行が出ていればエリア内なので、フェーズ判定用の行が
        // 1つも出ていない = 撃破済み、と確定してよい(エンティティの読み込み状況に依存しない)
        if ("Magma Boss".equals(bossName)) return GameState.MagmaBoss.inArena;

        int radius = confirmRadiusOf(bossName);
        int minChunkX = (spawnPos.getX() - radius) >> 4;
        int maxChunkX = (spawnPos.getX() + radius) >> 4;
        int minChunkZ = (spawnPos.getZ() - radius) >> 4;
        int maxChunkZ = (spawnPos.getZ() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // 各チャンク内の任意の1ブロックで判定できる
                if (!client.level.isLoaded(new BlockPos((chunkX << 4), spawnPos.getY(), (chunkZ << 4)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void updateHighlights(Minecraft client) {
        // Crimson ボス関連マップは毎 tick クリアして再検出（エンティティ参照が変わるため）
        // Arachne's Brood も複数体が同時に増減するため同様に毎 tick クリアして再検出する
        highlightedEntities.removeIf(e -> crimsonBossEntities.containsKey(e) || magmaGlareEntities.contains(e) || arachneBroodEntities.contains(e));
        crimsonBossEntities.clear();
        magmaGlareEntities.clear();
        arachneBroodEntities.clear();
        nameplateEntities.clear();
        ashfangTracerTarget = null;

        if (client.level == null || client.player == null) {
            highlightedEntities.clear();
            // ワールド遷移をまたいで古い計測・ラッチを持ち越さない
            absenceSince.clear();
            lastConfirmedSpawned.clear();
            killedConfirmedAt.clear();
            return;
        }

        ModConfig.TheEndCategory theEnd = ModConfig.INSTANCE.theEnd;
        ModConfig.SpidersDenCategory spidersDen = ModConfig.INSTANCE.spidersDen;

        // 各ボスについて「その場に居るか(Present)」「Glowing対象か(glow)」「探索が必要か(scan)」を分けて持つ。
        // ネームプレートのみ有効な場合も探索は必要だが、Glowing対象には加えない
        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean golemPresent = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);
        boolean glowGolem = golemPresent && theEnd.enableGolemHighlight;
        boolean scanGolem = golemPresent && (theEnd.enableGolemHighlight || theEnd.enableGolemNameplate);

        boolean isSpidersDen = ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map);
        boolean broodmotherPresent = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);
        boolean glowBroodmother = broodmotherPresent && spidersDen.enableBroodmotherHighlight;
        boolean scanBroodmother = broodmotherPresent && (spidersDen.enableBroodmotherHighlight || spidersDen.enableBroodmotherNameplate);
        // Sanctuary内かつ蜘蛛の巣を検知できている間のみスキャンする(スポーン前・撃破後は存在しないため)
        boolean arachnePresent = GameState.Arachne.inSanctuary && GameState.Arachne.cobwebDetected;
        boolean glowArachne = arachnePresent && spidersDen.enableArachneHighlight;
        boolean scanArachne = arachnePresent && (spidersDen.enableArachneHighlight || spidersDen.enableArachneNameplate);

        boolean dragonPresent = isTheEnd && GameState.Dragon.type != null;
        boolean glowDragon = dragonPresent && theEnd.enableDragonHighlight;
        boolean scanDragon = dragonPresent && (theEnd.enableDragonHighlight || theEnd.enableDragonNameplate);

        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map) || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        boolean scanCrimsonBosses = isCrimsonIsle && CRIMSON_BOSSES.stream().anyMatch(b -> b.enableHighlight().get() || b.enableNameplate().get());
        boolean scanMagmaGlare = isCrimsonIsle
                && "Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)
                && (ModConfig.INSTANCE.crimsonIsle.enableMagmaGlareHighlight
                        || ModConfig.INSTANCE.crimsonIsle.enableMagmaGlareNameplate);
        boolean scanAshfangFollowers = isCrimsonIsle
                && ASHFANG_FOLLOWERS.stream().anyMatch(f -> f.enableHighlight().get() || f.enableTracer().get());

        // 条件が無効になったカテゴリのエンティティを削除(Glowing対象かどうかで判定する)
        if (!glowGolem)       highlightedEntities.removeIf(e -> e instanceof IronGolem);
        if (!glowBroodmother) highlightedEntities.removeIf(e -> e instanceof Spider);
        if (!glowDragon)      highlightedEntities.removeIf(e -> e instanceof EnderDragon);
        if (!glowArachne)     highlightedEntities.removeAll(arachneEntities);
        if (!scanArachne)     arachneEntities.clear();

        // ワールドから削除済みのエンティティを削除
        highlightedEntities.removeIf(Entity::isRemoved);
        arachneEntities.removeIf(Entity::isRemoved);

        // Crimson Isle にいない場合は isDetected をリセット
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : CRIMSON_BOSSES) boss.setIsDetected().accept(false);
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !scanDragon && !scanCrimsonBosses && !scanMagmaGlare && !scanAshfangFollowers) return;

        boolean[] bossFound = new boolean[CRIMSON_BOSSES.size()];
        boolean[] followerFound = new boolean[ASHFANG_FOLLOWERS.size()];

        for (Entity entity : client.level.entitiesForRendering()) {
            Component customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanBroodmother && ModConstants.containsIgnoreCase(nameStr, "Broodmother")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<Spider> spiders = client.level.getEntitiesOfClass(Spider.class, searchBox, e -> true);
                Entity closest = getClosestEntity(spiders, entity);
                if (closest != null) {
                    if (glowBroodmother) highlightedEntities.add(closest);
                    if (spidersDen.enableBroodmotherNameplate) {
                        nameplateEntities.put(closest, BossNameplateRenderer.buildLabel("§c§lBroodmother", GameState.Broodmother.health, ModConfig.INSTANCE.spidersDen.showBroodmotherNameplateHealth));
                    }
                }
            }

            if (scanArachne && ModConstants.isArachneBossName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne");
                if (visualTarget != null) {
                    if (glowArachne) highlightedEntities.add(visualTarget);
                    arachneEntities.add(visualTarget);
                    if (spidersDen.enableArachneNameplate) {
                        nameplateEntities.put(visualTarget, BossNameplateRenderer.buildLabel("§5§lArachne", GameState.Arachne.health, ModConfig.INSTANCE.spidersDen.showArachneNameplateHealth));
                    }
                }
            }

            if (scanArachne && ModConstants.isArachneBroodName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne's Brood");
                if (visualTarget != null) {
                    if (glowArachne) highlightedEntities.add(visualTarget);
                    arachneBroodEntities.add(visualTarget);
                }
            }

            if (glowDragon && ModConstants.containsIgnoreCase(nameStr, "Dragon")) {
                AABB searchBox = entity.getBoundingBox().inflate(32.0);
                List<EnderDragon> dragons = client.level.getEntitiesOfClass(EnderDragon.class, searchBox, e -> true);
                Entity closest = getClosestEntity(dragons, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanCrimsonBosses) {
                for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                    CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                    boolean glowBoss = boss.enableHighlight().get();
                    boolean plateBoss = boss.enableNameplate().get();
                    if (!glowBoss && !plateBoss) continue;
                    // Bladesoul / Ashfang / Magma Boss はネームタグではなくエンティティの構成で判定する(後述の専用ブロック)
                    if ("Bladesoul".equals(boss.nameTag())
                            || "Ashfang".equals(boss.nameTag())
                            || "Magma Boss".equals(boss.nameTag())) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, boss.nameTag());
                        if (visualTarget != null) {
                            if (glowBoss) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, boss);
                            if (plateBoss) {
                                String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                                nameplateEntities.put(visualTarget, BossNameplateRenderer.buildLabel(label, boss.getHealth().get(), boss.enableNameplateHealth().get()));
                            }
                        }

                        bossFound[i] = true;
                    }
                }
            }

            if (scanAshfangFollowers) {
                for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                    CrimsonBossEntry follower = ASHFANG_FOLLOWERS.get(i);
                    if (!follower.enableHighlight().get() && !follower.enableTracer().get()) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, follower.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, follower.nameTag());
                        if (visualTarget != null) {
                            if (follower.enableHighlight().get()) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, follower);
                            followerFound[i] = true;
                        }
                    }
                }
            }
        }

        // Golem(Endstone Protector): The End に存在する IronGolem はこのボスしかいないため、
        // Stage 5 の間はネームタグを読めなくても IronGolem 自体を本体とみなす。
        // ネームプレートは1体だけ出したいので、最初に見つかった個体を対象にする
        if (scanGolem) {
            Entity plateGolem = null;
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof IronGolem)) continue;
                if (glowGolem) highlightedEntities.add(entity);
                if (plateGolem == null) plateGolem = entity;
            }
            if (theEnd.enableGolemNameplate && plateGolem != null) {
                nameplateEntities.put(plateGolem, BossNameplateRenderer.buildLabel("§6§lGolem", GameState.Golem.health, ModConfig.INSTANCE.theEnd.showGolemNameplateHealth));
            }
        }

        // Dragonはネームタグ経由で取りこぼすことがあるため、EnderDragon本体を直接探して補完する。
        // ネームプレートは1体だけ出したいので、最初に見つかった個体を対象にする
        if (scanDragon) {
            boolean needGlowFallback = glowDragon && highlightedEntities.stream().noneMatch(e -> e instanceof EnderDragon);
            Entity plateDragon = null;
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof EnderDragon)) continue;
                if (needGlowFallback) highlightedEntities.add(entity);
                if (plateDragon == null) plateDragon = entity;
            }
            if (theEnd.enableDragonNameplate && plateDragon != null) {
                String label = dragonColorCode(GameState.Dragon.type) + "§l" + GameState.Dragon.type + " Dragon";
                nameplateEntities.put(plateDragon, BossNameplateRenderer.buildLabel(label, GameState.Dragon.health, ModConfig.INSTANCE.theEnd.showDragonNameplateHealth));
            }
        }

        // Barbarian Duke X / Mage Outlaw: 名前が一致するプレイヤーを本体とみなす。
        // ネームタグが読み込まれない距離でも「存在するか」を判定できる
        if (isCrimsonIsle) {
            detectPlayerBoss(client, "Barbarian Duke X", BARBARIAN_ENTITY_NAME, bossFound);
            detectPlayerBoss(client, "Mage Outlaw", MAGE_OUTLAW_ENTITY_NAME, bossFound);
        }

        // Bladesoul: Blaze + Wither Skeleton の合体構成。ネームタグに頼らず、スポーン地点周辺で
        // 「ほぼ同じ座標に重なった Wither Skeleton と Blaze」の組を本体と判定する
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                if (!"Bladesoul".equals(boss.nameTag())) continue;

                boolean glowBoss = boss.enableHighlight().get();
                boolean plateBoss = boss.enableNameplate().get();
                if (!glowBoss && !plateBoss) break;

                Vec3 center = Vec3.atCenterOf(ModConstants.BLADESOUL_POS);
                AABB area = new AABB(
                        center.x - BLADESOUL_AREA_XZ, center.y - CRIMSON_AREA_Y, center.z - BLADESOUL_AREA_XZ,
                        center.x + BLADESOUL_AREA_XZ, center.y + CRIMSON_AREA_Y, center.z + BLADESOUL_AREA_XZ);
                List<WitherSkeleton> skeletons = client.level.getEntitiesOfClass(WitherSkeleton.class, area, e -> true);
                if (skeletons.isEmpty()) break;
                // Blaze は Wither Skeleton から数ブロック上下にずれるため、範囲の上下だけ広げて探す
                List<Blaze> blazes = client.level.getEntitiesOfClass(
                        Blaze.class, area.inflate(0, BLADESOUL_PAIR_Y, 0), e -> true);
                if (blazes.isEmpty()) break;

                Entity plateTarget = null;
                for (WitherSkeleton skeleton : skeletons) {
                    Entity pairedBlaze = null;
                    for (Blaze blaze : blazes) {
                        if (isBladesoulPair(skeleton, blaze)) {
                            pairedBlaze = blaze;
                            break;
                        }
                    }
                    if (pairedBlaze == null) continue;

                    if (glowBoss) {
                        highlightedEntities.add(skeleton);
                        highlightedEntities.add(pairedBlaze);
                    }
                    crimsonBossEntities.put(skeleton, boss);
                    crimsonBossEntities.put(pairedBlaze, boss);
                    // ネームプレートとTracerは見た目の本体である Wither Skeleton 側に出す
                    if (plateTarget == null) plateTarget = skeleton;
                    bossFound[i] = true;
                }

                if (plateBoss && plateTarget != null) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(plateTarget, BossNameplateRenderer.buildLabel(label, boss.getHealth().get(), boss.enableNameplateHealth().get()));
                }
                break;
            }
        }

        // Ashfang: 本体は2体のBlazeで構成される。ネームタグに頼らず、スポーン地点周辺のBlazeで判定する。
        // Follower/Acolyte/Underling も同じBlazeなので、直前のループで検出済みのものは除外したうえで
        // 基準座標に近い2体を本体として採用する
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                if (!"Ashfang".equals(boss.nameTag())) continue;

                boolean glowBoss = boss.enableHighlight().get();
                boolean plateBoss = boss.enableNameplate().get();
                if (!glowBoss && !plateBoss) break;

                Vec3 center = Vec3.atCenterOf(ModConstants.ASHFANG_POS);
                AABB area = new AABB(
                        center.x - ASHFANG_AREA_XZ, center.y - CRIMSON_AREA_Y, center.z - ASHFANG_AREA_XZ,
                        center.x + ASHFANG_AREA_XZ, center.y + CRIMSON_AREA_Y, center.z + ASHFANG_AREA_XZ);
                List<Blaze> blazes = new ArrayList<>(client.level.getEntitiesOfClass(Blaze.class, area,
                        e -> !crimsonBossEntities.containsKey(e)));
                if (blazes.size() < ASHFANG_BLAZE_COUNT) break;
                blazes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));

                Entity plateTarget = null;
                for (int n = 0; n < ASHFANG_BLAZE_COUNT; n++) {
                    Blaze blaze = blazes.get(n);
                    if (glowBoss) highlightedEntities.add(blaze);
                    crimsonBossEntities.put(blaze, boss);
                    // 基準座標に近い順にソート済み。ネームプレートとTracerはどちらも先頭の1体に出す
                    if (plateTarget == null) {
                        plateTarget = blaze;
                        ashfangTracerTarget = blaze;
                    }
                }

                if (plateBoss) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(plateTarget, BossNameplateRenderer.buildLabel(label, boss.getHealth().get(), boss.enableNameplateHealth().get()));
                }
                bossFound[i] = true;
                break;
            }
        }

        // Magma Glare: Kill the Magmas フェーズ中は本体が存在しないので、エリア内の MagmaCube は
        // すべて分裂した個体。ただし同フェーズには無害な Unstable Magma も湧いており、
        // そちらはバニラ自然湧きサイズの小さい個体なのでサイズで振り分ける。
        // ネームタグに依存しないので、個体数が多くても取りこぼさない
        if (scanMagmaGlare && GameState.MagmaBoss.inArena) {
            boolean glowGlare = ModConfig.INSTANCE.crimsonIsle.enableMagmaGlareHighlight;
            boolean plateGlare = ModConfig.INSTANCE.crimsonIsle.enableMagmaGlareNameplate;
            for (MagmaCube cube : magmaCubesInArena(client, e -> e.getSize() > MAGMA_MAX_NATURAL_SIZE)) {
                if (glowGlare) highlightedEntities.add(cube);
                magmaGlareEntities.add(cube);
                // 即死級のダメージを与えてくるため、体力ではなく警告として目立たせる
                if (plateGlare) nameplateEntities.put(cube, "§c§l! Magma Glare !");
            }
        }

        // Magma Boss: 基準座標から一定範囲内の MagmaCube を本体とみなす。ネームタグには依存しない。
        //
        // 判定はエリア内(spawnStatus が読める = Magma Chamber のサイドバーが出ている)に限定する。
        // エリア外では Sea Creature など同じ MagmaCube 型のモブを誤検出してしまうため行わない。
        // Kill the Magmas / Reforming 中は本体が存在しないので除外する。
        if (isCrimsonIsle) {
            String magmaStatus = GameState.MagmaBoss.spawnStatus;
            // フェーズ行が読めていることに加え、念のため「Magma Chamber」の行も確認する
            boolean inArena = magmaStatus != null && GameState.MagmaBoss.inArena;
            boolean bodyAbsent = "Kill the Magmas".equals(magmaStatus) || "Reforming...".equals(magmaStatus);
            if (inArena && !bodyAbsent) {
                for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                    CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                    if (!"Magma Boss".equals(boss.nameTag())) continue;

                    boolean glowBoss = boss.enableHighlight().get();
                    boolean plateBoss = boss.enableNameplate().get();
                    if (!glowBoss && !plateBoss) break;

                    // Final Stage 以外では本体が巨大なので、サイズでも絞り込む。
                    // Final Stage は本体が小さくなるためサイズ判定を使えない
                    boolean requireLargeSize = !"Final Stage".equals(magmaStatus);

                    List<MagmaCube> cubes = magmaCubesInArena(client,
                            e -> !requireLargeSize || e.getSize() > MAGMA_MAX_NATURAL_SIZE);
                    if (cubes.isEmpty()) break;

                    // エリア内に本体は1体しかいないため、基準座標に最も近い1体だけを対象にする
                    Vec3 arenaCenter = Vec3.atCenterOf(ModConstants.MAGMA_BOSS_POS);
                    Entity target = null;
                    double nearest = Double.MAX_VALUE;
                    for (MagmaCube cube : cubes) {
                        double dist = cube.distanceToSqr(arenaCenter);
                        if (dist < nearest) {
                            nearest = dist;
                            target = cube;
                        }
                    }

                    if (glowBoss) highlightedEntities.add(target);
                    crimsonBossEntities.put(target, boss);
                    if (plateBoss) {
                        String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                        nameplateEntities.put(target, BossNameplateRenderer.buildLabel(label, boss.getHealth().get(), boss.enableNameplateHealth().get()));
                    }
                    bossFound[i] = true;
                    break;
                }
            }
        }

        // isDetected 状態を更新
        if (isCrimsonIsle) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                // Magma Boss はサイドバーにフェーズ行が出ていれば、エンティティを特定できていなくても存在する。
                // (Final Stage 以外はサイズで絞り込むため、条件次第でエンティティ側を取り逃すことがある。
                //  それを不在と扱うと Killed ラッチが誤って立ってしまう)
                boolean present = bossFound[i]
                        || ("Magma Boss".equals(boss.nameTag()) && GameState.MagmaBoss.spawnStatus != null);
                boss.setIsDetected().accept(present);

                // 「いない」と確定するまでの猶予を計測しつつ、最後に確定した状態をラッチする
                boolean detectionEnabled = boss.enableHighlight().get() || boss.enableNameplate().get();
                if (present) {
                    absenceSince.remove(boss.nameTag());
                    killedConfirmedAt.remove(boss.nameTag());
                    lastConfirmedSpawned.add(boss.nameTag());
                } else if (!detectionEnabled || !canObserveSpawnPoint(client, boss.nameTag())) {
                    // 判定できない状態。ラッチと Killed 確定時刻は範囲外の表示に使うため保持する
                    absenceSince.remove(boss.nameTag());
                } else {
                    absenceSince.putIfAbsent(boss.nameTag(), now);
                    // 「いない」と確定できたらラッチを解除し、リスポーン推定の起点を記録する
                    if (canConfirmAbsence(boss.nameTag())) {
                        lastConfirmedSpawned.remove(boss.nameTag());
                        // 確定した瞬間ではなく、不在を観測し始めた時刻を起点にする
                        killedConfirmedAt.putIfAbsent(boss.nameTag(), absenceSince.get(boss.nameTag()));
                    }
                }
            }
            for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                ASHFANG_FOLLOWERS.get(i).setIsDetected().accept(followerFound[i]);
            }
        }
    }

    // DragonStatusHud と同じ配色ルール(ネームプレートの名前部分に使う)
    private static String dragonColorCode(String type) {
        if (type == null) return "§d";
        return switch (type) {
            case "Protector" -> "§8";
            case "Old"       -> "§7";
            case "Unstable"  -> "§5";
            case "Young"     -> "§f";
            case "Strong"    -> "§c";
            case "Wise"      -> "§b";
            case "Superior"  -> "§e";
            default          -> "§d";
        };
    }

    // Wither Skeleton と Blaze が同一の Bladesoul を構成しているかを判定する。
    // 水平は重なっているが高さは数ブロックずれるため、XZとYで別々のしきい値を使う
    private static boolean isBladesoulPair(Entity skeleton, Entity blaze) {
        double dx = blaze.getX() - skeleton.getX();
        double dz = blaze.getZ() - skeleton.getZ();
        double dy = Math.abs(blaze.getY() - skeleton.getY());
        return dx * dx + dz * dz <= BLADESOUL_PAIR_XZ * BLADESOUL_PAIR_XZ && dy <= BLADESOUL_PAIR_Y;
    }

    // 読み込み済みのプレイヤーから、名前が一致するもの(=ボス本体)を探して登録する
    private static void detectPlayerBoss(Minecraft client, String bossName, String entityName, boolean[] bossFound) {
        for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
            CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
            if (!bossName.equals(boss.nameTag())) continue;

            boolean glowBoss = boss.enableHighlight().get();
            boolean plateBoss = boss.enableNameplate().get();
            if (!glowBoss && !plateBoss) return;

            for (Player player : client.level.players()) {
                if (player == client.player || !matchesEntityName(player, entityName)) continue;

                if (glowBoss) highlightedEntities.add(player);
                crimsonBossEntities.put(player, boss);
                if (plateBoss) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(player, BossNameplateRenderer.buildLabel(label, boss.getHealth().get(), boss.enableNameplateHealth().get()));
                }
                bossFound[i] = true;
                return;
            }
            return;
        }
    }

    // Hypixel側の名前は末尾に空白が入ることがあるため、trim + 大文字小文字無視 + 部分一致で照合する
    private static boolean matchesEntityName(Entity entity, String expected) {
        return ModConstants.containsIgnoreCase(entity.getName().getString().trim(), expected);
    }

    // プレイヤーエンティティ型ボスの照合名。該当しないボスは null
    private static String playerBossEntityName(String bossName) {
        return switch (bossName) {
            case "Barbarian Duke X" -> BARBARIAN_ENTITY_NAME;
            case "Mage Outlaw"      -> MAGE_OUTLAW_ENTITY_NAME;
            default                 -> null;
        };
    }

    private static Entity findVisualEntity(Minecraft client, Entity namedEntity, String bossName) {
        if (!(namedEntity instanceof ArmorStand)) return namedEntity;
        AABB box = namedEntity.getBoundingBox().inflate(8.0);

        // Magma Boss: MagmaCube のみを対象とし、ジャンプ中の位置ずれに対応するため広めに探索。
        // Kill the Magmas と Reforming の間はボス本体が存在せず、分裂した個体しかいない。
        // Reforming ではそれらがフィールド中央(ネームタグの近く)に集まり誤認するため、両フェーズともスキップする
        if ("Magma Boss".equals(bossName)) {
            String status = GameState.MagmaBoss.spawnStatus;
            if ("Kill the Magmas".equals(status) || "Reforming...".equals(status)) return null;
            AABB wideBox = namedEntity.getBoundingBox().inflate(20.0);
            return getClosestEntity(client.level.getEntitiesOfClass(MagmaCube.class, wideBox, e -> true), namedEntity);
        }

        // Barbarian Duke X と Mage Outlaw は Player エンティティ型のボス。
        // 実在の他プレイヤーと型で区別できないため名前で照合する。
        // それ以外のボス(Arachne等)では、近傍にMobが見つからない場合でも
        // Player を視覚エンティティとして誤マッチさせないよう、Player検索はPlayer型ボスのときのみ行う
        String playerBossName = playerBossEntityName(bossName);
        if (playerBossName != null) {
            Entity closest = getClosestEntity(
                    client.level.getEntitiesOfClass(Player.class, box,
                            e -> e != client.player && matchesEntityName(e, playerBossName)),
                    namedEntity);
            if (closest != null) return closest;
        } else {
            // Skeleton 系はボスの視覚エンティティとして扱わない
            // （Bladesoul の Wither Skeleton は別途明示的に追加されるため影響なし）
            Entity closest = getClosestEntity(
                    client.level.getEntitiesOfClass(Mob.class, box, e -> !(e instanceof AbstractSkeleton)),
                    namedEntity);
            if (closest != null) return closest;
        }

        return getClosestEntity(client.level.getEntitiesOfClass(ArmorStand.class, box,
                e -> e != namedEntity && e.getCustomName() == null), namedEntity);
    }


    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity e : entities) {
            double dist = e.distanceToSqr(center);
            if (dist < minDistance) {
                minDistance = dist;
                closest = e;
            }
        }
        return closest;
    }
}
