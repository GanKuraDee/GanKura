package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.MobVisual;
import com.deeply.gankura.data.MobVisual.CrimsonIsle;
import com.deeply.gankura.data.MobVisual.MoongladeMarsh;
import com.deeply.gankura.data.MobVisual.SafariCavern;
import com.deeply.gankura.data.MobVisual.SafariForest;
import com.deeply.gankura.data.MobVisual.SafariHaunted;
import com.deeply.gankura.data.MobVisual.SafariIcy;
import com.deeply.gankura.data.MobVisual.SpidersDen;
import com.deeply.gankura.data.MobVisual.TheEnd;
import com.deeply.gankura.data.MobVisual.TorrhusCanyon;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.ParchedEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.TadpoleEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;

public class EntityHighlightManager {

    // Magma Boss の戦闘エリア半径(ブロック)。ワールドテキストの基準座標 MAGMA_BOSS_POS を中心とする
    // Magma Glare のネームタグに出る名前
    private static final String MAGMA_GLARE_NAME = "Magma Glare";
    // Magma Glare のネームタグを探す半径(ブロック)。個体が密集するため狭めにとる
    private static final double GLARE_NAMETAG_RADIUS = 3.0;
    // 「現在HP/最大HP」の抽出。EntityHealthScanner と同じ形式
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");
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
    // Beeheemoth はミニボス級に大きく、当たり判定もそのぶん大きくなる。
    // バニラの成体ハチの高さは 0.6 なので、明らかに超えているものだけを Beeheemoth とみなす
    private static final float BEEHEEMOTH_MIN_HEIGHT = 1.2f;


    // Invisibug を探す半径(ブロック)。CRIT パーティクルは本体のすぐ近くに出る
    private static final double INVISIBUG_RADIUS = 5.0;
    // Torrhus Canyon のヘッド系モブは、ヘッドの scale と当たり判定の大きさが呼び名ごとに決まっている。
    // 実測値は Ant 0.40 / Queen Ant 0.60 / Water Snake 1.00 なので、その中間で切る
    private static final float CANYON_ANT_MAX_SCALE = 0.5f;
    private static final float CANYON_QUEEN_ANT_MAX_SCALE = 0.8f;
    // Moonglade Marsh のファントム3種は同じ Phantom 型だが、scale 属性で大きさが変えてあり
    // 当たり判定にもそれが出る。実測値は Phanpyre 0.36 / Phanflare 0.90(バニラ相当)。
    // Dreadwing はさらに大きいので、実測値の中間と、余裕を見た値で切る
    private static final float PHANFLARE_MIN_WIDTH = 0.63f;
    private static final float DREADWING_MIN_WIDTH = 1.5f;

    // ネームタグ経由でモブの一部と確定したエンティティ。
    // 型だけで判定するモブが、これらを別のモブとして二重に拾わないようにする
    private static final Set<Entity> nametagClaimedEntities = new HashSet<>();

    // シュルカー系(Hideonfloor / Hideonwall)は、動き出すと見た目のブロックが
    // 当たり判定のシルバーフィッシュを追いかける形になり、少し離れる。
    // 同じ個体とみなす距離はそのぶん広めに取る
    private static final double SHULKER_COMPANION_RADIUS = 4.0;

    // Critter Safari は4つのバイオームに分かれており、この座標を中心に4象限で区切られている。
    //   Cavern  … x-  z+      Forest … x+ z+
    //   Icy     … x-  z-      Haunted … x+ z-
    // エリア名はどこも "Safari" なので、バイオームの区別はこの座標から求める
    private static final double SAFARI_CENTER_X = -50.0;
    private static final double SAFARI_CENTER_Z = 0.0;

    // Display(アイテム/ブロック表示)の表示位置。
    // Display は原点がそのまま見た目の中心になるため、足元からの補正は要らない
    private static final double DISPLAY_ANCHOR = 0.0;
    // 当たり判定のモブから、見た目を担うヘッドを探す距離(ブロック)。
    // これが見つかる当たり判定は「別のモブの一部」なので、それ自体をモブとして扱わない
    private static final double HITBOX_HEAD_RADIUS = 1.5;
    // ネームタグは本体の真上に出るため、これ以上水平に離れたヘッドは別のモブとみなす(ブロック)
    private static final double NAMETAG_HORIZONTAL_LIMIT = 1.0;
    // ネームタグから本体を探す半径(ブロック)
    private static final double NAMETAG_SEARCH_RADIUS = 4.0;
    // skull を被せたアーマースタンドは、足元ではなく頭の位置に見た目がある。
    // 表示をそこへ合わせるための、足元からの高さ(ブロック)。
    // 通常サイズ(高さ 1.975)で頭の中心はおよそ 1.7。scale 属性を掛けて実際の高さにする
    private static final double HEAD_STAND_ANCHOR = 1.7;
    private static final double SMALL_HEAD_STAND_ANCHOR = 0.85;
    // ネームタグからヘッドを探す半径(ブロック)。
    // 連なる数や向きはモブによって違ううえ、ネームタグは一番上のヘッドよりさらに上に出る。
    // 端のヘッドを取りこぼすと表示が点滅するので、広めに取ったうえで
    // 下の HEAD_CHAIN_GAP による連結判定で絞り込む
    private static final double HEAD_CHAIN_SEARCH_RADIUS = 8.0;
    // ヘッドどうしがつながっているとみなす距離(ブロック)
    private static final double HEAD_CHAIN_GAP = 2.5;
    // ネットワークスレッドから溜まるパーティクル座標の上限。
    // 取りこぼしても本体は出し続けるので、次のパーティクルで拾える
    private static final int MAX_PENDING_CRIT_PARTICLES = 256;
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
    // エンティティ → 輪郭の色(RGB)。同じ型でもエリアや体色・変種で呼び名(=色)が変わるモブ用に、
    // 走査時に決めた色を mixin 側から引けるよう保持する
    public static final Map<Entity, Integer> customGlowColors = new HashMap<>();
    // ネームプレート表示対象 → 表示文字列。Glowingとは独立して有効化できるよう別管理としている
    public static final Map<Entity, String> nameplateEntities = new LinkedHashMap<>();
    // Tracer 対象 → 線の色(ARGB)。Highlight とは独立して切り替えられるよう、
    // highlightedEntities とは別に持つ
    public static final Map<Entity, Integer> tracerEntities = new LinkedHashMap<>();
    // モブの種類 → 現在 Tracer を出している個体。同じモブが複数いても線は1本に絞る
    private static final Map<Object, Entity> tracerNearest = new HashMap<>();
    // Invisibug 本体(透明なアーマースタンド)。一度見つけたら消えるまで保持する
    // 名前を確認できたハチ → その呼び名。Honeybuzz と Pollendart はネームタグでしか区別できないが、
    // ハチ本体より先にネームタグが届かなくなるため、一度判別できた個体は覚えておく。
    // 他のモブはネームタグが本体と同じくらい届くので、この仕組みは使わない
    private static final Map<Entity, MobVisual> canyonBeeNames = new LinkedHashMap<>();
    // 描画時だけ marker として扱うアーマースタンド。
    // marker のアーマースタンドは本体モデルが描画されず装備だけが描かれるので、
    // これを利用して glow の輪郭をヘッドだけに絞る
    public static final Set<Entity> headOnlyGlowEntities = new HashSet<>();

    // 見た目のエンティティ → ネームプレートと Tracer を合わせる高さ(足元から)。
    // Display のように当たり判定を持たないものは、対になるエンティティから実測して入れる
    public static final Map<Entity, Double> renderAnchors = new HashMap<>();

    // 型では判別できない「見た目」側のエンティティ(ヘッドのアーマースタンド、NPC など)。
    // 型による一括削除ができないため、ここに控えて毎 tick 作り直す。
    // これを忘れると、設定で対象から外した後もハイライトが残り続けてしまう
    private static final Set<Entity> rebuiltVisuals = new HashSet<>();
    private static final Set<Entity> invisibugEntities = new HashSet<>();
    // CRIT パーティクルの座標。ネットワークスレッドから積まれるため並行キューを使う
    private static final Queue<double[]> pendingCritParticles = new ConcurrentLinkedQueue<>();

    // パーティクルパケット(ネットワークスレッド)から呼ばれる。座標を控えるだけに留める
    public static void onCritParticle(double x, double y, double z) {
        if (pendingCritParticles.size() >= MAX_PENDING_CRIT_PARTICLES) return;
        pendingCritParticles.add(new double[]{x, y, z});
    }
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

    // シュルカー(Hideon系)の4種。探索が必要かどうかの判定にまとめて使う
    public static final List<MobVisual> SHULKER_TARGETS = List.of(
            MoongladeMarsh.HIDEONLEAF, TorrhusCanyon.HIDEONSUN,
            SafariForest.HIDEONFLOOR, SafariHaunted.HIDEONWALL);

    // Moonglade Marsh / Torrhus Canyon の固有モブ。探索が必要かどうかの判定にまとめて使う
    public static final List<MobVisual> AREA_ANIMAL_TARGETS = List.of(
            MoongladeMarsh.CORALOT, MoongladeMarsh.MOSSYBIT,
            MoongladeMarsh.COD, MoongladeMarsh.SALMON, MoongladeMarsh.JOYDIVE,
            MoongladeMarsh.LUMISQUID, MoongladeMarsh.SHELLWISE, MoongladeMarsh.SPIKE,
            MoongladeMarsh.BIRRIES, MoongladeMarsh.BAMBULEAF, MoongladeMarsh.MOCHIBEAR,
            MoongladeMarsh.PHANPYRE, MoongladeMarsh.PHANFLARE, MoongladeMarsh.DREADWING,
            MoongladeMarsh.HEWVER, MoongladeMarsh.HONEYHOG, MoongladeMarsh.HONEYMITE,
            MoongladeMarsh.MURKBAT, MoongladeMarsh.TIDETOT, MoongladeMarsh.CHILL,
            MoongladeMarsh.AZURE, MoongladeMarsh.VERDANT,
            TorrhusCanyon.BLUE_JAY, TorrhusCanyon.DUSTYBIT,
            TorrhusCanyon.SEPIALOT, TorrhusCanyon.GOLDOLOT, TorrhusCanyon.PANGOLIN,
            TorrhusCanyon.BUNBUN, TorrhusCanyon.DRYBARK, TorrhusCanyon.FIREFOX,
            TorrhusCanyon.GROUNDHOG, TorrhusCanyon.HIVETHIEF, TorrhusCanyon.MOUNTAIN_GOAT,
            TorrhusCanyon.PUCK, TorrhusCanyon.BEEHEEMOTH,
            TorrhusCanyon.EMBER, TorrhusCanyon.SOLAR, TorrhusCanyon.TIMIL,
            TorrhusCanyon.PARCHED);

    // Critter Safari の、エンティティ型で判別できるモブ。
    // Wumpa / Doomspiral / Hideon 系はネームプレートの中身が違うので専用の処理を持っており、ここには含めない
    public static final List<MobVisual> SAFARI_TYPE_TARGETS = List.of(
            SafariCavern.ROCKMITE, SafariCavern.SCRAPPY, SafariCavern.SNOOZLE, SafariCavern.GEMZIE,
            SafariForest.FOXTROT, SafariForest.BLUEBIRD, SafariForest.HONEYBUG, SafariForest.TREEFROG,
            SafariForest.WOODCHUCKER, SafariForest.FLUFFLING, SafariForest.PARAKEET, SafariForest.MACAW,
            SafariHaunted.AREITA, SafariHaunted.BLOODBAT, SafariHaunted.LITTERBUG, SafariHaunted.SOLSNATCHER,
            SafariIcy.BILLYGOAT, SafariIcy.NOZZLENOSE, SafariIcy.POLARIS, SafariIcy.SHUDDERSQUID,
            SafariIcy.STRONGARM, SafariCavern.CAVERNFISH, SafariIcy.TEPID,
            SafariHaunted.DUPLICO, SafariCavern.CHUCKWALLA,
            SafariCavern.FLITTER, SafariCavern.DRIFTLING,
            SafariHaunted.GAZER, SafariHaunted.GIMMIEGOLD,
            SafariIcy.MANTIS_SHRIMP, SafariIcy.TROODON, SafariCavern.SHYWORM);

    // 見た目がブロック表示(Display)のモブ。透明な当たり判定から見つけて差し替える
    public static final List<MobVisual> SAFARI_DISPLAY_TARGETS = List.of(
            SafariCavern.CHUCKWALLA, SafariCavern.FLITTER,
            SafariHaunted.DUPLICO, SafariHaunted.GIMMIEGOLD,
            SafariIcy.MANTIS_SHRIMP, SafariIcy.TROODON, SafariForest.HIDEONFLOOR);

    // Critter Safari の、独自の見た目でエンティティ型からは判別できないモブ。ネームタグで判定する
    public static final List<MobVisual> SAFARI_NAMED_TARGETS = List.of(
            SafariHaunted.HIDEYHO);

    // Moonglade Marsh の、ネームタグでしか判別できないモブ。
    // Stag Beetle と Woodlouse は同じ見た目の作りで、型では絞り込めない
    public static final List<MobVisual> MARSH_NAMED_TARGETS = List.of(
            MoongladeMarsh.STAG_BEETLE, MoongladeMarsh.WOODLOUSE);

    // ヘッドが複数積み重なって1体を成すモブ。まとめて1体として扱う
    public static final List<MobVisual> HEAD_CHAIN_TARGETS = List.of(
            TorrhusCanyon.SNEAKY_TIKI, TorrhusCanyon.SHRIEKY_TIKI, TorrhusCanyon.CHEEKY_TIKI);

    // Torrhus Canyon の、ネームタグでしか判別できないモブ。
    // Grizzly Bear はプレイヤー型。Tiki 系は節ごとに固有のプロフィールを持つ skull を使っているが、
    // 種類ごとの skull がすべて出そろっているか確かめきれないため、名前で判定する
    public static final List<MobVisual> CANYON_NAMED_TARGETS = List.of(
            TorrhusCanyon.GRIZZLY_BEAR,
            TorrhusCanyon.SNEAKY_TIKI, TorrhusCanyon.SHRIEKY_TIKI, TorrhusCanyon.CHEEKY_TIKI);

    // Torrhus Canyon の、marker のアーマースタンドに素の skull を被せて作られたモブ。
    // 節ごとに透明なスライムが当たり判定として付いていて、ヘッドの scale で呼び名が分かれる
    public static final List<MobVisual> CANYON_HEAD_TARGETS = List.of(
            TorrhusCanyon.ANT, TorrhusCanyon.QUEEN_ANT, TorrhusCanyon.WATER_SNAKE);

    // ネームタグは "[Lv58] Parched 25,000/25,000❤" のような形。名前部分を単語単位で照合する。
    // 単純な部分一致だと「Ant」が「Giant Isopod」に引っかかってしまうため、前後が英字でないことを条件にする
    private static final Map<MobVisual, Pattern> CANYON_NAMED_PATTERNS = new LinkedHashMap<>();
    static {
        for (MobVisual target : CANYON_NAMED_TARGETS) {
            CANYON_NAMED_PATTERNS.put(target,
                    Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(target.plainLabel()) + "(?![A-Za-z])"));
        }
    }

    // Torrhus Canyon のハチのうち Honeybuzz と Pollendart。
    // 同じ大きさの Bee 型で見分けが付かないため、ネームタグの名前で振り分ける
    // (Beeheemoth はミニボス級に大きいので大きさで判別でき、こちらには含めない)
    public static final List<MobVisual> CANYON_BEE_TARGETS = List.of(
            TorrhusCanyon.HONEYBUZZ, TorrhusCanyon.POLLENDART);

    public static final List<CrimsonBossEntry> ASHFANG_FOLLOWERS = List.of(
        new CrimsonBossEntry("Ashfang Follower",
            0x555555, 0xFF555555,
            CrimsonIsle.ASHFANG_FOLLOWER::highlight,
            CrimsonIsle.ASHFANG_FOLLOWER::tracer,
            CrimsonIsle.ASHFANG_FOLLOWER::nameplate,
            () -> GameState.AshfangFollower.isDetected,
            d -> GameState.AshfangFollower.isDetected = d,
            () -> GameState.AshfangFollower.health,
            h -> GameState.AshfangFollower.health = h),
        new CrimsonBossEntry("Ashfang Acolyte",
            0x5555FF, 0xFF5555FF,
            CrimsonIsle.ASHFANG_ACOLYTE::highlight,
            CrimsonIsle.ASHFANG_ACOLYTE::tracer,
            CrimsonIsle.ASHFANG_ACOLYTE::nameplate,
            () -> GameState.AshfangAcolyte.isDetected,
            d -> GameState.AshfangAcolyte.isDetected = d,
            () -> GameState.AshfangAcolyte.health,
            h -> GameState.AshfangAcolyte.health = h),
        new CrimsonBossEntry("Ashfang Underling",
            0xFF5555, 0xFFFF5555,
            CrimsonIsle.ASHFANG_UNDERLING::highlight,
            CrimsonIsle.ASHFANG_UNDERLING::tracer,
            CrimsonIsle.ASHFANG_UNDERLING::nameplate,
            () -> GameState.AshfangUnderling.isDetected,
            d -> GameState.AshfangUnderling.isDetected = d,
            () -> GameState.AshfangUnderling.health,
            h -> GameState.AshfangUnderling.health = h)
    );

    public static final List<CrimsonBossEntry> CRIMSON_BOSSES = List.of(
        new CrimsonBossEntry("Bladesoul",
            0x555555, 0xFF555555,
            CrimsonIsle.BLADESOUL::highlight,
            CrimsonIsle.BLADESOUL::tracer,
            CrimsonIsle.BLADESOUL::nameplate,
            () -> GameState.Bladesoul.isDetected,
            d -> GameState.Bladesoul.isDetected = d,
            () -> GameState.Bladesoul.health,
            h -> GameState.Bladesoul.health = h),
        new CrimsonBossEntry("Barbarian Duke X",
            0xFF5555, 0xFFFF5555,
            CrimsonIsle.BARBARIAN_DUKE_X::highlight,
            CrimsonIsle.BARBARIAN_DUKE_X::tracer,
            CrimsonIsle.BARBARIAN_DUKE_X::nameplate,
            () -> GameState.BarbarianDukeX.isDetected,
            d -> GameState.BarbarianDukeX.isDetected = d,
            () -> GameState.BarbarianDukeX.health,
            h -> GameState.BarbarianDukeX.health = h),
        new CrimsonBossEntry("Mage Outlaw",
            0xAA00AA, 0xFFAA00AA,
            CrimsonIsle.MAGE_OUTLAW::highlight,
            CrimsonIsle.MAGE_OUTLAW::tracer,
            CrimsonIsle.MAGE_OUTLAW::nameplate,
            () -> GameState.MageOutlaw.isDetected,
            d -> GameState.MageOutlaw.isDetected = d,
            () -> GameState.MageOutlaw.health,
            h -> GameState.MageOutlaw.health = h),
        new CrimsonBossEntry("Ashfang",
            0xAAAAAA, 0xFFAAAAAA,
            CrimsonIsle.ASHFANG::highlight,
            CrimsonIsle.ASHFANG::tracer,
            CrimsonIsle.ASHFANG::nameplate,
            () -> GameState.Ashfang.isDetected,
            d -> GameState.Ashfang.isDetected = d,
            () -> GameState.Ashfang.health,
            h -> GameState.Ashfang.health = h),
        new CrimsonBossEntry("Magma Boss",
            0xFFAA00, 0xFFFFAA00,
            CrimsonIsle.MAGMA_BOSS::highlight,
            CrimsonIsle.MAGMA_BOSS::tracer,
            CrimsonIsle.MAGMA_BOSS::nameplate,
            () -> GameState.MagmaBoss.isDetected,
            d -> GameState.MagmaBoss.isDetected = d,
            () -> GameState.MagmaBoss.health,
            h -> GameState.MagmaBoss.health = h)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EntityHighlightManager::updateHighlights);
    }

    // 不在計測とラッチをすべて破棄し、全ボスを Unknown に戻す。
    // これらは GameState の外に持っているため resetAll() では消えない。
    // 別サーバー(=エリアの入り直し)では前のエリアのスキャン結果は無効なので、
    // ワールド離脱時に加えてサーバー参加時にも呼ぶ
    public static void resetCrimsonBossTracking() {
        absenceSince.clear();
        lastConfirmedSpawned.clear();
        killedConfirmedAt.clear();
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
    // ただしチャンクが届いた直後はモブがまだ来ていない、検出が1tickだけ外れる等のちらつきがあるため、
    // その状態が ABSENCE_CONFIRM_DELAY_MS 続いた場合にのみ確定する
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

    // Magma Boss の戦闘エリア(基準座標を中心とした球)の内側にいる MagmaCubeEntity を返す。
    // 立方体で絞ってから球で判定し、角のぶんを取りこぼさないようにしている
    private static List<MagmaCubeEntity> magmaCubesInArena(MinecraftClient client, Predicate<MagmaCubeEntity> filter) {
        Vec3d center = Vec3d.ofCenter(ModConstants.MAGMA_BOSS_POS);
        Box box = new Box(
                center.x - MAGMA_ARENA_RADIUS, center.y - MAGMA_ARENA_RADIUS, center.z - MAGMA_ARENA_RADIUS,
                center.x + MAGMA_ARENA_RADIUS, center.y + MAGMA_ARENA_RADIUS, center.z + MAGMA_ARENA_RADIUS);
        return client.world.getEntitiesByClass(MagmaCubeEntity.class, box,
                e -> e.squaredDistanceTo(center) <= MAGMA_ARENA_RADIUS * MAGMA_ARENA_RADIUS && filter.test(e));
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
    // 範囲判定の isRegionLoaded() は内部で isChunkLoaded(int,int) を呼ぶが、
    // ClientWorld はこれを常に true で返すため使えない。
    // チャンクマネージャを経由する isPosLoaded(BlockPos) を各チャンクに対して個別に確認する
    private static boolean canObserveSpawnPoint(MinecraftClient client, String bossName) {
        BlockPos spawnPos = spawnPosOf(bossName);
        if (client.world == null || spawnPos == null) return false;
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
                if (!client.world.isPosLoaded(new BlockPos((chunkX << 4), spawnPos.getY(), (chunkZ << 4)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void updateHighlights(MinecraftClient client) {
        highlightedEntities.clear();
        crimsonBossEntities.clear();
        magmaGlareEntities.clear();
        arachneEntities.clear();
        arachneBroodEntities.clear();
        customGlowColors.clear();
        nameplateEntities.clear();
        // 覚えているハチのうち、消えた個体だけ捨てる
        canyonBeeNames.entrySet().removeIf(e -> e.getKey().isRemoved());
        // 型で消せない見た目エンティティは毎 tick 作り直す
        highlightedEntities.removeAll(rebuiltVisuals);
        rebuiltVisuals.clear();
        nametagClaimedEntities.clear();
        renderAnchors.clear();
        headOnlyGlowEntities.clear();
        tracerEntities.clear();
        tracerNearest.clear();
        ashfangTracerTarget = null;

        if (client.world == null || client.player == null) {
            canyonBeeNames.clear();
            invisibugEntities.clear();
            pendingCritParticles.clear();
            // ワールド遷移をまたいで古い計測・ラッチを持ち越さない
            resetCrimsonBossTracking();
            return;
        }

        ModConfig.TheEndCategory theEnd = ModConfig.INSTANCE.theEnd;
        ModConfig.SpidersDenCategory spidersDen = ModConfig.INSTANCE.spidersDen;

        // 各ボスについて「その場に居るか(Present)」「Glowing対象か(glow)」「探索が必要か(scan)」を分けて持つ。
        // ネームプレートのみ有効な場合も探索は必要だが、Glowing対象には加えない
        boolean isTheEnd = GameState.Server.isTheEnd();
        boolean golemPresent = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);
        boolean glowGolem = golemPresent && TheEnd.GOLEM.highlight();
        boolean scanGolem = golemPresent && TheEnd.GOLEM.anyEnabled();
        boolean isSpidersDen = GameState.Server.isSpidersDen();
        boolean broodmotherPresent = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);
        boolean glowBroodmother = broodmotherPresent && SpidersDen.BROODMOTHER.highlight();
        boolean scanBroodmother = broodmotherPresent && SpidersDen.BROODMOTHER.anyEnabled();
        // Sanctuary内かつ蜘蛛の巣を検知できている間のみスキャンする(スポーン前・撃破後は存在しないため)
        boolean arachnePresent = GameState.Arachne.inSanctuary && GameState.Arachne.cobwebDetected;
        boolean glowArachne = arachnePresent && SpidersDen.ARACHNE.highlight();
        boolean scanArachne = arachnePresent && (SpidersDen.ARACHNE.anyEnabled() || SpidersDen.ARACHNE_BROOD.anyEnabled());
        boolean dragonPresent = isTheEnd && "Hatched".equals(GameState.Dragon.eggState);
        boolean glowDragon = dragonPresent && TheEnd.DRAGON.highlight();
        boolean scanDragon = dragonPresent && TheEnd.DRAGON.anyEnabled();
        boolean isCrimsonIsle = GameState.Server.isCrimsonIsle();
        // ボスの存在判定はワールドテキストや Status HUD のスポーン表示にも使うため、
        // Mob Visuals で対象から外していても Crimson Isle にいる間は常に走らせる
        boolean scanCrimsonBosses = isCrimsonIsle;
        boolean scanMagmaGlare = isCrimsonIsle
                && "Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)
                && CrimsonIsle.MAGMA_GLARE.anyEnabled();
        boolean scanAshfangFollowers = isCrimsonIsle && ASHFANG_FOLLOWERS.stream().anyMatch(f -> f.enableHighlight().get() || f.enableTracer().get() || f.enableNameplate().get());

        // Wumpa: Safari に出現するラヴェジャーは Wumpa しかいないため、型だけで本体と判定できる
        ModConfig.ForagingCategory foraging = ModConfig.INSTANCE.foraging;
        boolean isSafari = GameState.Server.isSafari();
        boolean glowWumpa = isSafari && SafariIcy.WUMPA.highlight();
        boolean scanWumpa = isSafari && SafariIcy.WUMPA.anyEnabled();
        // Doomspiral: Safari に出現するウォーデンは Doomspiral しかいないため、型だけで本体と判定できる
        boolean glowDoomspiral = isSafari && SafariHaunted.DOOMSPIRAL.highlight();
        boolean scanDoomspiral = isSafari && SafariHaunted.DOOMSPIRAL.anyEnabled();

        boolean scanSafariTypes = isSafari && SAFARI_TYPE_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanSafariNamed = isSafari && SAFARI_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled);

        // Shulker: Hideon 系が敵として出現する3エリアでのみ探索する
        boolean isShulkerArea = GameState.Server.isMoongladeMarsh()
                || GameState.Server.isTorrhusCanyon()
                || isSafari;
        boolean scanShulker = isShulkerArea && SHULKER_TARGETS.stream().anyMatch(MobVisual::anyEnabled);

        // Moonglade Marsh / Torrhus Canyon では、該当するエンティティ型がこれらの固有モブしか
        // 存在しないため、ネームタグを読まずに型(一部は変種)だけで判定できる
        boolean isAreaAnimalArea = GameState.Server.isMoongladeMarsh() || GameState.Server.isTorrhusCanyon();
        boolean scanAreaAnimals = isAreaAnimalArea && AREA_ANIMAL_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanCanyonBees = GameState.Server.isTorrhusCanyon()
                && CANYON_BEE_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        // Torrhus Canyon のヘッド系モブは marker のアーマースタンドと透明なスライムの組で作られていて、
        // 装飾として置かれたアーマースタンドとは作りが違うため、ネームタグを読まずに判定できる
        boolean scanCanyonHeads = GameState.Server.isTorrhusCanyon()
                && CANYON_HEAD_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanMarshNamed = GameState.Server.isMoongladeMarsh()
                && MARSH_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        // Torrhus Canyon には同じ型のモブが多数いてエンティティ型では絞れないため、
        // これらはネームタグで判定する
        boolean scanCanyonNamed = GameState.Server.isTorrhusCanyon()
                && CANYON_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled);

        // Invisibug は透明なので型では見つけられない。CRIT パーティクルの近くにいる
        // 「名前も装備も持たない素のアーマースタンド」を本体とみなす(SkyHanni と同じ手法)
        boolean scanInvisibug = GameState.Server.isMoongladeMarsh() && MoongladeMarsh.INVISIBUG.anyEnabled();
        if (!scanInvisibug) {
            invisibugEntities.clear();
            pendingCritParticles.clear();
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !scanDragon && !scanCrimsonBosses && !scanMagmaGlare && !scanAshfangFollowers && !scanWumpa && !scanDoomspiral && !scanShulker && !scanAreaAnimals && !scanCanyonBees && !scanInvisibug && !scanCanyonHeads && !scanCanyonNamed && !scanMarshNamed && !scanSafariTypes && !scanSafariNamed) {
            if (!isCrimsonIsle) {
                for (CrimsonBossEntry boss : CRIMSON_BOSSES) boss.setIsDetected().accept(false);
            }
            return;
        }

        boolean[] bossFound = new boolean[CRIMSON_BOSSES.size()];
        boolean[] followerFound = new boolean[ASHFANG_FOLLOWERS.size()];

        // Ashfang: 本体は2体のBlazeで構成される。ネームタグに頼らず、スポーン地点周辺のBlazeで判定する。
        // Follower/Acolyte/Underling も同じBlazeなので、本体を先に確定させてから
        // minion 側がそれを掴まないようにする(逆順だと本体に minion のネームプレートが付く)
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                if (!"Ashfang".equals(boss.nameTag())) continue;

                boolean glowBoss = boss.enableHighlight().get();
                boolean plateBoss = boss.enableNameplate().get();

                Vec3d center = Vec3d.ofCenter(ModConstants.ASHFANG_POS);
                Box area = new Box(
                        center.x - ASHFANG_AREA_XZ, center.y - CRIMSON_AREA_Y, center.z - ASHFANG_AREA_XZ,
                        center.x + ASHFANG_AREA_XZ, center.y + CRIMSON_AREA_Y, center.z + ASHFANG_AREA_XZ);
                List<BlazeEntity> blazes = new ArrayList<>(client.world.getEntitiesByClass(BlazeEntity.class, area,
                        e -> !crimsonBossEntities.containsKey(e)));
                if (blazes.size() < ASHFANG_BLAZE_COUNT) break;
                blazes.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)));

                List<BlazeEntity> body = blazes.subList(0, ASHFANG_BLAZE_COUNT);
                // 本体は縦に連なった2体。ネームプレートと Tracer は上側の1体だけに出す
                Entity plateTarget = body.get(0);
                for (BlazeEntity blaze : body) {
                    if (glowBoss) highlightedEntities.add(blaze);
                    crimsonBossEntities.put(blaze, boss);
                    if (blaze.getY() > plateTarget.getY()) plateTarget = blaze;
                }


                ashfangTracerTarget = plateTarget;
                registerTracer(plateTarget, boss);

                if (plateBoss) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(plateTarget, BossNameplateRenderer.buildLabel(label, boss.getHealth().get()));
                }
                bossFound[i] = true;
                break;
            }
        }


        for (Entity entity : client.world.getEntities()) {
            Text customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanBroodmother && ModConstants.containsIgnoreCase(nameStr, "Broodmother")) {
                Box box = entity.getBoundingBox().expand(8.0);
                Entity s = getClosestEntity(client.world.getEntitiesByClass(SpiderEntity.class, box, e -> true), entity);
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                Entity broodmotherVisual = s != null ? s : entity;
                if (s != null) {
                    if (glowBroodmother) highlightedEntities.add(s);
                }
                registerTracer(broodmotherVisual, SpidersDen.BROODMOTHER);
                if (SpidersDen.BROODMOTHER.nameplate()) {
                    nameplateEntities.put(broodmotherVisual, BossNameplateRenderer.buildLabel("§c§lBroodmother", GameState.Broodmother.health));
                }
            }

            if (scanArachne && ModConstants.isArachneBossName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne");
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                Entity arachneVisual = visualTarget != null ? visualTarget : entity;
                if (visualTarget != null) {
                    if (glowArachne) highlightedEntities.add(visualTarget);
                    arachneEntities.add(visualTarget);
                }
                registerTracer(arachneVisual, SpidersDen.ARACHNE);
                if (SpidersDen.ARACHNE.nameplate()) {
                    nameplateEntities.put(arachneVisual, BossNameplateRenderer.buildLabel("§5§lArachne", GameState.Arachne.health));
                }
            }

            if (scanArachne && ModConstants.isArachneBroodName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne's Brood");
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                Entity broodVisual = visualTarget != null ? visualTarget : entity;
                if (visualTarget != null) {
                    if (SpidersDen.ARACHNE_BROOD.highlight()) highlightedEntities.add(visualTarget);
                    arachneBroodEntities.add(visualTarget);
                }
                registerTracer(broodVisual, SpidersDen.ARACHNE_BROOD);
                if (SpidersDen.ARACHNE_BROOD.nameplate()) {
                    nameplateEntities.put(broodVisual, BossNameplateRenderer.buildLabel("§d§lArachne's Brood", null));
                }
            }

            if (scanDragon && ModConstants.containsIgnoreCase(nameStr, "Dragon")) {
                Box box = entity.getBoundingBox().expand(20.0);
                Entity d = getClosestEntity(client.world.getEntitiesByClass(EnderDragonEntity.class, box, e -> true), entity);
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                if (d != null) {
                    if (glowDragon) highlightedEntities.add(d);
                }
                registerTracer(d != null ? d : entity, TheEnd.DRAGON, dragonTracerColor());
            }

            // Critter Safari のネームタグ判定モブ
            if (scanSafariNamed) {
                for (MobVisual target : SAFARI_NAMED_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    // 自分がいるバイオームのモブだけを対象にする
                    if (!inSafariBiome(client.player, target)) continue;
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    // Hideyho は NPC(プレイヤー型)なので、アーマースタンドではなくプレイヤーを探す
                    Entity visualTarget = target == SafariHaunted.HIDEYHO
                            ? nearestPlayerNear(client, entity)
                            : nametagVisual(client, entity);
                    // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                    // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                    Entity visual = visualTarget != null ? visualTarget : entity;
                    if (visualTarget != null) {
                        // 当たり判定を借りているだけのモブもいるので、型判定側で拾い直さないよう控える
                        nametagClaimedEntities.add(visualTarget);
                        if (target.highlight()) registerHighlight(visualTarget, target);
                    }
                    registerTracer(visual, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(visual, BossNameplateRenderer.buildLabel(label, null));
                    }
                    break;
                }
            }

            // Moonglade Marsh のネームタグ判定モブ
            if (scanMarshNamed) {
                for (MobVisual target : MARSH_NAMED_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    Entity visualTarget = nametagVisual(client, entity);
                    // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                    // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                    Entity visual = visualTarget != null ? visualTarget : entity;
                    if (visualTarget != null) {
                        if (target.highlight()) registerHighlight(visualTarget, target);
                    }
                    registerTracer(visual, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(visual, BossNameplateRenderer.buildLabel(label, null));
                    }
                    break;
                }
            }

            // Torrhus Canyon のネームタグ判定モブ。本体の探し方だけが種類ごとに違う
            if (scanCanyonNamed) {
                for (MobVisual target : CANYON_NAMED_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    if (!CANYON_NAMED_PATTERNS.get(target).matcher(nameStr).find()) continue;

                    // Tiki 系はヘッドが縦に積み重なって1体を成す。連なったヘッドをまとめて扱い、
                    // ネームプレートは1つだけ出す
                    if (HEAD_CHAIN_TARGETS.contains(target)) {
                        applyHeadChain(client, entity, target, HEAD_CHAIN_SEARCH_RADIUS);
                        break;
                    }

                    Entity visualTarget = canyonNamedVisual(client, entity, target);
                    // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                    // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                    Entity visual = visualTarget != null ? visualTarget : entity;
                    if (visualTarget != null) {
                        if (target.highlight()) registerHighlight(visualTarget, target);
                    }
                    registerTracer(visual, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(visual, BossNameplateRenderer.buildLabel(label, null));
                    }
                    break;
                }
            }

            // Honeybuzz と Pollendart は同じ Bee 型なので、ネームタグの名前で振り分ける。
            // ネームタグは本体とは別のエンティティなので、近くのハチを本体とみなす。
            // Beeheemoth を掴まないよう、大きい個体は候補から外す
            if (scanCanyonBees) {
                for (MobVisual target : CANYON_BEE_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    // ここでは呼び名を覚えるだけにして、表示は覚えた一覧をまとめて処理する。
                    // ネームタグが届かなくなった後も、覚えている限り表示を続けられる
                    Entity bee = getClosestEntity(client.world.getEntitiesByClass(BeeEntity.class, entity.getBoundingBox().expand(4.0), e -> !isBeeheemoth(e)), entity);
                    if (bee != null) {
                        canyonBeeNames.put(bee, target);
                    } else {
                        // 本体がまだ届いていない場合は、ネームタグ自体を対象にして
                        // 座標さえあれば描ける Tracer とネームプレートだけ出す
                        registerTracer(entity, target);
                        if (target.nameplate()) {
                            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                            nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                        }
                    }
                    break;
                }
            }

            if (scanCrimsonBosses) {
                for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                    CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                    boolean glowBoss = boss.enableHighlight().get();
                    boolean plateBoss = boss.enableNameplate().get();
                    // Bladesoul / Ashfang / Magma Boss はネームタグではなくエンティティの構成で判定する(後述の専用ブロック)
                    if ("Bladesoul".equals(boss.nameTag())
                            || "Ashfang".equals(boss.nameTag())
                            || "Magma Boss".equals(boss.nameTag())) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, boss.nameTag());
                        // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                        // ネームタグ自体を対象にして、Tracer とネームプレートだけでも出す
                        Entity bossVisual = visualTarget != null ? visualTarget : entity;
                        if (visualTarget != null) {
                            if (glowBoss) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, boss);
                        }
                        registerTracer(bossVisual, boss);
                        if (plateBoss) {
                            String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                            nameplateEntities.put(bossVisual, BossNameplateRenderer.buildLabel(label, boss.getHealth().get()));
                        }

                        bossFound[i] = true;
                    }
                }
            }

            if (scanAshfangFollowers) {
                for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                    CrimsonBossEntry follower = ASHFANG_FOLLOWERS.get(i);
                    if (ModConstants.containsIgnoreCase(nameStr, follower.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, follower.nameTag());
                        // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                        // ネームタグ自体を対象にして、Tracer とネームプレートだけでも出す
                        Entity followerVisual = visualTarget != null ? visualTarget : entity;
                        if (visualTarget != null) {
                            if (follower.enableHighlight().get()) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, follower);
                        }
                        registerTracer(followerVisual, follower);
                        if (follower.enableNameplate().get()) {
                            String label = BossNameplateRenderer.colorCode(follower.tracerColorARGB()) + "§l" + follower.nameTag();
                            // minion は同じ種類が同時に複数湧く。共有の HP を使うと別個体の値が出てしまうので、
                            // その個体のネームタグから直接読む
                            nameplateEntities.put(followerVisual, BossNameplateRenderer.buildLabel(label, healthFromNameTag(nameStr)));
                        }
                        // ネームタグが見つかっている時点で存在は確定しているので、本体の有無に関わらず立てる
                        followerFound[i] = true;
                    }
                }
            }
        }

        // Golem(Endstone Protector): The End に存在する IronGolemEntity はこのボスしかいないため、
        // Stage 5 の間はネームタグを読めなくても IronGolemEntity 自体を本体とみなす。
        // ネームプレートは1体だけ出したいので、最初に見つかった個体を対象にする
        if (scanGolem) {
            Entity plateGolem = null;
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof IronGolemEntity)) continue;
                if (glowGolem) highlightedEntities.add(entity);
                registerTracer(entity, TheEnd.GOLEM);
                if (plateGolem == null) plateGolem = entity;
            }
            if (TheEnd.GOLEM.nameplate() && plateGolem != null) {
                nameplateEntities.put(plateGolem, BossNameplateRenderer.buildLabel("§6§lGolem", GameState.Golem.health));
            }
        }

        // Wumpa: Safari に出現するラヴェジャーは Wumpa のみなので、ネームタグを読まずに型だけで判定する。
        // 同時に複数体が湧きうるため、Golem と違い見つかった全個体にネームプレートを付ける
        if (scanWumpa) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof RavagerEntity)) continue;
                if (glowWumpa) highlightedEntities.add(entity);
                registerTracer(entity, SafariIcy.WUMPA);
                if (SafariIcy.WUMPA.nameplate()) {
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel("§b§lWumpa",
                            capsuleLabel(GameState.CritterSafari.wumpaCapsuleHits)));
                }
            }
        }

        // Doomspiral: Wumpa と同じく、Safari のウォーデンは Doomspiral のみなので型だけで判定する
        if (scanDoomspiral) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof WardenEntity)) continue;
                if (glowDoomspiral) highlightedEntities.add(entity);
                registerTracer(entity, SafariHaunted.DOOMSPIRAL);
                if (SafariHaunted.DOOMSPIRAL.nameplate()) {
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel("§5§lDoomspiral",
                            capsuleLabel(GameState.Doomspiral.capsuleHits)));
                }
            }
        }

        // Shulker: ネームタグを読まず、エリアと体色から Hideon 系の呼び名と色を決める
        if (scanShulker) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof ShulkerEntity shulker)) continue;
                MobVisual target = shulkerTarget(shulker);
                if (target == null) continue;
                // Safari の Hideonfloor / Hideonwall はバイオームが決まっている。
                // 自分と対象の両方が該当バイオームにいることを確かめる
                if (!inSafariBiome(client.player, target)) continue;
                if (!inSafariBiome(entity, target)) continue;

                if (target.highlight()) {
                    highlightedEntities.add(entity);
                    // 輪郭の色は呼び名ごとに変わるため、mixin から引けるよう控えておく
                    customGlowColors.put(entity, target.glowColorRGB());
                }
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Coralot / Mossybit / Blue Jay / Dustybit / Sepialot / Goldolot:
        // エリアとエンティティ型(ウーパールーパーのみ変種も)から呼び名と色を決める
        if (scanAreaAnimals) {
            for (Entity entity : client.world.getEntities()) {
                MobVisual target = areaAnimalTarget(entity);
                if (target == null) continue;

                if (target.highlight()) {
                    highlightedEntities.add(entity);
                    // 同じ型でも呼び名ごとに色が変わるため、mixin から引けるよう控えておく
                    customGlowColors.put(entity, target.glowColorRGB());
                }
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Honeybuzz / Pollendart: 一度名前を確認できたハチは、
        // ネームタグが届かなくなっても覚えている呼び名で表示を続ける
        if (scanCanyonBees) {
            for (Map.Entry<Entity, MobVisual> entry : canyonBeeNames.entrySet()) {
                Entity bee = entry.getKey();
                MobVisual target = entry.getValue();

                if (target.highlight()) {
                    highlightedEntities.add(bee);
                    // 同じ型でも呼び名ごとに色が変わるため、mixin から引けるよう控えておく
                    customGlowColors.put(bee, target.glowColorRGB());
                }
                registerTracer(bee, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(bee, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Critter Safari: エリア内で型が一意なモブは、型(オウムのみ変種も)だけで判定できる
        if (scanSafariTypes) {
            for (Entity scanned : client.world.getEntities()) {
                Entity entity = scanned;
                // ネームタグ経由で別のモブの一部と確定しているものは飛ばす
                if (nametagClaimedEntities.contains(entity)) continue;
                MobVisual target = safariTarget(client, entity);
                if (target == null) continue;
                // 自分がいるバイオームのモブだけを対象にする。
                // Safari は狭く4バイオームぶんのモブが同時に読み込まれるため、
                // これが無いと隣のバイオームのモブまで表示されてしまう
                if (!inSafariBiome(client.player, target)) continue;
                // 対象自身も同じバイオームにいることを確かめ、型の取り違えを防ぐ
                if (!inSafariBiome(entity, target)) continue;

                // Shyworm はヘッドが連なって1体を成すので、まとめて扱う。
                // 連なりの一部として処理済みなら、同じ個体を何度も処理しない
                if (target == SafariCavern.SHYWORM) {
                    if (!rebuiltVisuals.contains(entity)) {
                        applyHeadChain(client, entity, target, HEAD_CHAIN_SEARCH_RADIUS);
                    }
                    continue;
                }

                // 当たり判定が起点になったモブは、見た目のブロック表示(Display)へ差し替える
                if (SAFARI_DISPLAY_TARGETS.contains(target)) {
                    Entity display = nearestOfType(client, entity, DisplayEntity.class);
                    if (display == null) continue;
                    entity = display;
                }

                if (target.highlight()) registerHighlight(entity, target);
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Torrhus Canyon: marker のヘッドと透明なスライムの組で作られたモブ。
        // Water Snake はヘッドが連なって1体を成すので、連なりごとまとめて扱う
        if (scanCanyonHeads) {
            Set<Entity> chained = new HashSet<>();
            for (Entity entity : client.world.getEntities()) {
                // ネームタグ経由で別のモブの一部と確定しているものは飛ばす
                if (nametagClaimedEntities.contains(entity)) continue;
                MobVisual target = canyonHeadTarget(client, entity);
                if (target == null) continue;

                if (target == TorrhusCanyon.WATER_SNAKE) {
                    // 連なりの一部として処理済みなら、同じ個体を何度も処理しない
                    if (!chained.contains(entity)) {
                        chained.addAll(applyHeadChain(client, entity, target, HEAD_CHAIN_SEARCH_RADIUS));
                    }
                    continue;
                }

                if (target.highlight()) registerHighlight(entity, target);
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Invisibug: CRIT パーティクルの近くにいる素のアーマースタンドを本体とみなす
        if (scanInvisibug) {
            resolveInvisibugs(client);
            MobVisual target = MoongladeMarsh.INVISIBUG;
            for (Entity entity : invisibugEntities) {
                if (target.highlight()) {
                    highlightedEntities.add(entity);
                    customGlowColors.put(entity, target.glowColorRGB());
                }
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label, null));
                }
            }
        }

        // Dragonはネームタグ経由で取りこぼすことがあるため、EnderDragonEntity本体を直接探して補完する。
        // ネームプレートは1体だけ出したいので、最初に見つかった個体を対象にする
        if (scanDragon) {
            boolean needGlowFallback = glowDragon && highlightedEntities.stream().noneMatch(e -> e instanceof EnderDragonEntity);
            Entity plateDragon = null;
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof EnderDragonEntity)) continue;
                if (needGlowFallback) highlightedEntities.add(entity);
                registerTracer(entity, TheEnd.DRAGON, dragonTracerColor());
                if (plateDragon == null) plateDragon = entity;
            }
            if (TheEnd.DRAGON.nameplate() && plateDragon != null) {
                String type = GameState.Dragon.type != null ? GameState.Dragon.type : "Ender";
                String label = dragonColorCode(GameState.Dragon.type) + "§l" + type + " Dragon";
                nameplateEntities.put(plateDragon, BossNameplateRenderer.buildLabel(label, GameState.Dragon.health));
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
                if (!glowBoss && !plateBoss && !boss.enableTracer().get()) break;

                Vec3d center = Vec3d.ofCenter(ModConstants.BLADESOUL_POS);
                Box area = new Box(
                        center.x - BLADESOUL_AREA_XZ, center.y - CRIMSON_AREA_Y, center.z - BLADESOUL_AREA_XZ,
                        center.x + BLADESOUL_AREA_XZ, center.y + CRIMSON_AREA_Y, center.z + BLADESOUL_AREA_XZ);
                List<WitherSkeletonEntity> skeletons = client.world.getEntitiesByClass(WitherSkeletonEntity.class, area, e -> true);
                if (skeletons.isEmpty()) break;
                // Blaze は Wither Skeleton から数ブロック上下にずれるため、範囲の上下だけ広げて探す
                List<BlazeEntity> blazes = client.world.getEntitiesByClass(
                        BlazeEntity.class, area.expand(0, BLADESOUL_PAIR_Y, 0), e -> true);
                if (blazes.isEmpty()) break;

                Entity plateTarget = null;
                for (WitherSkeletonEntity skeleton : skeletons) {
                    Entity pairedBlaze = null;
                    for (BlazeEntity blaze : blazes) {
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
                    if (plateTarget == null) {
                        plateTarget = skeleton;
                        registerTracer(skeleton, boss);
                    }
                    bossFound[i] = true;
                }

                if (plateBoss && plateTarget != null) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(plateTarget, BossNameplateRenderer.buildLabel(label, boss.getHealth().get()));
                }
                break;
            }
        }

        // Magma Glare: Kill the Magmas フェーズ中は本体が存在しないので、エリア内の MagmaCubeEntity は
        // すべて分裂した個体。ただし同フェーズには無害な Unstable Magma も湧いており、
        // そちらはバニラ自然湧きサイズの小さい個体なのでサイズで振り分ける。
        // ネームタグに依存しないので、個体数が多くても取りこぼさない
        if (scanMagmaGlare && GameState.MagmaBoss.inArena) {
            boolean glowGlare = CrimsonIsle.MAGMA_GLARE.highlight();
            boolean plateGlare = CrimsonIsle.MAGMA_GLARE.nameplate();
            for (MagmaCubeEntity cube : magmaCubesInArena(client, e -> e.getSize() > MAGMA_MAX_NATURAL_SIZE)) {
                if (glowGlare) highlightedEntities.add(cube);
                magmaGlareEntities.add(cube);
                registerTracer(cube, CrimsonIsle.MAGMA_GLARE);
                // 即死級のダメージを与えてくるため、名前は警告の形で目立たせたうえで体力を添える
                if (plateGlare) {
                    nameplateEntities.put(cube,
                            BossNameplateRenderer.buildLabel("§c§l! Magma Glare !", findNearbyHealth(client, cube)));
                }
            }
        }

        // Magma Boss: 基準座標から一定範囲内の MagmaCubeEntity を本体とみなす。ネームタグには依存しない。
        //
        // 判定はエリア内(spawnStatus が読める = Magma Chamber のサイドバーが出ている)に限定する。
        // エリア外では Sea Creature など同じ MagmaCubeEntity 型のモブを誤検出してしまうため行わない。
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

                    // Final Stage 以外では本体が巨大なので、サイズでも絞り込む。
                    // Final Stage は本体が小さくなるためサイズ判定を使えない
                    boolean requireLargeSize = !"Final Stage".equals(magmaStatus);

                    // ワールドテキストの基準座標(戦闘エリア中心)から一定範囲内に限定し、
                    // 遠方の無関係な MagmaCube を拾わないようにする
                    List<MagmaCubeEntity> cubes = magmaCubesInArena(client,
                            e -> !requireLargeSize || e.getSize() > MAGMA_MAX_NATURAL_SIZE);
                    if (cubes.isEmpty()) break;

                    // エリア内に本体は1体しかいないため、基準座標に最も近い1体だけを対象にする
                    Vec3d arenaCenter = Vec3d.ofCenter(ModConstants.MAGMA_BOSS_POS);
                    Entity target = null;
                    double nearest = Double.MAX_VALUE;
                    for (MagmaCubeEntity cube : cubes) {
                        double dist = cube.squaredDistanceTo(arenaCenter);
                        if (dist < nearest) {
                            nearest = dist;
                            target = cube;
                        }
                    }

                    if (glowBoss) highlightedEntities.add(target);
                    crimsonBossEntities.put(target, boss);
                    registerTracer(target, boss);
                    if (plateBoss) {
                        String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                        nameplateEntities.put(target, BossNameplateRenderer.buildLabel(label, boss.getHealth().get()));
                    }
                    bossFound[i] = true;
                    break;
                }
            }
        }

        // isDetected 状態を更新（タイトルなし）
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
                if (present) {
                    absenceSince.remove(boss.nameTag());
                    killedConfirmedAt.remove(boss.nameTag());
                    lastConfirmedSpawned.add(boss.nameTag());
                } else if (!canObserveSpawnPoint(client, boss.nameTag())) {
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
    private static void detectPlayerBoss(MinecraftClient client, String bossName, String entityName, boolean[] bossFound) {
        for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
            CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
            if (!bossName.equals(boss.nameTag())) continue;

            boolean glowBoss = boss.enableHighlight().get();
            boolean plateBoss = boss.enableNameplate().get();

            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == client.player || !matchesEntityName(player, entityName)) continue;

                if (glowBoss) highlightedEntities.add(player);
                crimsonBossEntities.put(player, boss);
                registerTracer(player, boss);
                if (plateBoss) {
                    String label = BossNameplateRenderer.colorCode(boss.tracerColorARGB()) + "§l" + boss.nameTag();
                    nameplateEntities.put(player, BossNameplateRenderer.buildLabel(label, boss.getHealth().get()));
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

    private static Entity findVisualEntity(MinecraftClient client, Entity namedEntity, String bossName) {
        if (!(namedEntity instanceof ArmorStandEntity)) return namedEntity;
        Box box = namedEntity.getBoundingBox().expand(8.0);

        // Magma Boss: MagmaCubeEntity のみを対象とし、ジャンプ中の位置ずれに対応するため広めに探索。
        // Kill the Magmas と Reforming の間はボス本体が存在せず、分裂した個体しかいない。
        // Reforming ではそれらがフィールド中央(ネームタグの近く)に集まり誤認するため、両フェーズともスキップする
        if ("Magma Boss".equals(bossName)) {
            String status = GameState.MagmaBoss.spawnStatus;
            if ("Kill the Magmas".equals(status) || "Reforming...".equals(status)) return null;
            Box wideBox = namedEntity.getBoundingBox().expand(20.0);
            return getClosestEntity(client.world.getEntitiesByClass(MagmaCubeEntity.class, wideBox, e -> true), namedEntity);
        }

        // PlayerEntity型ボス(Barbarian Duke X / Mage Outlaw)は実在の他プレイヤーと型で区別できないため名前で照合する。
        // それ以外では、近傍にMobが見つからない場合でも
        // Player を視覚エンティティとして誤マッチさせないよう、Player検索はPlayer型ボスのときのみ行う
        String playerBossName = playerBossEntityName(bossName);
        if (playerBossName != null) {
            Entity closest = getClosestEntity(
                    client.world.getEntitiesByClass(PlayerEntity.class, box,
                            e -> e != client.player && matchesEntityName(e, playerBossName)),
                    namedEntity);
            if (closest != null) return closest;
        } else {
            Entity closest = getClosestEntity(client.world.getEntitiesByClass(MobEntity.class, box,
                    e -> !crimsonBossEntities.containsKey(e)), namedEntity);
            if (closest != null) return closest;
        }

        return getClosestEntity(client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                e -> e != namedEntity && e.getCustomName() == null), namedEntity);
    }


    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            double d = e.squaredDistanceTo(center);
            if (d < minDist) { minDist = d; closest = e; }
        }
        return closest;
    }

    // Critter Capsule を当てた回数。HPと同じ位置に出したいので、
    // 数値パースを挟まず色付き文字列をそのまま描かせる印(RAW)を付ける
    private static String capsuleLabel(int hits) {
        int shown = Math.min(hits, ModConstants.CAPSULE_MAX_THROWS);
        return ModConstants.RAW_HEALTH_PREFIX + "§e" + shown + "§7/§e" + ModConstants.CAPSULE_MAX_THROWS;
    }


    // Moonglade Marsh と Torrhus Canyon は体色を問わず1種類しかいない。
    // Safari のみ体色で呼び名が分かれるため、そこだけ DyeColor を見る。
    // 染色されていない個体(getColor() == null)や想定外の体色は対象外とする
    private static MobVisual shulkerTarget(ShulkerEntity shulker) {
        if (GameState.Server.isMoongladeMarsh()) return MoongladeMarsh.HIDEONLEAF;
        if (GameState.Server.isTorrhusCanyon()) return TorrhusCanyon.HIDEONSUN;
        if (!GameState.Server.isSafari()) return null;

        DyeColor color = shulker.getColor();
        if (color == null) return null;
        return switch (color) {
            // Hypixel 側がどちらの緑/紫を使っていても拾えるよう、近い色をまとめて扱う
            case GREEN, LIME -> SafariForest.HIDEONFLOOR;
            case PURPLE, MAGENTA -> SafariHaunted.HIDEONWALL;
            default -> null;
        };
    }

    // Critter Safari のモブ。4つのバイオームに分かれているが、エリア名はどこも "Safari" なので
    // バイオームでは絞れない。幸い型はエリア全体で重複しないため、型だけで呼び名が決まる
    private static MobVisual safariTarget(MinecraftClient client, Entity entity) {
        // Hypixel は「見た目のエンティティ」と「当たり判定のモブ」を重ねて1体のモブを作る。
        // 当たり判定側は必ず透明にされているので、透明なものは見た目の主役ではないと判断できる。
        //   Rockmite … シルバーフィッシュそのもの(見えている)
        //   Hideonwall / Duplico / Driftling / Chuckwalla / Troodon
        //            … 見た目は別のエンティティで、中の透明なシルバーフィッシュは当たり判定
        // 見た目がアーマースタンドのモブ。装備の無いものはネームタグ用の透明なスタンド
        if (entity instanceof ArmorStandEntity stand) {
            if (stand.getCustomName() != null || !hasAnyEquipment(stand)) return null;
            if (inSafariHaunted(entity)) return SafariHaunted.GAZER;
            // Driftling はヘッドの中に透明なシルバーフィッシュを抱えている。
            // 同じ Cavern の Shyworm はスライムを抱えているので区別できる
            if (inSafariCavern(entity) && hasNear(client, entity, SilverfishEntity.class)) {
                return SafariCavern.DRIFTLING;
            }
            // Shyworm はヘッドが連なった1体。抱えているのはスライムなので Driftling と区別できる
            if (inSafariCavern(entity) && hasNear(client, entity, SlimeEntity.class)) {
                return SafariCavern.SHYWORM;
            }
            return null;
        }

        // 透明なものは別のモブの当たり判定。型といるバイオームで呼び名が決まる
        if (entity.isInvisible()) {
            if (entity instanceof SilverfishEntity) {
                if (hasHeadStandNear(client, entity)) return null;           // Driftling はスタンド側で判定済み

                // シュルカーを伴うものは Hideonfloor / Hideonwall。動いている間は少し離れる
                if (hasNearWithin(client, entity, ShulkerEntity.class, SHULKER_COMPANION_RADIUS)) return null;

                if (inSafariCavern(entity)) return SafariCavern.CHUCKWALLA;
                if (inSafariHaunted(entity)) return SafariHaunted.DUPLICO;
                if (inSafariIcy(entity)) return SafariIcy.TROODON;
                // Forest で透明なシルバーフィッシュを使うのは Hideonfloor だけ。
                // 動いてシュルカーを見失っても、見た目のブロックを対象にできる
                if (inSafariForest(entity)) return SafariForest.HIDEONFLOOR;
                return null;
            }
            if (entity instanceof BatEntity) {
                return inSafariCavern(entity) ? SafariCavern.FLITTER : null;
            }
            if (entity instanceof TropicalFishEntity) {
                if (inSafariHaunted(entity)) return SafariHaunted.GIMMIEGOLD;
                if (inSafariIcy(entity)) return SafariIcy.MANTIS_SHRIMP;
                return null;
            }
            // Shyworm(スライム)など、見た目を別で拾うものはここでは扱わない
            return null;
        }

        if (entity instanceof SilverfishEntity) return SafariCavern.ROCKMITE;
        if (entity instanceof ArmadilloEntity) return SafariCavern.SCRAPPY;
        if (entity instanceof SnifferEntity) return SafariCavern.SNOOZLE;
        if (entity instanceof VexEntity) return SafariCavern.GEMZIE;

        if (entity instanceof FoxEntity) return SafariForest.FOXTROT;
        if (entity instanceof BeeEntity) return SafariForest.HONEYBUG;
        if (entity instanceof FrogEntity) return SafariForest.TREEFROG;
        if (entity instanceof CreakingEntity) return SafariForest.WOODCHUCKER;
        if (entity instanceof PandaEntity) return SafariForest.FLUFFLING;
        // オウムは3種いるので変種で分ける
        if (entity instanceof ParrotEntity parrot) {
            return switch (parrot.getVariant()) {
                case BLUE -> SafariForest.BLUEBIRD;
                case GREEN -> SafariForest.PARAKEET;
                case RED_BLUE -> SafariForest.MACAW;
                default -> null;
            };
        }

        // CaveSpider は Spider の派生。Broodmother とはエリアが違うので競合しない
        if (entity instanceof CaveSpiderEntity) return SafariHaunted.AREITA;
        if (entity instanceof BatEntity) return SafariHaunted.BLOODBAT;
        if (entity instanceof EndermiteEntity) return SafariHaunted.LITTERBUG;
        if (entity instanceof PhantomEntity) return SafariHaunted.SOLSNATCHER;

        if (entity instanceof GoatEntity) return SafariIcy.BILLYGOAT;
        if (entity instanceof DolphinEntity) return SafariIcy.NOZZLENOSE;
        if (entity instanceof PolarBearEntity) return SafariIcy.POLARIS;
        if (entity instanceof GlowSquidEntity) return SafariIcy.SHUDDERSQUID;
        if (entity instanceof SnowGolemEntity) return SafariIcy.STRONGARM;
        // Cavernfish(茶・灰)と Tepid(白)はどちらも熱帯魚。色で分ける
        if (entity instanceof TropicalFishEntity fish) return tropicalFishTarget(fish);
        return null;
    }

    // 呼び出し元で Moonglade Marsh / Torrhus Canyon のどちらかであることを保証している。
    // カエル・オウム・アルマジロはエリア内に1種しかいないため型だけで確定でき、
    // ウーパールーパーのみ Torrhus Canyon に2種いるので変種で振り分ける
    private static MobVisual areaAnimalTarget(Entity entity) {
        // 透明なものは、別のモブの当たり判定か、モブ以外の見た目のために置かれたエンティティ。
        // Critter を捕まえる Lasso の先端は透明なコウモリで、そのままだと Murkbat になってしまう
        if (entity.isInvisible()) return null;

        boolean marsh = GameState.Server.isMoongladeMarsh();
        if (entity instanceof FrogEntity) return marsh ? MoongladeMarsh.MOSSYBIT : TorrhusCanyon.DUSTYBIT;
        // Pangolin と Blue Jay は Torrhus Canyon のみ。Moonglade Marsh の該当種は対象外
        if (entity instanceof ArmadilloEntity) return marsh ? null : TorrhusCanyon.PANGOLIN;
        if (entity instanceof ParrotEntity) return marsh ? null : TorrhusCanyon.BLUE_JAY;
        if (entity instanceof AxolotlEntity axolotl) {
            if (marsh) return MoongladeMarsh.CORALOT;
            return switch (axolotl.getVariant()) {
                case WILD -> TorrhusCanyon.SEPIALOT;
                case GOLD -> TorrhusCanyon.GOLDOLOT;
                default -> null;
            };
        }

        // 以降は Moonglade Marsh にしか出現しない型
        if (marsh) {
            if (entity instanceof CodEntity) return MoongladeMarsh.COD;
            if (entity instanceof SalmonEntity) return MoongladeMarsh.SALMON;
            if (entity instanceof DolphinEntity) return MoongladeMarsh.JOYDIVE;
            if (entity instanceof GlowSquidEntity) return MoongladeMarsh.LUMISQUID;
            if (entity instanceof TurtleEntity) return MoongladeMarsh.SHELLWISE;
            if (entity instanceof PufferfishEntity) return MoongladeMarsh.SPIKE;
            if (entity instanceof TadpoleEntity) return MoongladeMarsh.BIRRIES;
            if (entity instanceof SnifferEntity) return MoongladeMarsh.HEWVER;
            if (entity instanceof HoglinEntity) return MoongladeMarsh.HONEYHOG;
            if (entity instanceof EndermiteEntity) return MoongladeMarsh.HONEYMITE;
            if (entity instanceof BatEntity) return MoongladeMarsh.MURKBAT;
            // Tidetot はドラウンド。大人も子どももいるので大きさでは絞れない。
            // 同じエリアの Stridersurfer もドラウンドだが、こちらは必ずストライダーに
            // 乗っているので、騎乗していないことを条件にすれば弾ける
            if (entity instanceof DrownedEntity drowned && !drowned.hasVehicle()) return MoongladeMarsh.TIDETOT;
            // Chillblade / Chillshot はどちらも Stray。Chill としてまとめて扱う
            if (entity instanceof StrayEntity) return MoongladeMarsh.CHILL;
            // ファントムは3種いるが、大きさが段違いなので当たり判定の幅で見分けられる
            if (entity instanceof PhantomEntity) {
                if (entity.getWidth() >= DREADWING_MIN_WIDTH) return MoongladeMarsh.DREADWING;
                return entity.getWidth() >= PHANFLARE_MIN_WIDTH ? MoongladeMarsh.PHANFLARE : MoongladeMarsh.PHANPYRE;
            }
            // Azure と Verdant はどちらも熱帯魚。色でしか見分けられない
            if (entity instanceof TropicalFishEntity fish) return tropicalFishTarget(fish);
            // パンダは2種いるが、Mochibear だけが茶色の個体なので毛色で分けられる
            if (entity instanceof PandaEntity panda) {
                return panda.getProductGene() == PandaEntity.Gene.BROWN ? MoongladeMarsh.MOCHIBEAR : MoongladeMarsh.BAMBULEAF;
            }
            return null;
        }

        // 以降は Torrhus Canyon にしか出現しない型
        if (entity instanceof RabbitEntity) return TorrhusCanyon.BUNBUN;
        if (entity instanceof CreakingEntity) return TorrhusCanyon.DRYBARK;
        if (entity instanceof FoxEntity) return TorrhusCanyon.FIREFOX;
        if (entity instanceof HoglinEntity) return TorrhusCanyon.GROUNDHOG;
        if (entity instanceof GhastEntity) return TorrhusCanyon.HIVETHIEF;
        if (entity instanceof GoatEntity) return TorrhusCanyon.MOUNTAIN_GOAT;
        if (entity instanceof VexEntity) return TorrhusCanyon.PUCK;
        // Parched はバニラのスケルトン系モブ。このエリアで同じ型は他にいない
        if (entity instanceof ParchedEntity) return TorrhusCanyon.PARCHED;
        // Beeheemoth 以外のハチはネームタグ側で振り分けるため、ここでは大きい個体だけを拾う
        if (entity instanceof BeeEntity) return isBeeheemoth(entity) ? TorrhusCanyon.BEEHEEMOTH : null;
        // Ember / Solar / Timil も熱帯魚。こちらも色でしか見分けられない
        if (entity instanceof TropicalFishEntity fish) return tropicalFishTarget(fish);
        return null;
    }

    // 溜まった CRIT パーティクルを Invisibug 本体に解決する。
    // 既に見つけている個体の近くのパーティクルは読み飛ばし、同じ個体を何度も探し直さない
    private static void resolveInvisibugs(MinecraftClient client) {
        invisibugEntities.removeIf(Entity::isRemoved);

        double[] particle;
        while ((particle = pendingCritParticles.poll()) != null) {
            Vec3d pos = new Vec3d(particle[0], particle[1], particle[2]);
            if (invisibugEntities.stream().anyMatch(e -> e.squaredDistanceTo(pos) < INVISIBUG_RADIUS * INVISIBUG_RADIUS)) continue;

            Box box = new Box(
                    pos.x - INVISIBUG_RADIUS, pos.y - INVISIBUG_RADIUS, pos.z - INVISIBUG_RADIUS,
                    pos.x + INVISIBUG_RADIUS, pos.y + INVISIBUG_RADIUS, pos.z + INVISIBUG_RADIUS);
            ArmorStandEntity nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (ArmorStandEntity stand : client.world.getEntitiesByClass(ArmorStandEntity.class, box, e -> true)) {
                double distSqr = stand.squaredDistanceTo(pos);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearest = stand;
                }
            }
            // 最も近いものが素のアーマースタンドでなければ、そのパーティクルは Invisibug 由来ではない
            if (nearest != null && isPlainArmorStand(nearest)) invisibugEntities.add(nearest);
        }
    }

    // 名前も装備も持たない、初期状態のままのアーマースタンドか。
    // Hypixel の他のモブはネームタグや装備を持つため、これで Invisibug 以外を弾ける
    private static boolean isPlainArmorStand(ArmorStandEntity stand) {
        if (stand.getCustomName() != null) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stand.getEquippedStack(slot).isEmpty()) return false;
        }
        return true;
    }

    // Azure / Verdant / Ember / Solar / Timil はいずれも熱帯魚で、色でしか見分けられない。
    // 体色で判別できない場合(Timil の白地にピンクの模様など)に備え、模様の色も見る
    private static MobVisual tropicalFishTarget(TropicalFishEntity fish) {
        // Safari の Tepid は白一色。体にオレンジが入るものは別のモブなので先に弾く
        if (GameState.Server.isSafari()
                && (fish.getBaseColor() == DyeColor.ORANGE || fish.getPatternColor() == DyeColor.ORANGE)) {
            return null;
        }
        MobVisual byBase = tropicalFishColor(fish.getBaseColor());
        return byBase != null ? byBase : tropicalFishColor(fish.getPatternColor());
    }

    // 近い色はまとめて扱い、Hypixel 側がどの色を使っていても拾えるようにする。
    // 同じ熱帯魚でもエリアごとに呼び名が違うため、エリアで分岐する
    private static MobVisual tropicalFishColor(DyeColor color) {
        if (color == null) return null;
        if (GameState.Server.isMoongladeMarsh()) {
            return switch (color) {
                case LIGHT_BLUE, CYAN, BLUE -> MoongladeMarsh.AZURE;
                case GREEN, LIME -> MoongladeMarsh.VERDANT;
                default -> null;
            };
        }
        if (GameState.Server.isSafari()) {
            return switch (color) {
                case BROWN, GRAY, LIGHT_GRAY -> SafariCavern.CAVERNFISH;
                case WHITE -> SafariIcy.TEPID;
                default -> null;
            };
        }
        return switch (color) {
            case ORANGE -> TorrhusCanyon.EMBER;
            case YELLOW -> TorrhusCanyon.SOLAR;
            case PINK, MAGENTA -> TorrhusCanyon.TIMIL;
            default -> null;
        };
    }

    /**
     * ネームプレートと Tracer を合わせる高さ(足元からのブロック数)。
     *
     * 通常は当たり判定の中心でよいが、Hypixel が「skull を被せたアーマースタンド」で
     * 見た目を作っているモブは、当たり判定の中心と見た目が一致しない。
     * (marker のアーマースタンドは当たり判定が 0 なので、そのままだと足元に出てしまう)
     * その場合だけ頭の位置に合わせる。
     */
    public static double renderAnchorHeight(Entity entity) {
        // 実測から求めた高さがあればそれを使う
        Double measured = renderAnchors.get(entity);
        if (measured != null) return measured;

        // Display は当たり判定を持たないので、原点をそのまま表示位置にする
        if (entity instanceof DisplayEntity) return DISPLAY_ANCHOR;

        if (entity instanceof ArmorStandEntity stand && !stand.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            // marker のアーマースタンドは当たり判定が 0 になるため、当たり判定からは大きさを測れない。
            // 見た目の大きさは scale 属性で決まるので、そちらを基準にする
            double base = stand.isSmall() ? SMALL_HEAD_STAND_ANCHOR : HEAD_STAND_ANCHOR;
            return base * stand.getScaleFactor();
        }
        return entity.getHeight() / 2.0;
    }

    // 複数のヘッドで1体を成すモブの表示。連なったヘッドをすべて光らせる。
    // ネームプレートは重ならないよう、エンティティIDが最も小さいヘッド1つだけに出す。
    // 同じモブに複数のネームタグが付いていても、代表が同じになるので二重に出ない
    private static List<Entity> applyHeadChain(MinecraftClient client, Entity nameTag, MobVisual target, double searchRadius) {
        List<Entity> heads = connectedHeadStands(client, nameTag, searchRadius);
        if (heads.isEmpty()) return heads;

        for (Entity head : heads) {
            if (target.highlight()) registerHighlight(head, target);
        }

        // 表示位置が飛ばないよう、代表は毎 tick 同じヘッドになるように選ぶ。
        // エンティティIDは湧いた順に振られるので、ID順に並べた真ん中がモブの中ほどにあたる
        heads.sort(Comparator.comparingInt(Entity::getId));
        Entity representative = heads.get(heads.size() / 2);

        // Tracer をヘッドごとに登録すると、自分に最も近いヘッドが入れ替わるたびに
        // 線の行き先が上のヘッドと真ん中のヘッドの間で飛んでしまう。
        // ネームプレートと同じ代表のヘッドにだけ出す
        registerTracer(representative, target);
        if (target.nameplate()) {
            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
            nameplateEntities.put(representative, BossNameplateRenderer.buildLabel(label, null));
        }
        return heads;
    }

    // ネームタグは本体の真上に出るため、水平のずれが最も小さいヘッドを本体とみなす。
    // 単純な3D距離で選ぶと、近くにいる別の同型モブ(Ant と Queen Ant など)を掴んでしまい、
    // しかも一度覚えると入れ替わらないので、間違った表示が残り続けてしまう
    private static Entity headStandUnderNameTag(List<? extends Entity> candidates, Entity nameTag) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            double dx = candidate.getX() - nameTag.getX();
            double dz = candidate.getZ() - nameTag.getZ();
            double horizontalSqr = dx * dx + dz * dz;
            double dy = Math.abs(nameTag.getY() - candidate.getY());

            // 水平のずれを強く優先しつつ、高さの差も少しだけ見る。
            // 条件で弾かずに一番それらしいものを選ぶことで、
            // ヘッドが少し上にある場合などでも取りこぼさない
            double score = horizontalSqr * 100.0 + dy;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static void registerHighlight(Entity visual, MobVisual target) {
        // marker でないアーマースタンドは、そのままだと腕・胴・脚の輪郭まで出てしまう。
        // 描画時だけ marker として扱わせ、ヘッドだけの輪郭にする
        if (visual instanceof ArmorStandEntity stand && !stand.isMarker()) headOnlyGlowEntities.add(visual);

        highlightedEntities.add(visual);
        // アーマースタンドや Display は型による一括削除ができないので、毎 tick 作り直す集合へ入れる
        rebuiltVisuals.add(visual);
        // 同じ型でも呼び名ごとに色が変わるため、mixin から引けるよう控えておく
        customGlowColors.put(visual, target.glowColorRGB());
    }

    // Torrhus Canyon のヘッド系モブ。見た目は marker のアーマースタンドに被せた skull で、
    // 節ごとに透明なスライムが当たり判定として付いている。
    // 装飾として置かれているアーマースタンドは marker でなくスライムも伴わないため、これで区別できる
    private static MobVisual canyonHeadTarget(MinecraftClient client, Entity entity) {
        if (!(entity instanceof ArmorStandEntity stand)) return null;
        if (!stand.isMarker() || stand.getCustomName() != null) return null;

        ItemStack head = stand.getEquippedStack(EquipmentSlot.HEAD);
        if (head.isEmpty()) return null;
        // Tiki 系は節ごとに固有のプロフィールを持つ skull を使っているので、素の skull だけを拾う
        if (headProfileName(head) != null) return null;
        if (!hasNear(client, entity, SlimeEntity.class)) return null;

        float scale = stand.getScaleFactor();
        if (scale < CANYON_ANT_MAX_SCALE) return TorrhusCanyon.ANT;
        if (scale < CANYON_QUEEN_ANT_MAX_SCALE) return TorrhusCanyon.QUEEN_ANT;
        return TorrhusCanyon.WATER_SNAKE;
    }

    // skull に設定されたプロフィール名。素のプレイヤーヘッドなら null
    private static String headProfileName(ItemStack head) {
        ProfileComponent profile = head.get(DataComponentTypes.PROFILE);
        return profile == null ? null : profile.getName().orElse(null);
    }

    // 対象がその呼び名のバイオームにいるか
    private static boolean inSafariBiome(Entity entity, MobVisual target) {
        if (target instanceof SafariCavern) return inSafariCavern(entity);
        if (target instanceof SafariForest) return inSafariForest(entity);
        if (target instanceof SafariHaunted) return inSafariHaunted(entity);
        if (target instanceof SafariIcy) return inSafariIcy(entity);
        return true;
    }

    private static boolean inSafariCavern(Entity entity) {
        return entity.getX() < SAFARI_CENTER_X && entity.getZ() > SAFARI_CENTER_Z;
    }

    private static boolean inSafariForest(Entity entity) {
        return entity.getX() > SAFARI_CENTER_X && entity.getZ() > SAFARI_CENTER_Z;
    }

    private static boolean inSafariHaunted(Entity entity) {
        return entity.getX() > SAFARI_CENTER_X && entity.getZ() < SAFARI_CENTER_Z;
    }

    private static boolean inSafariIcy(Entity entity) {
        return entity.getX() < SAFARI_CENTER_X && entity.getZ() < SAFARI_CENTER_Z;
    }

    // すぐ近くにある、指定した種類の最も近いエンティティ
    private static Entity nearestOfType(MinecraftClient client, Entity entity, Class<? extends Entity> type) {
        Box box = entity.getBoundingBox().expand(HITBOX_HEAD_RADIUS);
        return getClosestEntity(client.world.getEntitiesByClass(type, box, e -> true), entity);
    }

    // すぐ近くに指定した種類のエンティティがあるか。
    // Hypixel は「当たり判定のモブ」と「見た目のエンティティ」を重ねて1体のモブを作るので、
    // 何と一緒にいるかで同じ型のモブを見分けられる
    private static boolean hasNear(MinecraftClient client, Entity entity, Class<? extends Entity> type) {
        return hasNearWithin(client, entity, type, HITBOX_HEAD_RADIUS);
    }

    private static boolean hasNearWithin(MinecraftClient client, Entity entity, Class<? extends Entity> type, double radius) {
        Box box = entity.getBoundingBox().expand(radius);
        return !client.world.getEntitiesByClass(type, box, e -> true).isEmpty();
    }

    // すぐ近くに「頭に skull を被せたアーマースタンド」があるか。
    // ある場合、その当たり判定は別のモブの一部なので単独のモブとして扱わない
    private static boolean hasHeadStandNear(MinecraftClient client, Entity entity) {
        Box box = entity.getBoundingBox().expand(HITBOX_HEAD_RADIUS);
        return !client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                e -> e.getCustomName() == null && !e.getEquippedStack(EquipmentSlot.HEAD).isEmpty()).isEmpty();
    }

    // プレイヤー型のモブ(Grizzly Bear や Hideyho など)の本体を探す
    private static Entity nearestPlayerNear(MinecraftClient client, Entity nameTag) {
        Box box = nameTag.getBoundingBox().expand(NAMETAG_SEARCH_RADIUS);
        return getClosestEntity(client.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != client.player), nameTag);
    }

    // ネームタグの近くから本体を探す。Hypixel は実体を通常のモブで作ることも、
    // skull を被せたアーマースタンドで作ることもあるので、その両方を候補にする
    private static Entity nametagVisual(MinecraftClient client, Entity nameTag) {
        Box box = nameTag.getBoundingBox().expand(NAMETAG_SEARCH_RADIUS);

        // 見た目は「何かを装備したアーマースタンド」か「ブロック/アイテムの Display」。
        // どのスロットで見た目を作っているかはモブによって違うので、スロットは限定しない。
        // 当たり判定のモブを光らせると見えないエンティティが光ってしまうため、そちらへは切り替えない
        Entity stand = headStandUnderNameTag(client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                EntityHighlightManager::hasAnyEquipment), nameTag);
        if (stand != null) return stand;

        Entity display = headStandUnderNameTag(client.world.getEntitiesByClass(DisplayEntity.class, box, e -> true), nameTag);
        if (display != null) return display;

        // ネームタグ自身が見た目を兼ねている場合もある
        return nameTag instanceof ArmorStandEntity named && hasAnyEquipment(named) ? nameTag : null;
    }

    // アーマースタンドが何かを装備しているか。装備が無いものはネームタグ用の透明なスタンド
    private static boolean hasAnyEquipment(ArmorStandEntity stand) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stand.getEquippedStack(slot).isEmpty()) return true;
        }
        return false;
    }

    // ネームタグから本体を探す。見た目の作り方がモブごとに違うので、そこだけ振り分ける
    private static Entity canyonNamedVisual(MinecraftClient client, Entity nameTag, MobVisual target) {
        Box box = nameTag.getBoundingBox().expand(NAMETAG_SEARCH_RADIUS);
        // Grizzly Bear は Barbarian Duke X などと同じプレイヤー型のモブ
        if (target == TorrhusCanyon.GRIZZLY_BEAR) return nearestPlayerNear(client, nameTag);
        // Ant / Queen Ant / Tiki 系は Water Snake と同じく、skull を被せたアーマースタンドで見た目を作る
        return headStandUnderNameTag(client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                    e -> e.getCustomName() == null && !e.getEquippedStack(EquipmentSlot.HEAD).isEmpty()), nameTag);
    }

    // ネームタグの周りから、連なったヘッドを集める。
    // ヘッドは「頭に skull を被せた名前なしのアーマースタンド」で、当たり判定は別のモブが持っている。
    // glow は描画モデルに沿うため、見た目どおりの輪郭にするにはこのアーマースタンドを光らせる
    private static List<Entity> connectedHeadStands(MinecraftClient client, Entity nameTag, double searchRadius) {
        Box box = nameTag.getBoundingBox().expand(searchRadius);
        List<ArmorStandEntity> candidates = new ArrayList<>(client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                e -> e.getCustomName() == null && !e.getEquippedStack(EquipmentSlot.HEAD).isEmpty()));

        // 半径内をすべて拾うと、近くにいる別のヘッドモブ(Ant や Tiki 系)まで巻き込む。
        // ネームタグに最も近い節を起点にして、隣り合う節だけをたどって集める
        List<Entity> heads = new ArrayList<>();
        Entity start = headStandUnderNameTag(candidates, nameTag);
        if (start == null) return heads;
        heads.add(start);
        candidates.remove(start);

        boolean added = true;
        while (added) {
            added = false;
            for (Iterator<ArmorStandEntity> iterator = candidates.iterator(); iterator.hasNext(); ) {
                ArmorStandEntity candidate = iterator.next();
                if (heads.stream().noneMatch(
                        h -> h.squaredDistanceTo(candidate) <= HEAD_CHAIN_GAP * HEAD_CHAIN_GAP)) continue;

                heads.add(candidate);
                iterator.remove();
                added = true;
            }
        }
        return heads;
    }

    // ネームタグの文字列から「現在HP/最大HP」を取り出す
    private static String healthFromNameTag(String nameStr) {
        Matcher matcher = HEALTH_PATTERN.matcher(nameStr);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Beeheemoth かどうか。他の2種のハチはバニラサイズなので、大きさだけで見分けられる
    private static boolean isBeeheemoth(Entity entity) {
        return entity.getHeight() >= BEEHEEMOTH_MIN_HEIGHT;
    }

    // エリア固有モブとして扱う型。対象がエリアや変種で変わるため、毎 tick 作り直す判定に使う
    private static boolean isAreaAnimalEntity(Entity entity) {
        return entity instanceof AxolotlEntity || entity instanceof FrogEntity
                || entity instanceof ParrotEntity || entity instanceof ArmadilloEntity
                || entity instanceof RabbitEntity || entity instanceof CreakingEntity
                || entity instanceof FoxEntity || entity instanceof HoglinEntity
                || entity instanceof GhastEntity || entity instanceof GoatEntity
                || entity instanceof VexEntity || entity instanceof BeeEntity
                || entity instanceof CodEntity || entity instanceof SalmonEntity
                || entity instanceof DolphinEntity || entity instanceof GlowSquidEntity
                || entity instanceof TurtleEntity || entity instanceof PufferfishEntity
                || entity instanceof TadpoleEntity || entity instanceof PandaEntity
                || entity instanceof PhantomEntity || entity instanceof TropicalFishEntity
                || entity instanceof SnifferEntity || entity instanceof EndermiteEntity
                || entity instanceof BatEntity || entity instanceof DrownedEntity
                || entity instanceof StrayEntity
                || entity instanceof ParchedEntity
                || entity instanceof SilverfishEntity || entity instanceof CaveSpiderEntity
                || entity instanceof PolarBearEntity || entity instanceof SnowGolemEntity;
    }

    // Tracer は Highlight と独立して切り替えられる。対象に入っていれば線の色を登録する
    private static void registerTracer(Entity entity, MobVisual target) {
        registerTracer(entity, target, target.tracerColorARGB());
    }

    private static void registerTracer(Entity entity, MobVisual target, int colorARGB) {
        if (target.tracer()) registerNearestTracer(entity, target, colorARGB);
    }

    // Crimson 系はエントリ側の設定(= Mob Visuals のリスト)をそのまま使う
    private static void registerTracer(Entity entity, CrimsonBossEntry boss) {
        if (boss.enableTracer().get()) registerNearestTracer(entity, boss.nameTag(), boss.tracerColorARGB());
    }

    // 線が乱立しないよう、同じモブ(key)の中では自分に最も近い1体だけに Tracer を出す。
    // 走査順は検出方法ごとにばらばらなので、より近い個体が来たら差し替える形で絞り込む
    private static void registerNearestTracer(Entity entity, Object key, int colorARGB) {
        if (entity == null) return;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Entity current = tracerNearest.get(key);
        if (current != null) {
            if (current.squaredDistanceTo(player) <= entity.squaredDistanceTo(player)) return;
            // 差し替えるので、前の個体の線は消す
            tracerEntities.remove(current);
        }
        tracerNearest.put(key, entity);
        tracerEntities.put(entity, colorARGB);
    }

    // Dragon だけは種類ごとに色が変わるため、Tracer の色もそこから作る
    private static int dragonTracerColor() {
        return switch (GameState.Dragon.type == null ? "" : GameState.Dragon.type) {
            case "Protector" -> 0xFF555555;
            case "Old"       -> 0xFFAAAAAA;
            case "Unstable"  -> 0xFFAA00AA;
            case "Young"     -> 0xFFFFFFFF;
            case "Strong"    -> 0xFFFF5555;
            case "Wise"      -> 0xFF55FFFF;
            case "Superior"  -> 0xFFFFFF55;
            default          -> 0xFFFF55FF;
        };
    }

    // Magma Glare は個体が多くネームタグ経由で検出していないため、体力だけを近くのネームタグから拾う。
    // ネームタグには "Magma Glare" とそのまま出るので、それで絞れば
    // 同フェーズに湧く Unstable Magma や本体のタグを拾うことはない
    private static String findNearbyHealth(MinecraftClient client, Entity entity) {
        Box box = entity.getBoundingBox().expand(GLARE_NAMETAG_RADIUS);
        Entity closest = null;
        double nearest = Double.MAX_VALUE;
        for (Entity candidate : client.world.getEntitiesByClass(Entity.class, box, e -> e.getCustomName() != null)) {
            String nameStr = candidate.getCustomName().getString();
            if (!ModConstants.containsIgnoreCase(nameStr, MAGMA_GLARE_NAME)) continue;
            if (!HEALTH_PATTERN.matcher(nameStr).find()) continue;
            double dist = candidate.squaredDistanceTo(entity);
            if (dist < nearest) {
                nearest = dist;
                closest = candidate;
            }
        }
        if (closest == null) return null;
        Matcher m = HEALTH_PATTERN.matcher(closest.getCustomName().getString());
        return m.find() ? m.group(1) : null;
    }
}
