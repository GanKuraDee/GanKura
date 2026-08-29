package com.deeply.gankura.render;

import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.MobVisual;
import com.deeply.gankura.data.MobVisual.CrimsonIsle;
import com.deeply.gankura.data.MobVisual.CrystalHollows;
import com.deeply.gankura.handler.CorleoneHandler;
import com.deeply.gankura.handler.GoldenFishHandler;
// 26.2: EntityType の定数は EntityTypes へ移動
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.client.player.AbstractClientPlayer;
import com.deeply.gankura.data.MobVisual.LotusAtoll;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import com.mojang.authlib.properties.Property;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;
import net.minecraft.world.entity.animal.frog.FrogVariants;

/**
 * Mob Visuals の本体判定。
 *
 * Hypixel のモブは「見た目のエンティティ」と「当たり判定のモブ」が別々のことが多く、
 * ネームタグもまた別のアーマースタンドになっている。
 * そのため「どのエンティティを、どのモブとみなすか」の決め方がモブごとに違う。
 * 手掛かりは次の6通りで、上にあるものほど確実。
 *
 * <p>1. ヘッドのスキン ({@link #HEAD_SKIN_TARGETS})
 * <br>skull のテクスチャIDで種まで決まる。エリアにも位置関係にも依らない。
 * <ul>
 *   <li>Sea Creature … Titanoboa / Blue Ringed Octopus / Giant Isopod / Wiki Tiki
 *       / Fiery Scuttler / Water Hydra(ゾンビ本体が被っている)
 *   <li>Torrhus Canyon … Ant / Queen Ant / Water Snake / Sneaky・Shrieky・Cheeky Tiki
 *   <li>Critter Safari … Gazer / Driftling / Shyworm / Chuckwalla / Flitter
 *       / Gimmiegold / Mantis Shrimp / Troodon
 *   <li>Moonglade Marsh … Stag Beetle / Woodlouse
 *       (2種で skull が同じなので、本体の特定だけに使い、種の別はネームタグで決める)
 * </ul>
 *
 * <p>2. 実体名 ({@link #matchesNamedPlayerMob})
 * <br>プレイヤーエンティティ型(NPC)のモブ。表示名ではなく実体名で照合する。
 * ネームタグより読み込み範囲が広いので、遠くからでも見つかる。
 * <ul>
 *   <li>Sea Creature … Alligator / Yeti / Phantom Fisher / Grim Reaper
 *       / Great White Shark / Abyssal Miner / Ragnarok
 *   <li>その他 … Grizzly Bear / Hideyho / Matcho / Barbarian Duke X / Mage Outlaw
 *   <li>Boss Corleone だけは実体名が使えないので、代わりにスキンで照合する
 * </ul>
 *
 * <p>3. エンティティの構成
 * <br>乗り物と乗り手、あるいは重なった組み合わせで決まるもの。
 * <br>The Loch Emperor(ガーディアン+スケルトン) / Frog Prince(スライム+カエル)
 * / Ragnarok(骸骨の馬+プレイヤー型) / Bladesoul / Ashfang / Magma Boss
 *
 * <p>4. エンティティ型(+エリア / 変種 / 大きさ)
 * <br>そのエリアにその型のモブが1種しかいないと言い切れるもの。
 * <br>Critter Safari の大半、Moonglade Marsh と Torrhus Canyon のエリア動物、
 * Silkbreeze / Thunder / Lord Jawbus / Reindrake / Puddle Jumper / Nessie など
 *
 * <p>5. ネームタグの名前 + 本体の型 ({@link #seaCreatureVisual})
 * <br>バニラと同じ型で、型だけでは断定できないもの。名前で種を決めてから型で本体を探す。
 * <br>Sea Creature 11種 … Mithril Grubber / Water Worm / Poisoned Water Worm
 * / Flaming Worm / Oasis Sheep / Oasis Rabbit / Carrot King / Agarimoo
 * / Lava Blaze / Lava Pigman / Plhlegblast
 *
 * <p>6. ネームタグの名前のみ
 * <br>Broodmother / Arachne / Arachne's Brood / Dragon
 * / Ashfang Follower・Acolyte・Underling / Honeybuzz / Pollendart
 *
 * <p>1 と 2 の面々はネームタグも読むが、種の確定には使わない。
 * どちらもネームタグより遠くまで届くので、ネームタグがあるのに見つからないのは
 * Hypixel 側で差し替えられたときだけ。その場合は {@link #reportMissingVisual} で知らせる。
 * 「ネームタグの下にいるものを、確認せずそのモブとみなす」処理は置かない。
 */
public class EntityHighlightManager {

    // Magma Glare のネームタグに出る名前
    private static final String MAGMA_GLARE_NAME = "Magma Glare";
    // Magma Glare のネームタグを探す半径(ブロック)。個体が密集するため狭めにとる
    private static final double GLARE_NAMETAG_RADIUS = 3.0;
    // 「現在HP/最大HP」の抽出。EntityHealthScanner と同じ形式
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");
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
    // Beeheemoth はミニボス級に大きく、当たり判定もそのぶん大きくなる。
    // バニラの成体ハチの高さは 0.6 なので、明らかに超えているものだけを Beeheemoth とみなす
    private static final float BEEHEEMOTH_MIN_HEIGHT = 1.2f;


    // Invisibug を探す半径(ブロック)。CRIT パーティクルは本体のすぐ近くに出る
    private static final double INVISIBUG_RADIUS = 5.0;
    // Lotus Atoll のカエルで、Critter として扱う大きさの上限。
    // Lotum と Atoll Croaker はバニラ相当の 0.50、
    // Sea Creature の Frog Prince は 1.00、Puddle Jumper は 2.00
    private static final float LOTUM_MAX_WIDTH = 0.7f;

    // Moonglade Marsh のファントム3種は同じ Phantom 型だが、scale 属性で大きさが変えてあり
    // 当たり判定にもそれが出る。実測値は Phanpyre 0.36 / Phanflare 0.90(バニラ相当)。
    // Dreadwing はさらに大きいので、実測値の中間と、余裕を見た値で切る
    private static final float PHANFLARE_MIN_WIDTH = 0.63f;
    private static final float DREADWING_MIN_WIDTH = 1.5f;

    // ネームタグ経由でモブの一部と確定したエンティティ。
    // 型だけで判定するモブが、これらを別のモブとして二重に拾わないようにする
    private static final Set<Entity> nametagClaimedEntities = new HashSet<>();
    // Sea Creature のタイトルはフェードなしで出す。単位は tick
    private static final int SEA_CREATURE_TITLE_FADE = 0;
    private static final int SEA_CREATURE_TITLE_STAY = 70;
    // 自分で釣ったときの文言で出したタイトルが、エンティティの検知と重なりうる時間(ミリ秒)
    private static final long SEA_CREATURE_CATCH_ECHO_MS = 5000;
    // Double Hook で2匹まとめて釣れたときに、名前の後ろに付ける。
    // モブの色に埋もれないよう赤で出す
    private static final String SEA_CREATURE_DOUBLE_HOOK_SUFFIX = " §c§lx2";
    // 自分で釣り上げてタイトルを出した Sea Creature と、その時刻。
    // エンティティ側の重複を、釣れた匹数ぶんだけ抑えるために使う
    private static MobVisual caughtSeaCreature;
    private static long caughtSeaCreatureMillis;
    // まだ控えていないエンティティの数。Double Hook なら2匹湧く
    private static int caughtSeaCreaturePending;
    // タイトルに添える音の大きさと高さ。SkyHanni の Rare Sea Creatures アラートと同じ
    private static final float SEA_CREATURE_SOUND_VOLUME = 1.0f;
    private static final float SEA_CREATURE_SOUND_PITCH = 1.0f;
    // この tick で既に表示を付けた、複数エンティティで1体を成す Sea Creature の部品。
    // 別の経路から同じ個体の別の部品に行き着いても、二重に出さないために使う
    private static final Set<Entity> seaCreaturePartsShown = new HashSet<>();
    // タイトルで知らせ済みの Sea Creature。同じ個体で何度も出さないために覚えておく
    private static final Set<Entity> announcedSeaCreatures = new HashSet<>();

    // Hideon 系のシュルカーが当たり判定として抱えているシルバーフィッシュ。
    // 型だけで判定するモブが、これらを別のモブとして二重に拾わないようにする
    private static final Set<Entity> shulkerClaimedEntities = new HashSet<>();

    // 止まっている Hideonfloor / Hideonwall は、シュルカーと当たり判定が重なっている。
    // 少しの遅れでずれることがあるので、同じ個体とみなす距離は余裕を持たせる
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
    // ただしブロックを映しているものは、原点がブロックの底になる。
    // そのままだと足元に寄ってしまうので、ブロックの中ほどまで上げる
    private static final double BLOCK_DISPLAY_ANCHOR = 0.5;
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
    // 1体を成すヘッドを、見つけた1つから探し集める半径(ブロック)。
    // Titanoboa は長く伸びるので、ネームタグ起点のときより広めに取る
    private static final double HEAD_PARTS_SEARCH_RADIUS = 24.0;
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
    // Matcho もプレイヤーエンティティ型。ボスと違い複数体いるが、探し方は同じ
    private static final String MATCHO_ENTITY_NAME = "Matcho";
    // Critter Safari の Hideyho もプレイヤーエンティティ型。実体名がそのまま呼び名になっている
    private static final String HIDEYHO_ENTITY_NAME = "Hideyho";
    // Torrhus Canyon の Grizzly Bear も同じく、実体名がそのまま呼び名になっている
    private static final String GRIZZLY_BEAR_ENTITY_NAME = "Grizzly Bear";
    // Crystal Hollows の Boss Corleone もプレイヤーエンティティ型だが、
    // 実体名(Team Treasurite)・スコアボードのチーム・装備がどれも他の Treasurite 系と共通で、
    // 個体ごとに違うのはスキンだけ。テクスチャのパス末尾(スキンのハッシュ)で見分ける。
    // Hypixel 側でスキンが差し替えられた場合は、この値も更新が必要
    private static final String CORLEONE_SKIN_HASH = "3c37b434bbb65fe5838afced8301604126214b2a";

    // プレイヤーエンティティ型モブの実体名。探す経路が2つあるので、名前はここにまとめる。
    // Sea Creature は表示名がそのまま実体名なので載せない。
    // Boss Corleone だけは実体名が使えず、スキンで見分ける
    private static final Map<MobVisual, String> NAMED_PLAYER_ENTITY_NAMES = Map.of(
            CrimsonIsle.BARBARIAN_DUKE_X, BARBARIAN_ENTITY_NAME,
            CrimsonIsle.MAGE_OUTLAW, MAGE_OUTLAW_ENTITY_NAME,
            CrimsonIsle.MATCHO, MATCHO_ENTITY_NAME,
            SafariHaunted.HIDEYHO, HIDEYHO_ENTITY_NAME,
            TorrhusCanyon.GRIZZLY_BEAR, GRIZZLY_BEAR_ENTITY_NAME);

    // Crimson Isle のボスのうち、プレイヤーエンティティ型のもの。
    // こちらは CrimsonBossEntry 側から引くため、ボス名を鍵にする
    private static final Map<String, MobVisual> PLAYER_BOSS_TARGETS = Map.of(
            "Barbarian Duke X", CrimsonIsle.BARBARIAN_DUKE_X,
            "Mage Outlaw", CrimsonIsle.MAGE_OUTLAW);

    public static final Set<Entity> highlightedEntities = new HashSet<>();
    public static final Map<Entity, CrimsonBossEntry> crimsonBossEntities = new HashMap<>();
    public static final Set<Entity> magmaGlareEntities = new HashSet<>();
    public static final Set<Entity> arachneEntities = new HashSet<>();
    public static final Set<Entity> arachneBroodEntities = new HashSet<>();
    // エンティティ → 輪郭の色(RGB)。同じ型でもエリアや体色・変種で呼び名(=色)が変わるモブ用に、
    // 走査時に決めた色を mixin 側から引けるよう保持する
    public static final Map<Entity, Integer> customGlowColors = new HashMap<>();

    // Scrappy に与える魚のハイライト色。水色で、Critter の色とは役割を分けている
    private static final int SAFARI_FISH_GLOW_COLOR = 0x55FFFF;

    // Golden Fish の見た目に使われている skull と、ハイライト色。
    // Mob Visuals の対象ではないので、Golden Fish の機能側から直接光らせる
    private static final String GOLDEN_FISH_SKIN =
            "120cf3c0a40fc67e0e5fe0c46b0ae409ac71030a7656da17b11ed001645888fe";
    private static final int GOLDEN_FISH_GLOW_COLOR = 0xFFAA00;

    // Rockmite Mound のハイライト色と、その見た目に使われているスキンのテクスチャ。
    // 色は移植元の Skyblocker と同じ明るい水色
    private static final int ROCKMITE_MOUND_GLOW_COLOR = 0x3AB3DA;
    private static final String ROCKMITE_MOUND_TEXTURE =
            "5dbaab74d1acd0abe9d04abe9928725de5d4495fcb63b647228caf6944c20800";
    // ネームプレート表示対象 → 表示文字列。Glowingとは独立して有効化できるよう別管理とし、
    // 毎tick作り直すことで削除済みエンティティの掃除を不要にしている
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

    // 体力が書いてあるネームタグ。ネームプレートに体力を出すのに使う。
    // 本体の判定には使わないので、取り違えても表示がずれるだけ
    private static final List<Entity> healthNameTags = new ArrayList<>();
    // 本体からネームタグを探す半径(ブロック)。
    // Titanoboa のように長いモブでも届くよう広めに取る
    private static final double HEALTH_NAMETAG_RADIUS = 12.0;

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
    // 判定: 4 型 + 体色
    public static final List<MobVisual> SHULKER_TARGETS = List.of(
            MoongladeMarsh.HIDEONLEAF, TorrhusCanyon.HIDEONSUN,
            SafariForest.HIDEONFLOOR, SafariHaunted.HIDEONWALL);

    // Moonglade Marsh / Torrhus Canyon の固有モブ。探索が必要かどうかの判定にまとめて使う
    // 判定: 4 型 + エリア
    public static final List<MobVisual> AREA_ANIMAL_TARGETS = List.of(
            MoongladeMarsh.CORALOT, MoongladeMarsh.MOSSYBIT,
            MoongladeMarsh.COD, MoongladeMarsh.SALMON, MoongladeMarsh.JOYDIVE,
            MoongladeMarsh.LUMISQUID, MoongladeMarsh.SHELLWISE, MoongladeMarsh.SPIKE,
            MoongladeMarsh.BIRRIES, MoongladeMarsh.BAMBULEAF, MoongladeMarsh.MOCHIBEAR,
            MoongladeMarsh.PHANPYRE, MoongladeMarsh.PHANFLARE, MoongladeMarsh.DREADWING,
            MoongladeMarsh.HEWVER, MoongladeMarsh.HONEYHOG, MoongladeMarsh.HONEYMITE,
            // Nessie は大人のスニッファー。Hewver と同じ型なのでここで一緒に走査する
            MobVisual.SeaCreature.NESSIE,
            MoongladeMarsh.MURKBAT, MoongladeMarsh.TIDETOT, MoongladeMarsh.CHILL,
            MoongladeMarsh.AZURE, MoongladeMarsh.VERDANT,
            TorrhusCanyon.BLUE_JAY, TorrhusCanyon.DUSTYBIT,
            TorrhusCanyon.SEPIALOT, TorrhusCanyon.GOLDOLOT, TorrhusCanyon.PANGOLIN,
            TorrhusCanyon.BUNBUN, TorrhusCanyon.DRYBARK, TorrhusCanyon.FIREFOX,
            TorrhusCanyon.GROUNDHOG, TorrhusCanyon.HIVETHIEF, TorrhusCanyon.MOUNTAIN_GOAT,
            TorrhusCanyon.PUCK, TorrhusCanyon.BEEHEEMOTH,
            TorrhusCanyon.EMBER, TorrhusCanyon.SOLAR, TorrhusCanyon.TIMIL,
            TorrhusCanyon.PARCHED,
            LotusAtoll.LOTUSFISH, LotusAtoll.LOTUM, LotusAtoll.TEWTIL,
            LotusAtoll.FLIPFLOPPER, LotusAtoll.SEASHINE);

    // Critter Safari の、エンティティ型で判別できるモブ。
    // Wumpa / Doomspiral / Hideon 系はネームプレートの中身が違うので専用の処理を持っており、ここには含めない
    // 判定: 1 スキン / 4 型 + バイオーム
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
    // 表示の差し替え先(判定ではない)
    public static final List<MobVisual> SAFARI_DISPLAY_TARGETS = List.of(
            SafariCavern.CHUCKWALLA, SafariCavern.FLITTER,
            SafariHaunted.DUPLICO, SafariHaunted.GIMMIEGOLD,
            SafariIcy.MANTIS_SHRIMP, SafariIcy.TROODON,
            SafariForest.HIDEONFLOOR, SafariHaunted.HIDEONWALL);

    // Critter Safari の、独自の見た目でエンティティ型からは判別できないモブ。ネームタグで判定する。
    // 本命は SAFARI_NAMED_PLAYER_TARGETS の実体名照合で、こちらはその取りこぼし用の保険
    // 判定: 2 実体名
    public static final List<MobVisual> SAFARI_NAMED_TARGETS = List.of(
            SafariHaunted.HIDEYHO);

    // Critter Safari の、プレイヤーエンティティ型(NPC)のモブ。
    // ネームタグ(ArmorStand)より読み込み範囲が広いので、実体名で直接探す
    // 判定: 2 実体名
    public static final List<MobVisual> SAFARI_NAMED_PLAYER_TARGETS = List.of(
            SafariHaunted.HIDEYHO);

    // Crimson Isle の、ネームタグでしか判別できないモブ。
    // Matcho はプレイヤー型(NPC)で、エンティティ型では他の NPC と区別できない
    // 判定: 2 実体名
    public static final List<MobVisual> CRIMSON_NAMED_TARGETS = List.of(
            CrimsonIsle.MATCHO);

    // Crystal Hollows の、ネームタグでしか判別できないモブ。
    // Boss Corleone はプレイヤー型(NPC)で、エンティティ型では他の NPC と区別できない
    // 判定: 2 実体名(Boss Corleone はスキン)
    public static final List<MobVisual> CRYSTAL_NAMED_TARGETS = List.of(
            CrystalHollows.BOSS_CORLEONE);

    // Moonglade Marsh の、ネームタグでしか判別できないモブ。
    // Stag Beetle と Woodlouse は同じ見た目の作りで、型では絞り込めない
    // 判定: 1 スキンで本体を探し、種はネームタグの名前
    public static final List<MobVisual> MARSH_NAMED_TARGETS = List.of(
            MoongladeMarsh.STAG_BEETLE, MoongladeMarsh.WOODLOUSE);

    // Stag Beetle と Woodlouse の見た目に使われている skull。2種で共通なので、
    // 種の区別はネームタグに任せ、これは本体を特定するためだけに使う
    private static final String MARSH_BUG_HEAD_SKIN =
            "e08fc1ae87a7035d32b0b0da58de4801463aaf8b238618024aacb0c515ae3bba";

    // 釣りで湧く Sea Creature の全種。探索が必要かどうかの判定にまとめて使う。
    // 判定の手掛かりは種ごとに違うので、クラスの説明を参照
    public static final List<MobVisual> SEA_CREATURE_TARGETS =
            List.of((MobVisual[]) MobVisual.SeaCreature.values());

    // Sea Creature のうち、プレイヤーエンティティ型(カスタムスキンのNPC)のもの。
    // ネームタグ(アーマースタンド)より読み込み範囲が広いので、実体名で直接探す
    // 判定: 2 実体名
    public static final List<MobVisual> SEA_CREATURE_PLAYER_TARGETS = List.of(
            MobVisual.SeaCreature.ALLIGATOR, MobVisual.SeaCreature.YETI,
            MobVisual.SeaCreature.PHANTOM_FISHER, MobVisual.SeaCreature.GRIM_REAPER,
            MobVisual.SeaCreature.GREAT_WHITE_SHARK, MobVisual.SeaCreature.ABYSSAL_MINER,
            MobVisual.SeaCreature.RAGNAROK);

    // 見た目に使っている skull のテクスチャと、そのモブ。
    // 当たり判定が同じ形の面々を、これで見分ける。
    // Sea Creature だけでなく、Torrhus Canyon の Tiki 系もここで引く。
    // Titanoboa は頭と胴体で skull が違う。胴体の節はすべて同じ skull を使っている。
    // 残りはヘッド１つで一体なので、連なりはたどらない。
    // Water Hydra だけはアーマースタンドではなく、ゾンビ本体が被っている
    // Map.of は10組までなので、足し続けられる ofEntries で作る
    // 判定: 1 ヘッドのスキン
    private static final Map<String, MobVisual> HEAD_SKIN_TARGETS = Map.ofEntries(
            Map.entry("645f2c0bbfe3b8b19b7452072db69a5f59da38ff61415545156e5701e1be756d",
                    MobVisual.SeaCreature.TITANOBOA),
            Map.entry("b82086882b25e9e914362f2048c285c18c8d698a336f7e83f0a1964c760b11",
                    MobVisual.SeaCreature.TITANOBOA),
            Map.entry("b2b6074d0c9d6b89a494cf4f74158282a64ee23ba8a0725633ad70932ada1a8f",
                    MobVisual.SeaCreature.BLUE_RINGED_OCTOPUS),
            Map.entry("3fcb203b029aac4958407daf4072a01a353105162373ce7e559c669d78325bcb",
                    MobVisual.SeaCreature.GIANT_ISOPOD),
            Map.entry("f3c802e580bfefc18c4af94cceb82968b5b4aeab0d832346a633a7473a41dfac",
                    MobVisual.SeaCreature.WIKI_TIKI),
            Map.entry("e64331c8fb750f9043334320c94580e7896955695156d80689e5d0a6c60a10e7",
                    MobVisual.SeaCreature.WIKI_TIKI),
            Map.entry("9122f7a19b3197766b381fb36bfeb6f442d62509e44cc7847c75c8e8c387225a",
                    MobVisual.SeaCreature.WIKI_TIKI),
            Map.entry("c5fd6b9a59ec5b97db8bdc158fbd5f91ef7b317b859fcebe6d09e7bd80eaca9d",
                    MobVisual.SeaCreature.WIKI_TIKI),
            Map.entry("55b194025806687642e2bc239895d646a6d8c193d9253b61bfce908f6ce1b84a",
                    MobVisual.SeaCreature.FIERY_SCUTTLER),
            Map.entry("6d6bcd3bea0dff1f45d808e7a8550f95106af41b6d8d18a0793e19c9255ae845",
                    MobVisual.SeaCreature.WATER_HYDRA),
            // Tiki 系は節ごとに違う skull を使っており、種ごとに面々が違う
            Map.entry("f62bef8ca58d83766c7e26f9fb4fe06c11b93542a9c10f36f460a3b427748223",
                    TorrhusCanyon.SHRIEKY_TIKI),
            Map.entry("7a267c2699e38062ea4baedbfa4bb80a631048f7c8d2ee4c460f40036b084b2b",
                    TorrhusCanyon.SHRIEKY_TIKI),
            Map.entry("419773998a0b85ffebde5ff925ea4c9b9e007436b0eb9a43afd2d3b367d241f6",
                    TorrhusCanyon.SHRIEKY_TIKI),
            Map.entry("28bdbd87b64e049562c986bb4c5b1336d92e68431260a9e0e8f3338d302b9761",
                    TorrhusCanyon.SNEAKY_TIKI),
            Map.entry("b89f1cb02d4acd71940d9e6c29fe75ce3b15d291d9e383030b59e068b5f5a135",
                    TorrhusCanyon.SNEAKY_TIKI),
            Map.entry("3eac8820104cc421e0392142dc7c79eb84bd2be8f8155c29d72f5fc58d4ee953",
                    TorrhusCanyon.SNEAKY_TIKI),
            Map.entry("bb19e5b72167db517ff8b9e2dd0735996641bdb6b6267a758ad2864022023cb7",
                    TorrhusCanyon.CHEEKY_TIKI),
            Map.entry("dfe6925f85cf02d2f8b501291d26aa5f4f6a136de0e974c2e9e3c20633ae9202",
                    TorrhusCanyon.CHEEKY_TIKI),
            Map.entry("247da91d9d4dd57bad2678baf648e4d9dd4b9f454a606962ceaac11ed00dea9f",
                    TorrhusCanyon.CHEEKY_TIKI),
            // Ant 系はヘッド１つ。Water Snake は頭と胴体で skull が違う
            Map.entry("88a51208885548fbbe08271148db3691f59e275a8d723be094b44d34ccfd9c85",
                    TorrhusCanyon.ANT),
            Map.entry("321fcf90d9090de8fd720cb2817a4dfdc2283e828fd44f8bfdc7ff9a8505cc",
                    TorrhusCanyon.QUEEN_ANT),
            Map.entry("1b06d15155c60d0f0f267be05c8436c5a1e764a8c8aca236bb37223f49d9a1d0",
                    TorrhusCanyon.WATER_SNAKE),
            Map.entry("23a0d55ad300e4f8870e754fb06db58c7f15fc292026edc79ea7e4ef3cc808cb",
                    TorrhusCanyon.WATER_SNAKE),
            Map.entry("407b3c3d2c3fe259d69207a14ca5cd99713c7096ba122bb40326f3489e5d0d6c",
                    SafariHaunted.GAZER),
            // 見た目を skull を被せた Display で作っている面々
            Map.entry("8b329e108ac28b0bec8d47b7cdce253df1db80b46052b5915d963e1bcbab0db4",
                    SafariHaunted.GIMMIEGOLD),
            Map.entry("9924c105aa431dabd47952dc1dddd6f751f883423f4db1487d9bacc2cfe99c7a",
                    SafariIcy.MANTIS_SHRIMP),
            Map.entry("53de4135a3b19a2187029c86a0020e58c907c7bdd4e37b7643f120e16a0aa9ab",
                    SafariIcy.TROODON),
            Map.entry("f4c4f8e5fce1ec2d299cb8a395792ecddc497a1d8af86faaa5e20373016c7225",
                    SafariCavern.DRIFTLING),
            // Shyworm は頭と胴体で skull が違う
            Map.entry("b4287b8a0a642dac535a6ee29459efd17d5cee1eb359e436bc8e7abba3da14b7",
                    SafariCavern.SHYWORM),
            Map.entry("a684e00e7394cb0c84c082e4dbc7c7e91ea67f6bc718a1aace7f99596b65d422",
                    SafariCavern.SHYWORM),
            Map.entry("fc63cd0d480971a7beae5fd503e5d51658cd906330843cbad92018f5b98b4fe5",
                    SafariCavern.CHUCKWALLA),
            Map.entry("a89a76deedd42b410344100df2fa79b6eeac7e6f287745d656179368340ffade",
                    SafariCavern.FLITTER));

    // 被っているスキンから引いた結果を覚えておく。
    // 毎 tick 見るので、base64 の展開を繰り返さないようにする。
    // 見つからなかったことも覚えるので、空の Optional で持つ
    private static final Map<String, Optional<MobVisual>> HEAD_SKIN_CACHE = new HashMap<>();
    // 覚えておくスキンの上限。見たことのないスキンが増え続けても溢れないようにする
    private static final int MAX_HEAD_SKIN_CACHE = 512;

    // 見た目を、ヘッドを被せたアーマースタンドの連なりで作っている Sea Creature。
    // 当たり判定は別にある透明なモブだが、見た目とは大きさが合わないので触らない。
    // ヘッド１つで一体の種は入れない。
    // スキン照合が周囲の同じヘッドを集めるので、2体湧いたときに1体としてまとめられてしまう
    // 表示の作り(判定ではない)
    public static final List<MobVisual> HEAD_BODY_TARGETS = List.of(
            MobVisual.SeaCreature.TITANOBOA, MobVisual.SeaCreature.WIKI_TIKI);

    // 連なりをたどらず、登録済みのスキンを被ったヘッドだけを集める Sea Creature。
    // Titanoboa は長く伸びるので、連なりをたどると節の間隔で切れうる。
    // 頭と胴体の skull が分かっているので、スキン照合だけで全身を集められる。
    // Wiki Tiki のように積み上がっているだけの種は、連なりで個体を分けられるのでここに入れない
    // 表示の作り(判定ではない)
    private static final List<MobVisual> HEAD_SKIN_GROUPED_TARGETS = List.of(
            MobVisual.SeaCreature.TITANOBOA);

    // 複数のヘッドで1体を成すモブの、頭にあたる skull。
    // 胴体の節と見分けて、Tracer とネームプレートを頭に出すために使う
    // 表示の作り(判定ではない)
    private static final Map<MobVisual, String> HEAD_ANCHOR_SKINS = Map.of(
            MobVisual.SeaCreature.TITANOBOA,
            "645f2c0bbfe3b8b19b7452072db69a5f59da38ff61415545156e5701e1be756d",
            TorrhusCanyon.WATER_SNAKE,
            "1b06d15155c60d0f0f267be05c8436c5a1e764a8c8aca236bb37223f49d9a1d0",
            SafariCavern.SHYWORM,
            "b4287b8a0a642dac535a6ee29459efd17d5cee1eb359e436bc8e7abba3da14b7");

    // ヘッドのスキンで見分ける Sea Creature。
    // スキンはネームタグより遠くまで届くので、
    // ネームタグがあるのに登録済みのスキンが1つも見つからないのは、
    // Hypixel 側で差し替えられたときだけ
    private static final Set<MobVisual> HEAD_SKIN_SPECIES = new HashSet<>(HEAD_SKIN_TARGETS.values());
    // スキンが見つからないことをすでに知らせたネームタグ。
    // 同じネームタグで毎 tick 出さないために覚えておく。
    // エンティティが消えれば勝手に落ちるよう、弱参照で持つ
    private static final Set<Entity> missingVisualReported =
            Collections.newSetFromMap(new WeakHashMap<>());
    // スキンが見つからないネームタグを、いつから見ているか。
    // 同じく弱参照で持つ
    private static final Map<Entity, MissingWindow> missingVisualWindows = new WeakHashMap<>();
    // 知らせるまでに待つ時間(ミリ秒)。
    // ラグでヘッドがまだ届いていないだけのときに反応しないようにする
    private static final long MISSING_VISUAL_GRACE_MS = 3000;
    // この時間だけ途切れたら、間で見つかっていたとみなして数え直す(ミリ秒)
    private static final long MISSING_VISUAL_GAP_MS = 500;

    // Sea Creature のうち、ネームタグを待たずにエンティティ型だけで判別してよいもの。
    // Special タブの面々は、バニラ相当の同じ型のモブと取り違える恐れがあるため入れない。
    // それらはネームタグを見つけてから、その直下の該当型を本体にする(seaCreatureVisual)
    // 判定: 4 型 + エリア
    public static final List<MobVisual> SEA_CREATURE_TYPE_TARGETS = List.of(
            MobVisual.SeaCreature.SILKBREEZE, MobVisual.SeaCreature.THUNDER,
            MobVisual.SeaCreature.LORD_JAWBUS, MobVisual.SeaCreature.REINDRAKE,
            MobVisual.SeaCreature.THE_LOCH_EMPEROR, MobVisual.SeaCreature.TITANOBOA,
            MobVisual.SeaCreature.BLUE_RINGED_OCTOPUS, MobVisual.SeaCreature.PUDDLE_JUMPER,
            MobVisual.SeaCreature.FROG_PRINCE, MobVisual.SeaCreature.GIANT_ISOPOD,
            MobVisual.SeaCreature.WIKI_TIKI, MobVisual.SeaCreature.FIERY_SCUTTLER,
            MobVisual.SeaCreature.WATER_HYDRA);

    // Sea Creature の名を含むが、その Sea Creature ではないネームタグ。
    // Wiki Tiki は戦闘中に "Wiki Tiki Laser Totem" を湧かせる。
    // 名前に本体の名が丸ごと入っているので、単語単位の照合では弾けない
    private static final Pattern SEA_CREATURE_DECOYS = Pattern.compile("(?i)Wiki Tiki Laser Totem");

    // ネームタグは "[Lv100] Water Hydra 500k/500k❤" のような形。名前部分を単語単位で照合する。
    // "Water Worm" は "Poisoned Water Worm" の一部なので、長い名前から先に照合する
    private static final Map<MobVisual, Pattern> SEA_CREATURE_PATTERNS = new LinkedHashMap<>();
    static {
        List<MobVisual> ordered = new ArrayList<>(SEA_CREATURE_TARGETS);
        ordered.sort(Comparator.comparingInt((MobVisual target) -> target.plainLabel().length()).reversed());
        for (MobVisual target : ordered) {
            // エリア動物として型で判別できる種は、ネームタグを待たない。
            // 両方で拾うと同じ個体を二重に扱うことになる
            if (AREA_ANIMAL_TARGETS.contains(target)) continue;

            SEA_CREATURE_PATTERNS.put(target,
                    Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(target.plainLabel()) + "(?![A-Za-z])"));
        }
    }

    // ヘッドが複数積み重なって1体を成すモブ。まとめて1体として扱う
    // 表示の作り(判定ではない)
    public static final List<MobVisual> HEAD_CHAIN_TARGETS = List.of(
            TorrhusCanyon.SNEAKY_TIKI, TorrhusCanyon.SHRIEKY_TIKI, TorrhusCanyon.CHEEKY_TIKI);

    // Torrhus Canyon の、ネームタグから引くモブ。
    // ヘッド系の表示はスキン側に移したので、ここに残しているのは
    // スキンが見つからないときに知らせるため。
    // Grizzly Bear の本命は CANYON_NAMED_PLAYER_TARGETS の実体名照合で、
    // ここに残しているのはその取りこぼし用の保険
    // 判定には使わない。差し替えを知らせるためだけ
    public static final List<MobVisual> CANYON_NAMED_TARGETS = List.of(
            TorrhusCanyon.GRIZZLY_BEAR,
            TorrhusCanyon.ANT, TorrhusCanyon.QUEEN_ANT, TorrhusCanyon.WATER_SNAKE,
            TorrhusCanyon.SNEAKY_TIKI, TorrhusCanyon.SHRIEKY_TIKI, TorrhusCanyon.CHEEKY_TIKI);

    // Torrhus Canyon の、プレイヤーエンティティ型(NPC)のモブ。
    // ネームタグ(ArmorStand)より読み込み範囲が広いので、実体名で直接探す
    // 判定: 2 実体名
    public static final List<MobVisual> CANYON_NAMED_PLAYER_TARGETS = List.of(
            TorrhusCanyon.GRIZZLY_BEAR);

    // Torrhus Canyon の、skull を被せたアーマースタンドで見た目を作っているモブ。
    // 節ごとに透明なスライムが当たり判定として付いているが、見分けにはスキンを使う
    // 判定: 1 ヘッドのスキン
    public static final List<MobVisual> CANYON_HEAD_TARGETS = List.of(
            TorrhusCanyon.ANT, TorrhusCanyon.QUEEN_ANT, TorrhusCanyon.WATER_SNAKE);

    // ヘッドのスキンで見分ける Torrhus Canyon のモブ。
    // 表示はスキン側の走査で行うので、ネームタグは知らせるためだけに使う
    // 判定: 1 ヘッドのスキン
    private static final List<MobVisual> CANYON_SKIN_TARGETS = new ArrayList<>(CANYON_HEAD_TARGETS);
    static {
        CANYON_SKIN_TARGETS.addAll(HEAD_CHAIN_TARGETS);
    }

    // ヘッドのスキンで見分ける Critter Safari のモブ。
    // Gazer はアーマースタンドが、残りは見た目の Display が skull を被っている
    // 判定: 1 ヘッドのスキン
    private static final List<MobVisual> SAFARI_SKIN_TARGETS = List.of(
            SafariHaunted.GAZER, SafariHaunted.GIMMIEGOLD,
            SafariIcy.MANTIS_SHRIMP, SafariIcy.TROODON,
            SafariCavern.DRIFTLING, SafariCavern.SHYWORM,
            SafariCavern.CHUCKWALLA, SafariCavern.FLITTER);

    // 型で判定するモブを、ネームタグの名前から引く表。
    // 判定には使わず、近くに実物がいないことを知らせるためだけに使う。
    // スキンや実体名で判定する面々は専用の経路があるので除く
    private static final Map<String, MobVisual> TYPE_NAMED_TARGETS = new HashMap<>();
    static {
        List<MobVisual> typed = new ArrayList<>(AREA_ANIMAL_TARGETS);
        typed.addAll(SAFARI_TYPE_TARGETS);
        typed.addAll(SEA_CREATURE_TYPE_TARGETS);
        typed.addAll(SHULKER_TARGETS);
        for (MobVisual target : typed) {
            if (SAFARI_SKIN_TARGETS.contains(target)) continue;
            if (CANYON_SKIN_TARGETS.contains(target)) continue;
            if (HEAD_SKIN_SPECIES.contains(target)) continue;
            if (SEA_CREATURE_PLAYER_TARGETS.contains(target)) continue;

            TYPE_NAMED_TARGETS.put(target.plainLabel().toLowerCase(Locale.ROOT), target);
        }
    }

    // ネームタグの先頭に付く "[Lv58] " などの括弧
    private static final Pattern NAMETAG_PREFIX = Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)+");

    // Critter Safari の、ネームタグから引くモブ。
    // スキンで見分ける面々の表示は型側の走査に任せているので、
    // ここではスキンが見つからないときに知らせるために使う。
    // "Gazer" のように短い名前があるので、長い名前から先に照合する
    private static final Map<MobVisual, Pattern> SAFARI_NAMED_PATTERNS = new LinkedHashMap<>();
    static {
        List<MobVisual> ordered = new ArrayList<>(SAFARI_NAMED_TARGETS);
        ordered.addAll(SAFARI_SKIN_TARGETS);
        ordered.sort(Comparator.comparingInt((MobVisual target) -> target.plainLabel().length()).reversed());
        for (MobVisual target : ordered) {
            SAFARI_NAMED_PATTERNS.put(target,
                    Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(target.plainLabel()) + "(?![A-Za-z])"));
        }
    }

    // ネームタグは "[Lv58] Parched 25,000/25,000❤" のような形。名前部分を単語単位で照合する。
    // 単純な部分一致だと「Ant」が「Giant Isopod」に引っかかってしまうため、前後が英字でないことを条件にする。
    // "Ant" は "Queen Ant" の一部でもあるので、長い名前から先に照合する
    private static final Map<MobVisual, Pattern> CANYON_NAMED_PATTERNS = new LinkedHashMap<>();
    static {
        List<MobVisual> ordered = new ArrayList<>(CANYON_NAMED_TARGETS);
        ordered.sort(Comparator.comparingInt((MobVisual target) -> target.plainLabel().length()).reversed());
        for (MobVisual target : ordered) {
            CANYON_NAMED_PATTERNS.put(target,
                    Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(target.plainLabel()) + "(?![A-Za-z])"));
        }
    }

    // Torrhus Canyon のハチのうち Honeybuzz と Pollendart。
    // 同じ大きさの Bee 型で見分けが付かないため、ネームタグの名前で振り分ける
    // (Beeheemoth はミニボス級に大きいので大きさで判別でき、こちらには含めない)
    // 判定: 6 ネームタグの名前のみ
    public static final List<MobVisual> CANYON_BEE_TARGETS = List.of(
            TorrhusCanyon.HONEYBUZZ, TorrhusCanyon.POLLENDART);

    // Ashfang のフォロワー3種（ハイライト・トレーサーの個別設定付き）
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
        ClientTickEvents.END_CLIENT_TICK.register(client -> updateHighlights(client));
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
        customGlowColors.clear();
        nameplateEntities.clear();
        // 覚えているハチのうち、消えた個体だけ捨てる
        canyonBeeNames.entrySet().removeIf(e -> e.getKey().isRemoved());
        // 型で消せない見た目エンティティは毎 tick 作り直す
        highlightedEntities.removeAll(rebuiltVisuals);
        rebuiltVisuals.clear();
        nametagClaimedEntities.clear();
        healthNameTags.clear();
        seaCreaturePartsShown.clear();
        // 知らせ済みの記録は消えた個体だけ捨てる。生きている間は出し直さない
        announcedSeaCreatures.removeIf(Entity::isRemoved);
        shulkerClaimedEntities.clear();
        renderAnchors.clear();
        headOnlyGlowEntities.clear();
        tracerEntities.clear();
        tracerNearest.clear();
        ashfangTracerTarget = null;

        if (client.level == null || client.player == null) {
            highlightedEntities.clear();
            canyonBeeNames.clear();
            invisibugEntities.clear();
            pendingCritParticles.clear();
            // ワールド遷移をまたいで古い計測・ラッチを持ち越さない
            resetCrimsonBossTracking();
            return;
        }

        // Golden Fish: Mob Visuals とは別の機能なので、ここもハイライトだけにする。
        // 下の早期戻りより前に置き、Mob Visuals を全て切っていても動くようにする
        boolean goldenFishVisuals = ModConfig.INSTANCE.fishing.highlightGoldenFish
                || ModConfig.INSTANCE.fishing.tracerGoldenFish;
        if (goldenFishVisuals && GoldenFishHandler.isActive()) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!matchesHeadSkin(entity, GOLDEN_FISH_SKIN)) continue;

                if (ModConfig.INSTANCE.fishing.tracerGoldenFish) {
                    tracerEntities.put(entity, 0xFF000000 | GOLDEN_FISH_GLOW_COLOR);
                }
                if (!ModConfig.INSTANCE.fishing.highlightGoldenFish) continue;

                // marker でないアーマースタンドは、そのままだと腕・胴・脚の輪郭まで出てしまう
                if (entity instanceof ArmorStand stand && !stand.isMarker()) headOnlyGlowEntities.add(entity);

                highlightedEntities.add(entity);
                // アーマースタンドは型による一括削除ができないので、毎 tick 作り直す集合へ入れる
                rebuiltVisuals.add(entity);
                customGlowColors.put(entity, GOLDEN_FISH_GLOW_COLOR);
            }
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

        boolean dragonPresent = isTheEnd && GameState.Dragon.type != null;
        boolean glowDragon = dragonPresent && TheEnd.DRAGON.highlight();
        boolean scanDragon = dragonPresent && TheEnd.DRAGON.anyEnabled();

        boolean isCrimsonIsle = GameState.Server.isCrimsonIsle();
        // ボスの存在判定はワールドテキストや Status HUD のスポーン表示にも使うため、
        // Mob Visuals で対象から外していても Crimson Isle にいる間は常に走らせる
        boolean scanCrimsonBosses = isCrimsonIsle;
        boolean scanMagmaGlare = isCrimsonIsle
                && "Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)
                && CrimsonIsle.MAGMA_GLARE.anyEnabled();
        boolean scanAshfangFollowers = isCrimsonIsle
                && ASHFANG_FOLLOWERS.stream().anyMatch(f -> f.enableHighlight().get() || f.enableTracer().get() || f.enableNameplate().get());

        // Wumpa: Safari に出現するラヴェジャーは Wumpa しかいないため、型だけで本体と判定できる
        ModConfig.ForagingCategory foraging = ModConfig.INSTANCE.foraging;
        boolean isSafari = GameState.Server.isSafari();
        boolean glowWumpa = isSafari && SafariIcy.WUMPA.highlight();
        boolean scanWumpa = isSafari && SafariIcy.WUMPA.anyEnabled();
        // Doomspiral: Safari に出現するウォーデンは Doomspiral しかいないため、型だけで本体と判定できる
        boolean glowDoomspiral = isSafari && SafariHaunted.DOOMSPIRAL.highlight();
        boolean scanDoomspiral = isSafari && SafariHaunted.DOOMSPIRAL.anyEnabled();

        // 動いている Hideonfloor / Hideonwall はシュルカーが消えて当たり判定だけになり、
        // この走査で拾うことになる。そのため Hideon 系の表示設定も条件に入れる
        boolean scanSafariTypes = isSafari && (SAFARI_TYPE_TARGETS.stream().anyMatch(MobVisual::anyEnabled)
                || SafariForest.HIDEONFLOOR.anyEnabled() || SafariHaunted.HIDEONWALL.anyEnabled());
        boolean scanSafariNamed = isSafari
                && (SAFARI_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled)
                        || SAFARI_SKIN_TARGETS.stream().anyMatch(MobVisual::anyEnabled));

        // Shulker: Hideon 系が敵として出現する3エリアでのみ探索する
        boolean isShulkerArea = GameState.Server.isMoongladeMarsh()
                || GameState.Server.isTorrhusCanyon()
                || isSafari;
        boolean scanShulker = isShulkerArea && SHULKER_TARGETS.stream().anyMatch(MobVisual::anyEnabled);

        // Moonglade Marsh / Torrhus Canyon では、該当するエンティティ型がこれらの固有モブしか
        // 存在しないため、ネームタグを読まずに型(一部は変種)だけで判定できる
        boolean isAreaAnimalArea = GameState.Server.isMoongladeMarsh()
                || GameState.Server.isTorrhusCanyon() || GameState.Server.isLotusAtoll();
        boolean scanAreaAnimals = isAreaAnimalArea && AREA_ANIMAL_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanCanyonBees = GameState.Server.isTorrhusCanyon()
                && CANYON_BEE_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        // Torrhus Canyon のヘッド系モブは marker のアーマースタンドと透明なスライムの組で作られていて、
        // 装飾として置かれたアーマースタンドとは作りが違うため、ネームタグを読まずに判定できる
        boolean scanCanyonHeads = GameState.Server.isTorrhusCanyon()
                && (CANYON_HEAD_TARGETS.stream().anyMatch(MobVisual::anyEnabled)
                        || HEAD_CHAIN_TARGETS.stream().anyMatch(MobVisual::anyEnabled));
        boolean isCrystalHollows = GameState.Server.isCrystalHollows();
        // Boss Corleone の存在判定はスポーン通知にも使うため、
        // Mob Visuals で対象から外していても Crystal Hollows にいる間は常に走らせる
        boolean scanCrystalNamed = isCrystalHollows;
        boolean scanCrimsonNamed = isCrimsonIsle
                && CRIMSON_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanMarshNamed = GameState.Server.isMoongladeMarsh()
                && MARSH_NAMED_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        // Sea Creature は釣れる場所が決まっているので、対象の絞り込み(anyEnabled)が
        // そのままエリアの絞り込みになる
        boolean scanSeaCreatures = SEA_CREATURE_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanSeaCreatureTypes = SEA_CREATURE_TYPE_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
        boolean scanSeaCreaturePlayers = SEA_CREATURE_PLAYER_TARGETS.stream().anyMatch(MobVisual::anyEnabled);
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

        // 条件が無効になったカテゴリのエンティティを削除(Glowing対象かどうかで判定する)
        if (!glowGolem)       highlightedEntities.removeIf(e -> e instanceof IronGolem);
        if (!glowBroodmother) highlightedEntities.removeIf(e -> e instanceof Spider);
        if (!glowDragon)      highlightedEntities.removeIf(e -> e instanceof EnderDragon);
        if (!glowWumpa)       highlightedEntities.removeIf(e -> e instanceof Ravager);
        if (!glowDoomspiral)  highlightedEntities.removeIf(e -> e instanceof Warden);
        // シュルカーはエリアと体色で対象が変わるため、毎 tick 作り直す
        highlightedEntities.removeIf(e -> e instanceof Shulker);
        // エリア固有モブもエリアと変種で対象が変わるため、同じく毎 tick 作り直す
        highlightedEntities.removeIf(EntityHighlightManager::isAreaAnimalEntity);
        // Sea Creature もエリアと設定で対象が変わるため、同じく毎 tick 作り直す
        highlightedEntities.removeIf(EntityHighlightManager::isSeaCreatureTypeEntity);
        // Invisibug も設定で対象が変わるため、毎 tick 作り直す
        highlightedEntities.removeAll(invisibugEntities);
        if (!glowArachne)     highlightedEntities.removeAll(arachneEntities);
        if (!scanArachne)     arachneEntities.clear();

        // ワールドから削除済みのエンティティを削除
        highlightedEntities.removeIf(Entity::isRemoved);
        arachneEntities.removeIf(Entity::isRemoved);

        // Crimson Isle にいない場合は isDetected をリセット
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : CRIMSON_BOSSES) boss.setIsDetected().accept(false);
        }

        if (!isCrystalHollows) CorleoneHandler.reset();

        if (!scanGolem && !scanBroodmother && !scanArachne && !scanDragon && !scanCrimsonBosses && !scanMagmaGlare && !scanAshfangFollowers && !scanWumpa && !scanDoomspiral && !scanShulker && !scanAreaAnimals && !scanCanyonBees && !scanInvisibug && !scanCanyonHeads && !scanCanyonNamed && !scanMarshNamed && !scanCrimsonNamed && !scanCrystalNamed && !scanSafariTypes && !scanSafariNamed && !scanSeaCreatures && !scanSeaCreatureTypes && !scanSeaCreaturePlayers) return;

        boolean[] bossFound = new boolean[CRIMSON_BOSSES.size()];
        // Boss Corleone を見つけたか。ネームタグ経由とプレイヤー名照合のどちらで見つけても立てる
        boolean corleoneFound = false;
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

                Vec3 center = Vec3.atCenterOf(ModConstants.ASHFANG_POS);
                AABB area = new AABB(
                        center.x - ASHFANG_AREA_XZ, center.y - CRIMSON_AREA_Y, center.z - ASHFANG_AREA_XZ,
                        center.x + ASHFANG_AREA_XZ, center.y + CRIMSON_AREA_Y, center.z + ASHFANG_AREA_XZ);
                List<Blaze> blazes = new ArrayList<>(client.level.getEntitiesOfClass(Blaze.class, area,
                        e -> !crimsonBossEntities.containsKey(e)));
                if (blazes.size() < ASHFANG_BLAZE_COUNT) break;
                blazes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));

                List<Blaze> body = blazes.subList(0, ASHFANG_BLAZE_COUNT);
                // 本体は縦に連なった2体。ネームプレートと Tracer は上側の1体だけに出す
                Entity plateTarget = body.get(0);
                for (Blaze blaze : body) {
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


        for (Entity entity : client.level.entitiesForRendering()) {
            Component customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            // ネームプレートに体力を出すため、体力付きのネームタグを控えておく
            if (HEALTH_PATTERN.matcher(nameStr).find()) healthNameTags.add(entity);

            if (scanBroodmother && ModConstants.containsIgnoreCase(nameStr, "Broodmother")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<Spider> spiders = client.level.getEntitiesOfClass(Spider.class, searchBox, e -> true);
                Entity closest = getClosestEntity(spiders, entity);
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                Entity broodmotherVisual = closest != null ? closest : entity;
                if (closest != null) {
                    if (glowBroodmother) highlightedEntities.add(closest);
                } else {
                    reportMissingBody(client, entity, SpidersDen.BROODMOTHER);
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
                } else {
                    reportMissingBody(client, entity, SpidersDen.ARACHNE);
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
                } else {
                    reportMissingBody(client, entity, SpidersDen.ARACHNE_BROOD);
                }
                registerTracer(broodVisual, SpidersDen.ARACHNE_BROOD);
                if (SpidersDen.ARACHNE_BROOD.nameplate()) {
                    nameplateEntities.put(broodVisual, BossNameplateRenderer.buildLabel("§d§lArachne's Brood", null));
                }
            }

            if (scanDragon && ModConstants.containsIgnoreCase(nameStr, "Dragon")) {
                AABB searchBox = entity.getBoundingBox().inflate(32.0);
                List<EnderDragon> dragons = client.level.getEntitiesOfClass(EnderDragon.class, searchBox, e -> true);
                Entity closest = getClosestEntity(dragons, entity);
                // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                if (closest != null) {
                    if (glowDragon) highlightedEntities.add(closest);
                } else {
                    reportMissingBody(client, entity, TheEnd.DRAGON);
                }
                registerTracer(closest != null ? closest : entity, TheEnd.DRAGON, dragonTracerColor());
            }

            // 型で判定するモブ。ネームタグは判定に使わないが、
            // 名前が分かっているのに近くに実物がいなければ、作りが変わったとみる
            MobVisual typeNamed = TYPE_NAMED_TARGETS.get(nameTagSpecies(nameStr).toLowerCase(Locale.ROOT));
            if (typeNamed != null && typeNamed.anyEnabled()
                    && !hasTypeTargetNear(client, entity, typeNamed)) {
                reportMissingBody(client, entity, typeNamed);
            }

            // Critter Safari のネームタグ判定モブ
            if (scanSafariNamed) {
                for (Map.Entry<MobVisual, Pattern> safariEntry : SAFARI_NAMED_PATTERNS.entrySet()) {
                    MobVisual target = safariEntry.getKey();
                    if (!target.anyEnabled()) continue;
                    // 自分がいるバイオームのモブだけを対象にする
                    if (!inSafariBiome(client.player, target)) continue;
                    if (!safariEntry.getValue().matcher(nameStr).find()) continue;

                    // スキンで見分ける種は、表示は型側の走査に任せる。
                    // ネームタグが届いているならスキンも届いているので、
                    // 見つからないときだけ差し替えを知らせる
                    if (SAFARI_SKIN_TARGETS.contains(target)) {
                        if (!hasSafariSkinNear(client, entity, target)) {
                            reportMissingHeadSkin(client, entity, target);
                        }
                        break;
                    }

                    // 実体名照合が本命で、そちらはネームタグより遠くまで届く。
                    // 見つかっているならそちらに任せ、ここでは何もしない
                    if (hasNamedPlayerMob(client, target)) break;

                    // 見つからないのは実体名が変わったとき。知らせたうえで、
                    // 座標さえあれば描ける Tracer とネームプレートはネームタグに出す
                    reportMissingNamedPlayer(client, entity, target);
                    registerTracer(entity, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                    }
                    break;
                }
            }

            // Crystal Hollows のネームタグ判定モブ
            if (scanCrystalNamed) {
                for (MobVisual target : CRYSTAL_NAMED_TARGETS) {
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    // 存在の判定はスポーン通知にも使うので、表示設定より先に立てる
                    if (target == CrystalHollows.BOSS_CORLEONE) corleoneFound = true;
                    if (!target.anyEnabled()) break;

                    // 実体名照合が本命で、そちらはネームタグより遠くまで届く。
                    // 見つかっているならそちらに任せ、ここでは何もしない
                    if (hasNamedPlayerMob(client, target)) break;

                    // 見つからないのは実体名が変わったとき。知らせたうえで、
                    // 座標さえあれば描ける Tracer とネームプレートはネームタグに出す
                    reportMissingNamedPlayer(client, entity, target);
                    registerTracer(entity, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                    }
                    break;
                }
            }

            // Crimson Isle のネームタグ判定モブ
            if (scanCrimsonNamed) {
                for (MobVisual target : CRIMSON_NAMED_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    // 実体名照合が本命で、そちらはネームタグより遠くまで届く。
                    // 見つかっているならそちらに任せ、ここでは何もしない
                    if (hasNamedPlayerMob(client, target)) break;

                    // 見つからないのは実体名が変わったとき。知らせたうえで、
                    // 座標さえあれば描ける Tracer とネームプレートはネームタグに出す
                    reportMissingNamedPlayer(client, entity, target);
                    registerTracer(entity, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                    }
                    break;
                }
            }

            // Moonglade Marsh のネームタグ判定モブ
            if (scanMarshNamed) {
                for (MobVisual target : MARSH_NAMED_TARGETS) {
                    if (!target.anyEnabled()) continue;
                    if (!ModConstants.containsIgnoreCase(nameStr, target.plainLabel())) continue;

                    // 見た目は決まった skull を被せたアーマースタンド。
                    // 「ネームタグの下にいる何か」を本体とみなす保険は置かない
                    Entity visualTarget = headStandWithSkinUnderNameTag(client, entity, MARSH_BUG_HEAD_SKIN);
                    // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                    // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                    Entity visual = visualTarget != null ? visualTarget : entity;
                    if (visualTarget != null) {
                        if (target.highlight()) registerHighlight(visualTarget, target);
                    } else {
                        // ネームタグが届いているならヘッドも届いている。
                        // 見つからないのはスキンが変わったときなので、その場で知らせる
                        reportMissingHeadSkin(client, entity, target);
                    }
                    registerTracer(visual, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(visual, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(visual, target)));
                    }
                    break;
                }
            }

            // Sea Creature。どのエリアでも湧くので、ネームタグの名前だけで判定する。
            // 本体の名を含む別のモブは、照合に入る前に弾く
            if (scanSeaCreatures && !SEA_CREATURE_DECOYS.matcher(nameStr).find()) {
                for (Map.Entry<MobVisual, Pattern> seaEntry : SEA_CREATURE_PATTERNS.entrySet()) {
                    MobVisual target = seaEntry.getKey();
                    if (!target.anyEnabled()) continue;
                    if (!seaEntry.getValue().matcher(nameStr).find()) continue;

                    // プレイヤーエンティティ型が実体名で見つかっているなら、そちらに任せる
                    if (SEA_CREATURE_PLAYER_TARGETS.contains(target)
                            && hasNamedPlayerMob(client, target)) break;

                    Entity visualTarget = seaCreatureVisual(client, entity, target);
                    if (visualTarget != null) {
                        applySeaCreatureVisuals(client, target, visualTarget);
                    } else {
                        // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                        // ネームタグ自体を対象にして、座標さえあれば描ける Tracer とネームプレートは出す
                        registerTracer(entity, target);
                        if (target.nameplate()) {
                            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                            nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                        }
                        announceSeaCreature(client, target, entity);

                        // スキンで見分ける種は、ネームタグが届いているならヘッドも届いている。
                        // 見つからないのはスキンが変わったときなので、その場で知らせる
                        if (HEAD_SKIN_SPECIES.contains(target)) {
                            reportMissingHeadSkin(client, entity, target);
                        } else if (SEA_CREATURE_PLAYER_TARGETS.contains(target)) {
                            reportMissingNamedPlayer(client, entity, target);
                        } else {
                            // 残りはネームタグの名前で種を決め、その型のモブを本体とする。
                            // 型が変われば見つからなくなるので知らせる
                            reportMissingBody(client, entity, target);
                        }
                    }
                    break;
                }
            }

            // Torrhus Canyon のネームタグ判定モブ。本体の探し方だけが種類ごとに違う
            if (scanCanyonNamed) {
                for (Map.Entry<MobVisual, Pattern> canyonEntry : CANYON_NAMED_PATTERNS.entrySet()) {
                    MobVisual target = canyonEntry.getKey();
                    if (!target.anyEnabled()) continue;
                    if (!canyonEntry.getValue().matcher(nameStr).find()) continue;

                    // ヘッド系はスキンで見分けられるので、表示は canyonHeads 側に任せる。
                    // ネームタグが届いているならヘッドも届いているので、
                    // 見つからないときだけスキンの差し替えを知らせる
                    if (CANYON_SKIN_TARGETS.contains(target)) {
                        if (skinnedHeadUnderNameTag(client, entity, target) == null) {
                            reportMissingHeadSkin(client, entity, target);
                        }
                        break;
                    }

                    // 残るのは Grizzly Bear だけで、Barbarian Duke X などと同じプレイヤー型のモブ。
                    // 実体名照合が本命で、そちらはネームタグより遠くまで届く。
                    // 見つかっているならそちらに任せ、ここでは何もしない
                    if (hasNamedPlayerMob(client, target)) break;

                    // 見つからないのは実体名が変わったとき。知らせたうえで、
                    // 座標さえあれば描ける Tracer とネームプレートはネームタグに出す
                    reportMissingNamedPlayer(client, entity, target);
                    registerTracer(entity, target);
                    if (target.nameplate()) {
                        String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                        nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
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
                    Entity bee = getClosestEntity(client.level.getEntitiesOfClass(Bee.class, entity.getBoundingBox().inflate(4.0), e -> !isBeeheemoth(e)), entity);
                    if (bee != null) {
                        canyonBeeNames.put(bee, target);
                    } else {
                        reportMissingBody(client, entity, target);
                        // 本体がまだ届いていない場合は、ネームタグ自体を対象にして
                        // 座標さえあれば描ける Tracer とネームプレートだけ出す
                        registerTracer(entity, target);
                        if (target.nameplate()) {
                            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                            nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
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
                        // Barbarian Duke X と Mage Outlaw は実体名で探すので、
                        // ネームタグがあるのに見つからないのは実体名が変わったとき
                        if (visualTarget == null && playerBossEntityName(boss.nameTag()) != null) {
                            reportMissingNamedPlayer(client, entity, boss.nameTag());
                        }
                        // 本体はネームタグより手前でしか届かないことがある。見つからない場合は
                        // ネームタグ自体を対象にして、Tracer とネームプレートだけでも出す
                        Entity bossVisual = visualTarget != null ? visualTarget : entity;
                        if (visualTarget != null) {
                            if (glowBoss) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, boss);
                        } else if (playerBossEntityName(boss.nameTag()) == null) {
                            reportMissingBody(client, entity, boss.nameTag());
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
                        } else {
                            reportMissingBody(client, entity, follower.nameTag());
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

        // Golem(Endstone Protector): The End に存在する IronGolem はこのボスしかいないため、
        // Stage 5 の間はネームタグを読めなくても IronGolem 自体を本体とみなす。
        // ネームプレートは1体だけ出したいので、最初に見つかった個体を対象にする
        if (scanGolem) {
            Entity plateGolem = null;
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof IronGolem)) continue;
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
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof Ravager)) continue;
                if (glowWumpa) highlightedEntities.add(entity);
                registerTracer(entity, SafariIcy.WUMPA);
                if (SafariIcy.WUMPA.nameplate()) {
                    nameplateEntities.put(entity, BossNameplateRenderer.buildCapsuleLabel("§b§lWumpa",
                            capsuleLabel(GameState.CritterSafari.wumpaCapsuleHits)));
                }
            }
        }

        // Doomspiral: Wumpa と同じく、Safari のウォーデンは Doomspiral のみなので型だけで判定する
        if (scanDoomspiral) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof Warden)) continue;
                if (glowDoomspiral) highlightedEntities.add(entity);
                registerTracer(entity, SafariHaunted.DOOMSPIRAL);
                if (SafariHaunted.DOOMSPIRAL.nameplate()) {
                    nameplateEntities.put(entity, BossNameplateRenderer.buildCapsuleLabel("§5§lDoomspiral",
                            capsuleLabel(GameState.Doomspiral.capsuleHits)));
                }
            }
        }

        // Hideon 系の当たり判定は、その表示設定に関わらず必ず結び付けておく。
        // Hideon 系を消していても、余った当たり判定が Duplico として出てしまうため
        if (GameState.Server.isSafari() && (scanShulker || scanSafariTypes)) {
            claimShulkerHitboxes(client);
        }

        // Shulker: ネームタグを読まず、エリアと体色から Hideon 系の呼び名と色を決める
        if (scanShulker) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof Shulker shulker)) continue;
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
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                }
            }
        }

        // Coralot / Mossybit / Blue Jay / Dustybit / Sepialot / Goldolot:
        // エリアとエンティティ型(ウーパールーパーのみ変種も)から呼び名と色を決める
        if (scanAreaAnimals) {
            for (Entity entity : client.level.entitiesForRendering()) {
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
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                }
                // Nessie のように、ここで拾う Sea Creature もいる。
                // タイトルはその種のときだけ出る(announceSeaCreature)
                announceSeaCreature(client, target, entity);
            }
        }

        // エンティティ型が分かっている Sea Creature。エリアで絞ってから型で決める
        if (scanSeaCreatureTypes) {
            for (Entity entity : client.level.entitiesForRendering()) {
                // ネームタグ経由で別のモブと確定しているものは飛ばす
                if (nametagClaimedEntities.contains(entity)) continue;
                MobVisual target = seaCreatureTypeTarget(entity);
                if (target == null || !target.anyEnabled()) continue;

                applySeaCreatureVisuals(client, target, entity);
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
                    nameplateEntities.put(bee, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(bee, target)));
                }
            }
        }

        // Critter Safari: エリア内で型が一意なモブは、型(オウムのみ変種も)だけで判定できる
        if (scanSafariTypes) {
            for (Entity scanned : client.level.entitiesForRendering()) {
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
                    Entity display = nearestOfType(client, entity, Display.class);
                    if (display == null) continue;
                    entity = display;
                }

                if (target.highlight()) registerHighlight(entity, target);
                registerTracer(entity, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                }
            }
        }

        // Torrhus Canyon: marker のヘッドと透明なスライムの組で作られたモブ。
        // Water Snake はヘッドが連なって1体を成すので、連なりごとまとめて扱う
        if (scanCanyonHeads) {
            Set<Entity> chained = new HashSet<>();
            for (Entity entity : client.level.entitiesForRendering()) {
                // ネームタグ経由で別のモブの一部と確定しているものは飛ばす
                if (nametagClaimedEntities.contains(entity)) continue;
                MobVisual target = canyonHeadTarget(client, entity);
                if (target == null) continue;

                // Water Snake と Tiki 系は、ヘッドの連なりで1体を成す
                if (target == TorrhusCanyon.WATER_SNAKE || HEAD_CHAIN_TARGETS.contains(target)) {
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
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
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
                    nameplateEntities.put(entity, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(entity, target)));
                }
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
                registerTracer(entity, TheEnd.DRAGON, dragonTracerColor());
                if (plateDragon == null) plateDragon = entity;
            }
            if (TheEnd.DRAGON.nameplate() && plateDragon != null) {
                String label = dragonColorCode(GameState.Dragon.type) + "§l" + GameState.Dragon.type + " Dragon";
                nameplateEntities.put(plateDragon, BossNameplateRenderer.buildLabel(label, GameState.Dragon.health));
            }
        }

        // Barbarian Duke X / Mage Outlaw: 名前が一致するプレイヤーを本体とみなす。
        // ネームタグが読み込まれない距離でも「存在するか」を判定できる
        if (isCrimsonIsle) {
            detectPlayerBoss(client, "Barbarian Duke X", BARBARIAN_ENTITY_NAME, bossFound);
            detectPlayerBoss(client, "Mage Outlaw", MAGE_OUTLAW_ENTITY_NAME, bossFound);
            detectCrimsonNamedMobs(client);
        }

        // Boss Corleone も同じくプレイヤーの名前で直接探す
        if (scanCrystalNamed) {
            if (detectNamedPlayerMobs(client, CRYSTAL_NAMED_TARGETS)) corleoneFound = true;
        }

        // Hideyho も同じくプレイヤーの名前で直接探す。
        // ネームタグ経由だと、ネームタグが届かない距離で見失ってしまう
        if (scanSafariNamed) {
            detectNamedPlayerMobs(client, SAFARI_NAMED_PLAYER_TARGETS);
        }

        // Grizzly Bear も同じ
        if (scanCanyonNamed) {
            detectNamedPlayerMobs(client, CANYON_NAMED_PLAYER_TARGETS);
        }

        // Sea Creature のプレイヤー型も同じく実体名で直接探す
        if (scanSeaCreaturePlayers) {
            detectNamedPlayerMobs(client, SEA_CREATURE_PLAYER_TARGETS);
        }

        // Bladesoul: Blaze + Wither Skeleton の合体構成。ネームタグに頼らず、スポーン地点周辺で
        // 「ほぼ同じ座標に重なった Wither Skeleton と Blaze」の組を本体と判定する
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                if (!"Bladesoul".equals(boss.nameTag())) continue;

                // Mob Visuals を切っていても、ワールドテキストと Status HUD の
                // スポーン表示に使うので探索自体は続ける。見た目は下で個別に出し分ける
                boolean glowBoss = boss.enableHighlight().get();
                boolean plateBoss = boss.enableNameplate().get();

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

        // Magma Glare: Kill the Magmas フェーズ中は本体が存在しないので、エリア内の MagmaCube は
        // すべて分裂した個体。ただし同フェーズには無害な Unstable Magma も湧いており、
        // そちらはバニラ自然湧きサイズの小さい個体なのでサイズで振り分ける。
        // ネームタグに依存しないので、個体数が多くても取りこぼさない
        if (scanMagmaGlare && GameState.MagmaBoss.inArena) {
            boolean glowGlare = CrimsonIsle.MAGMA_GLARE.highlight();
            boolean plateGlare = CrimsonIsle.MAGMA_GLARE.nameplate();
            for (MagmaCube cube : magmaCubesInArena(client, e -> e.getSize() > MAGMA_MAX_NATURAL_SIZE)) {
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

        // Scrappy に与える魚をハイライトする。Critter として使われている魚は除く。
        // Mob Visuals とは別の機能なので、線もネームプレートも出さずハイライトだけにする
        if (isSafari && ModConfig.INSTANCE.foraging.enableSafariFishHighlight) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof AbstractFish)) continue;
                // 透明な個体は別のモブの当たり判定。ネームタグ経由で確定しているものも同じ
                if (entity.isInvisible() || nametagClaimedEntities.contains(entity)) continue;
                // Critter として使われている魚は Mob Visuals の担当なので触らない
                if (safariTarget(client, entity) != null) continue;

                highlightedEntities.add(entity);
                customGlowColors.put(entity, SAFARI_FISH_GLOW_COLOR);
            }
        }

        // Rockmite Mound: Cavern Biome に隠れている Rockmite の巣。
        // Mob Visuals とは別の機能なので、ここもハイライトだけにする
        if (isSafari && ModConfig.INSTANCE.foraging.enableRockmiteMoundHighlight
                && inSafariCavern(client.player)) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof Display.ItemDisplay display)) continue;
                if (!inSafariCavern(entity)) continue;

                Display.ItemDisplay.ItemRenderState state = display.itemRenderState();
                if (state == null || !isRockmiteMound(state.itemStack())) continue;

                highlightedEntities.add(entity);
                customGlowColors.put(entity, ROCKMITE_MOUND_GLOW_COLOR);
            }
        }

        if (isCrystalHollows) CorleoneHandler.update(client, corleoneFound);
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

    // Crimson Isle のネームタグ判定モブのうち、プレイヤーエンティティ型のもの。
    // ネームタグ(アーマースタンド)より読み込み範囲が広いので、Barbarian Duke X / Mage Outlaw と
    // 同じくプレイヤーの名前で直接探す。ボスと違って複数体いるため、一致したものはすべて登録する
    private static void detectCrimsonNamedMobs(Minecraft client) {
        detectNamedPlayerMobs(client, CRIMSON_NAMED_TARGETS);
    }

    // プレイヤーエンティティ型のモブを、プレイヤーの名前で探して登録する。
    // 戻り値は1体でも見つかったかどうか。スポーン通知に使うため、表示設定とは切り離して返す
    private static boolean detectNamedPlayerMobs(Minecraft client, List<MobVisual> candidates) {
        boolean found = false;

        for (MobVisual target : candidates) {

            if (!canDetectNamedPlayerMob(client, target)) continue;

            for (Player player : client.level.players()) {
                if (player == client.player || !matchesNamedPlayerMob(target, player)) continue;

                found = true;
                if (target.highlight()) {
                    registerHighlight(player, target);
                    // 乗っている土台も見えているので、本体と一緒に光らせる。
                    // Tracer とネームプレートは、上に乗っている本体の側だけに出す
                    Entity mount = mountedNamedPlayerBase(target, player);
                    if (mount != null) registerHighlight(mount, target);
                }
                registerTracer(player, target);
                if (target.nameplate()) {
                    String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
                    nameplateEntities.put(player, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(player, target)));
                }
                announceSeaCreature(client, target, player);
            }
        }

        return found;
    }

    // プレイヤーエンティティ型モブが乗っている土台。乗っていなければ null。
    // Ragnarok は骸骨の馬に乗った姿で１体を成す
    private static Entity mountedNamedPlayerBase(MobVisual target, Entity player) {
        return target == MobVisual.SeaCreature.RAGNAROK ? player.getVehicle() : null;
    }

    // プレイヤーエンティティ型モブを探してよい場所にいるか。
    // Critter Safari は1つのサーバーの中でバイオームが分かれているため、
    // そのバイオームにいる時だけ探す。それ以外のモブは場所を問わない
    private static boolean canDetectNamedPlayerMob(Minecraft client, MobVisual target) {
        if (!SAFARI_NAMED_PLAYER_TARGETS.contains(target)) return true;
        return inSafariBiome(client.player, target);
    }

    // プレイヤーエンティティ型モブの見分け方。実体名が一意ならそれで照合する。
    // 実体名が他と共通で使えないモブだけ、やむを得ず別の手がかりを使う
    private static boolean matchesNamedPlayerMob(MobVisual target, Entity player) {
        // 実体名で照合するモブは、同じ名前の実プレイヤーがいると取り違えてしまう。
        // Boss Corleone だけはスキンで見分けるので、この確認は要らない
        if (target != CrystalHollows.BOSS_CORLEONE && !isFakePlayer(player)) return false;

        if (target == CrystalHollows.BOSS_CORLEONE) return matchesSkin(player, CORLEONE_SKIN_HASH);

        String entityName = namedPlayerEntityName(target);
        return entityName != null && matchesEntityNameExactly(player, entityName);
    }

    // プレイヤーエンティティ型モブの実体名。実体名では探せないものは null
    private static String namedPlayerEntityName(MobVisual target) {
        String name = NAMED_PLAYER_ENTITY_NAMES.get(target);
        if (name != null) return name;

        // Sea Creature のプレイヤー型は、実体名が表示名そのままになっている
        return target instanceof MobVisual.SeaCreature ? target.plainLabel() : null;
    }

    /**
     * 見た目だけプレイヤーの、実在しないプレイヤーか。
     *
     * 実プレイヤーの UUID は版が 4 で、Hypixel が作る NPC はそれ以外になる。
     * タブリストの有無で見ると、湧いた直後の一瞬だけ載っている間を実プレイヤーと
     * 取り違えて表示が遅れるため、その場で確定する UUID の版で見る
     */
    private static boolean isFakePlayer(Entity player) {
        return player.getUUID().version() != 4;
    }

    // スキンで見分ける。テクスチャのパス末尾がスキンごとに固有の値になる。
    // 読み込みが済むまでは既定のスキンが返るため、その間は一致しない
    private static boolean matchesSkin(Entity entity, String skinHash) {
        if (!(entity instanceof AbstractClientPlayer player)) return false;
        return player.getSkin().body().texturePath().toString().endsWith(skinHash);
    }

    // 読み込み済みのプレイヤーから、名前が一致するもの(=ボス本体)を探して登録する
    private static void detectPlayerBoss(Minecraft client, String bossName, String entityName, boolean[] bossFound) {
        for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
            CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
            if (!bossName.equals(boss.nameTag())) continue;

            boolean glowBoss = boss.enableHighlight().get();
            boolean plateBoss = boss.enableNameplate().get();

            for (Player player : client.level.players()) {
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
    // 実体名がその呼び名そのものか。部分一致だと、その語を含む名前の実プレイヤーを
    // 掴んでしまうため、実体名を確認できているモブはこちらで見る
    private static boolean matchesEntityNameExactly(Entity entity, String expected) {
        return entity.getName().getString().trim().equalsIgnoreCase(expected);
    }

    private static boolean matchesEntityName(Entity entity, String expected) {
        return ModConstants.containsIgnoreCase(entity.getName().getString().trim(), expected);
    }

    // プレイヤーエンティティ型ボスの照合名。該当しないボスは null
    private static String playerBossEntityName(String bossName) {
        MobVisual target = PLAYER_BOSS_TARGETS.get(bossName);
        return target == null ? null : namedPlayerEntityName(target);
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
                    client.level.getEntitiesOfClass(Mob.class, box,
                            e -> !(e instanceof AbstractSkeleton) && !crimsonBossEntities.containsKey(e)),
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

    // Critter Capsule を当てた回数。HPと同じ位置に出したいので、
    // 数値パースを挟まず色付き文字列をそのまま描かせる印(RAW)を付ける
    private static String capsuleLabel(int hits) {
        int shown = Math.min(hits, ModConstants.CAPSULE_MAX_THROWS);
        return ModConstants.RAW_HEALTH_PREFIX + "§e" + shown + "§7/§e" + ModConstants.CAPSULE_MAX_THROWS;
    }

    // Moonglade Marsh と Torrhus Canyon は体色を問わず1種類しかいない。
    // Safari のみ体色で呼び名が分かれるため、そこだけ DyeColor を見る。
    // 染色されていない個体(getColor() == null)や想定外の体色は対象外とする
    private static MobVisual shulkerTarget(Shulker shulker) {
        if (GameState.Server.isMoongladeMarsh()) return MoongladeMarsh.HIDEONLEAF;
        if (GameState.Server.isTorrhusCanyon()) return TorrhusCanyon.HIDEONSUN;
        if (!GameState.Server.isSafari()) return null;

        return safariShulkerColorTarget(shulker.getColor());
    }

    // Critter Safari の Hideon 系を色から引く。
    // 止まっているときはシュルカーの体色、
    // 動いているときはシュルカーボックスの色で、どちらも同じ色になる
    private static MobVisual safariShulkerColorTarget(DyeColor color) {
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
    private static MobVisual safariTarget(Minecraft client, Entity entity) {
        // Hypixel は「見た目のエンティティ」と「当たり判定のモブ」を重ねて1体のモブを作る。
        // 当たり判定側は必ず透明にされているので、透明なものは見た目の主役ではないと判断できる。
        //   Rockmite … シルバーフィッシュそのもの(見えている)
        //   Hideonwall / Duplico / Driftling / Chuckwalla / Troodon
        //            … 見た目は別のエンティティで、中の透明なシルバーフィッシュは当たり判定
        // 見た目がアーマースタンドのモブ。装備の無いものはネームタグ用の透明なスタンド
        if (entity instanceof ArmorStand stand) {
            if (stand.getCustomName() != null || !hasAnyEquipment(stand)) return null;

            // 被っている skull だけで種まで決まる。
            // Fairy Soul も名前なし・装備ありのアーマースタンドだが、skull が違うので自然に外れる
            MobVisual bySkin = headSkinTarget(entity);
            return SAFARI_SKIN_TARGETS.contains(bySkin) ? bySkin : null;
        }

        // 透明なものは別のモブの当たり判定。型といるバイオームで呼び名が決まる
        if (entity.isInvisible()) {
            if (entity instanceof Silverfish) {
                if (hasHeadStandNear(client, entity)) return null;           // Driftling はスタンド側で判定済み

                // 止まっている間はシュルカーが見た目を担うので、当たり判定は拾わない
                if (shulkerClaimedEntities.contains(entity)) return null;

                // 動き出すとシュルカーと絵が消え、シュルカーボックスを持つ ItemDisplay に置き換わる。
                // Duplico も「透明なシルバーフィッシュ + ItemDisplay」で作りが同じなので、
                // 中身がシュルカーボックスかどうかで Hideon 系と分ける
                ShulkerBoxBlock shulkerBox = shulkerBoxDisplayNear(client, entity);
                if (shulkerBox != null) {
                    // 箱の色は止まっているときの体色と同じなので、そこから種を決める
                    return safariShulkerColorTarget(shulkerBox.getColor());
                }

                // 見た目の Display に被せた skull で種が決まる。
                // Duplico だけはバニラのブロックに化けるので skull が無く、バイオームで決める
                MobVisual bySkin = safariDisplaySkinTarget(client, entity);
                if (bySkin != null) return bySkin;

                if (inSafariHaunted(entity)) return SafariHaunted.DUPLICO;
                // Forest で透明なシルバーフィッシュを使うのは Hideonfloor だけ。
                // 動いてシュルカーを見失っても、見た目のブロックを対象にできる
                if (inSafariForest(entity)) return SafariForest.HIDEONFLOOR;
                return null;
            }
            if (entity instanceof Bat) {
                // 見た目の Display に被せた skull で、バイオームを見なくても種が決まる
                return safariDisplaySkinTarget(client, entity);
            }
            if (entity instanceof TropicalFish) {
                // 見た目の Display に被せた skull で、バイオームを見なくても種が決まる
                return safariDisplaySkinTarget(client, entity);
            }
            // Shyworm(スライム)など、見た目を別で拾うものはここでは扱わない
            return null;
        }

        if (entity instanceof Silverfish) return SafariCavern.ROCKMITE;
        if (entity instanceof Armadillo) return SafariCavern.SCRAPPY;
        if (entity instanceof Sniffer) return SafariCavern.SNOOZLE;
        if (entity instanceof Vex) return SafariCavern.GEMZIE;

        if (entity instanceof Fox) return SafariForest.FOXTROT;
        if (entity instanceof Bee) return SafariForest.HONEYBUG;
        if (entity instanceof Frog) return SafariForest.TREEFROG;
        if (entity instanceof Creaking) return SafariForest.WOODCHUCKER;
        if (entity instanceof Panda) return SafariForest.FLUFFLING;
        // オウムは3種いるので変種で分ける
        if (entity instanceof Parrot parrot) {
            return switch (parrot.getVariant()) {
                case BLUE -> SafariForest.BLUEBIRD;
                case GREEN -> SafariForest.PARAKEET;
                case RED_BLUE -> SafariForest.MACAW;
                default -> null;
            };
        }

        // CaveSpider は Spider の派生。Broodmother とはエリアが違うので競合しない
        if (entity instanceof CaveSpider) return SafariHaunted.AREITA;
        if (entity instanceof Bat) return SafariHaunted.BLOODBAT;
        if (entity instanceof Endermite) return SafariHaunted.LITTERBUG;
        if (entity instanceof Phantom) return SafariHaunted.SOLSNATCHER;

        if (entity instanceof Goat) return SafariIcy.BILLYGOAT;
        if (entity instanceof Dolphin) return SafariIcy.NOZZLENOSE;
        if (entity instanceof PolarBear) return SafariIcy.POLARIS;
        if (entity instanceof GlowSquid) return SafariIcy.SHUDDERSQUID;
        if (entity instanceof SnowGolem) return SafariIcy.STRONGARM;
        // Cavernfish(茶・灰)と Tepid(白)はどちらも熱帯魚。色で分ける
        if (entity instanceof TropicalFish fish) return tropicalFishTarget(fish);
        return null;
    }

    // 呼び出し元で Moonglade Marsh / Torrhus Canyon のどちらかであることを保証している。
    // カエル・オウム・アルマジロはエリア内に1種しかいないため型だけで確定でき、
    // ウーパールーパーのみ Torrhus Canyon に2種いるので変種で振り分ける
    /**
     * Lotus Atoll の Critter。どれもバニラ相当の大きさで、型で見分けられる。
     *
     * カエルだけは同じ大きさの Sea Creature (Atoll Croaker) がいるので、
     * 体色で見分ける。Lotum はオレンジ、Atoll Croaker は緑
     */
    private static MobVisual lotusAtollTarget(Entity entity) {
        if (entity instanceof TropicalFish) return LotusAtoll.LOTUSFISH;
        if (entity instanceof Turtle) return LotusAtoll.TEWTIL;
        if (entity instanceof Dolphin) return LotusAtoll.FLIPFLOPPER;
        if (entity instanceof GlowSquid) return LotusAtoll.SEASHINE;

        if (entity instanceof Frog frog) {
            // 大きいカエルは Sea Creature の Puddle Jumper / Frog Prince。
            // Frog Prince も緑だが、色を見る前にここで外れる
            if (frog.getBbWidth() > LOTUM_MAX_WIDTH) return null;
            // 残る 0.50 のカエルは Lotum(オレンジ)と Atoll Croaker(緑)の2択
            return frog.getVariant().is(FrogVariants.COLD) ? null : LotusAtoll.LOTUM;
        }
        return null;
    }

    private static MobVisual areaAnimalTarget(Entity entity) {
        // 透明なものは、別のモブの当たり判定か、モブ以外の見た目のために置かれたエンティティ。
        // Critter を捕まえる Lasso の先端は透明なコウモリで、そのままだと Murkbat になってしまう
        if (entity.isInvisible()) return null;

        if (GameState.Server.isLotusAtoll()) return lotusAtollTarget(entity);

        boolean marsh = GameState.Server.isMoongladeMarsh();
        if (entity instanceof Frog) return marsh ? MoongladeMarsh.MOSSYBIT : TorrhusCanyon.DUSTYBIT;
        // Pangolin と Blue Jay は Torrhus Canyon のみ。Moonglade Marsh の該当種は対象外
        if (entity instanceof Armadillo) return marsh ? null : TorrhusCanyon.PANGOLIN;
        if (entity instanceof Parrot) return marsh ? null : TorrhusCanyon.BLUE_JAY;
        if (entity instanceof Axolotl axolotl) {
            if (marsh) return MoongladeMarsh.CORALOT;
            return switch (axolotl.getVariant()) {
                case WILD -> TorrhusCanyon.SEPIALOT;
                case GOLD -> TorrhusCanyon.GOLDOLOT;
                default -> null;
            };
        }

        // 以降は Moonglade Marsh にしか出現しない型
        if (marsh) {
            if (entity instanceof Cod) return MoongladeMarsh.COD;
            if (entity instanceof Salmon) return MoongladeMarsh.SALMON;
            if (entity instanceof Dolphin) return MoongladeMarsh.JOYDIVE;
            if (entity instanceof GlowSquid) return MoongladeMarsh.LUMISQUID;
            if (entity instanceof Turtle) return MoongladeMarsh.SHELLWISE;
            if (entity instanceof Pufferfish) return MoongladeMarsh.SPIKE;
            if (entity instanceof Tadpole) return MoongladeMarsh.BIRRIES;
            // Hewver は子どものスニッファーで、大人は Sea Creature の Nessie
            if (entity instanceof Sniffer sniffer) {
                return sniffer.isBaby() ? MoongladeMarsh.HEWVER : MobVisual.SeaCreature.NESSIE;
            }
            if (entity instanceof Hoglin) return MoongladeMarsh.HONEYHOG;
            if (entity instanceof Endermite) return MoongladeMarsh.HONEYMITE;
            if (entity instanceof Bat) return MoongladeMarsh.MURKBAT;
            // Tidetot はドラウンド。大人も子どももいるので大きさでは絞れない。
            // 同じエリアの Stridersurfer もドラウンドだが、こちらは必ずストライダーに
            // 乗っているので、騎乗していないことを条件にすれば弾ける
            if (entity instanceof Drowned drowned && !drowned.isPassenger()) return MoongladeMarsh.TIDETOT;
            // Chillblade / Chillshot はどちらも Stray。Chill としてまとめて扱う
            if (entity instanceof Stray) return MoongladeMarsh.CHILL;
            // ファントムは3種いるが、大きさが段違いなので当たり判定の幅で見分けられる
            if (entity instanceof Phantom) {
                if (entity.getBbWidth() >= DREADWING_MIN_WIDTH) return MoongladeMarsh.DREADWING;
                return entity.getBbWidth() >= PHANFLARE_MIN_WIDTH ? MoongladeMarsh.PHANFLARE : MoongladeMarsh.PHANPYRE;
            }
            // Azure と Verdant はどちらも熱帯魚。色でしか見分けられない
            if (entity instanceof TropicalFish fish) return tropicalFishTarget(fish);
            // パンダは2種いるが、Mochibear だけが茶色の個体なので毛色で分けられる
            if (entity instanceof Panda panda) {
                return panda.getVariant() == Panda.Gene.BROWN ? MoongladeMarsh.MOCHIBEAR : MoongladeMarsh.BAMBULEAF;
            }
            return null;
        }

        // 以降は Torrhus Canyon にしか出現しない型
        // EVIL 変種は Sea Creature の Carrot King なので、Bunbun からは外す
        if (entity instanceof Rabbit rabbit) {
            return rabbit.getVariant() == Rabbit.Variant.EVIL ? null : TorrhusCanyon.BUNBUN;
        }
        if (entity instanceof Creaking) return TorrhusCanyon.DRYBARK;
        if (entity instanceof Fox) return TorrhusCanyon.FIREFOX;
        if (entity instanceof Hoglin) return TorrhusCanyon.GROUNDHOG;
        if (entity instanceof Ghast) return TorrhusCanyon.HIVETHIEF;
        if (entity instanceof Goat) return TorrhusCanyon.MOUNTAIN_GOAT;
        if (entity instanceof Vex) return TorrhusCanyon.PUCK;
        // Parched はバニラのスケルトン系モブ。このエリアで同じ型は他にいない
        if (entity instanceof Parched) return TorrhusCanyon.PARCHED;
        // Beeheemoth 以外のハチはネームタグ側で振り分けるため、ここでは大きい個体だけを拾う
        if (entity instanceof Bee) return isBeeheemoth(entity) ? TorrhusCanyon.BEEHEEMOTH : null;
        // Ember / Solar / Timil も熱帯魚。こちらも色でしか見分けられない
        if (entity instanceof TropicalFish fish) return tropicalFishTarget(fish);
        return null;
    }

    // 溜まった CRIT パーティクルを Invisibug 本体に解決する。
    // 既に見つけている個体の近くのパーティクルは読み飛ばし、同じ個体を何度も探し直さない
    private static void resolveInvisibugs(Minecraft client) {
        invisibugEntities.removeIf(Entity::isRemoved);

        double[] particle;
        while ((particle = pendingCritParticles.poll()) != null) {
            Vec3 pos = new Vec3(particle[0], particle[1], particle[2]);
            if (invisibugEntities.stream().anyMatch(e -> e.distanceToSqr(pos) < INVISIBUG_RADIUS * INVISIBUG_RADIUS)) continue;

            AABB box = new AABB(
                    pos.x - INVISIBUG_RADIUS, pos.y - INVISIBUG_RADIUS, pos.z - INVISIBUG_RADIUS,
                    pos.x + INVISIBUG_RADIUS, pos.y + INVISIBUG_RADIUS, pos.z + INVISIBUG_RADIUS);
            ArmorStand nearest = null;
            double nearestDistSqr = Double.MAX_VALUE;
            for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, box, e -> true)) {
                double distSqr = stand.distanceToSqr(pos);
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
    private static boolean isPlainArmorStand(ArmorStand stand) {
        if (stand.getCustomName() != null) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stand.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    // Azure / Verdant / Ember / Solar / Timil はいずれも熱帯魚で、色でしか見分けられない。
    // 体色で判別できない場合(Timil の白地にピンクの模様など)に備え、模様の色も見る
    private static MobVisual tropicalFishTarget(TropicalFish fish) {
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

        // Display は当たり判定を持たないので、原点をそのまま表示位置にする。
        // ブロックを映しているものだけは、原点が底なので中ほどまで上げる
        if (entity instanceof Display.ItemDisplay item) {
            return displayItem(item).getItem() instanceof BlockItem ? BLOCK_DISPLAY_ANCHOR : DISPLAY_ANCHOR;
        }
        if (entity instanceof Display) return DISPLAY_ANCHOR;

        if (entity instanceof ArmorStand stand && !stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            // marker のアーマースタンドは当たり判定が 0 になるため、当たり判定からは大きさを測れない。
            // 見た目の大きさは scale 属性で決まるので、そちらを基準にする
            double base = stand.isSmall() ? SMALL_HEAD_STAND_ANCHOR : HEAD_STAND_ANCHOR;
            return base * stand.getScale();
        }
        return entity.getBbHeight() / 2.0;
    }

    // 複数のヘッドで1体を成すモブの表示。連なったヘッドをすべて光らせる。
    // ネームプレートは重ならないよう、エンティティIDが最も小さいヘッド1つだけに出す。
    // 同じモブに複数のネームタグが付いていても、代表が同じになるので二重に出ない
    private static List<Entity> applyHeadChain(Minecraft client, Entity nameTag, MobVisual target, double searchRadius) {
        List<Entity> heads = connectedHeadStands(client, nameTag, target, searchRadius);
        if (heads.isEmpty()) return heads;

        for (Entity head : heads) {
            if (target.highlight()) registerHighlight(head, target);
        }

        // 頭の skull が分かっている種は頭に、それ以外は体の真ん中に出す
        heads.sort(Comparator.comparingInt(Entity::getId));
        Entity representative = headBodyAnchor(target, heads);

        // Tracer をヘッドごとに登録すると、自分に最も近いヘッドが入れ替わるたびに
        // 線の行き先が上のヘッドと真ん中のヘッドの間で飛んでしまう。
        // ネームプレートと同じ代表のヘッドにだけ出す
        registerTracer(representative, target);
        if (target.nameplate()) {
            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
            nameplateEntities.put(representative, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(representative, target)));
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
        if (visual instanceof ArmorStand stand && !stand.isMarker()) headOnlyGlowEntities.add(visual);

        highlightedEntities.add(visual);
        // アーマースタンドや Display は型による一括削除ができないので、毎 tick 作り直す集合へ入れる
        rebuiltVisuals.add(visual);
        // 同じ型でも呼び名ごとに色が変わるため、mixin から引けるよう控えておく
        customGlowColors.put(visual, target.glowColorRGB());
    }

    // Torrhus Canyon のヘッド系モブ。見た目は skull を被せたアーマースタンドで、
    // 被っている skull のスキンだけで種まで決まる
    private static MobVisual canyonHeadTarget(Minecraft client, Entity entity) {
        MobVisual bySkin = headSkinTarget(entity);
        if (bySkin == null) return null;
        return CANYON_SKIN_TARGETS.contains(bySkin) ? bySkin : null;
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

    // モブ以外(ブロックなど)も同じ区分けで Forest Biome を判定できるようにしておく
    public static boolean inSafariForest(BlockPos pos) {
        return pos.getX() > SAFARI_CENTER_X && pos.getZ() > SAFARI_CENTER_Z;
    }

    // Critter Safari は中心を境に4つのバイオームへ分かれている。
    // その位置が、指定した座標(プレイヤー)と同じバイオームに入っているか
    public static boolean inSameSafariBiome(BlockPos pos, double originX, double originZ) {
        return (pos.getX() > SAFARI_CENTER_X) == (originX > SAFARI_CENTER_X)
                && (pos.getZ() > SAFARI_CENTER_Z) == (originZ > SAFARI_CENTER_Z);
    }

    private static boolean inSafariHaunted(Entity entity) {
        return entity.getX() > SAFARI_CENTER_X && entity.getZ() < SAFARI_CENTER_Z;
    }

    private static boolean inSafariIcy(Entity entity) {
        return entity.getX() < SAFARI_CENTER_X && entity.getZ() < SAFARI_CENTER_Z;
    }

    // すぐ近くにある、指定した種類の最も近いエンティティ
    private static Entity nearestOfType(Minecraft client, Entity entity, Class<? extends Entity> type) {
        return nearestOfTypeWithin(client, entity, type, HITBOX_HEAD_RADIUS);
    }

    private static Entity nearestOfTypeWithin(Minecraft client, Entity entity, Class<? extends Entity> type, double radius) {
        AABB box = entity.getBoundingBox().inflate(radius);
        return getClosestEntity(client.level.getEntitiesOfClass(type, box, e -> true), entity);
    }

    // すぐ近くに「シュルカーボックスを持つ ItemDisplay」があるか。
    // 動いている Hideonfloor / Hideonwall の見た目がこれで、体色がそのまま箱の色になる
    private static ShulkerBoxBlock shulkerBoxDisplayNear(Minecraft client, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(HITBOX_HEAD_RADIUS);
        for (Display.ItemDisplay display : client.level.getEntitiesOfClass(Display.ItemDisplay.class, box, e -> true)) {
            if (displayItem(display).getItem() instanceof BlockItem item
                    && item.getBlock() instanceof ShulkerBoxBlock shulkerBox) {
                return shulkerBox;
            }
        }
        return null;
    }

    // Display が映しているアイテム。まだ届いていなければ空
    private static ItemStack displayItem(Display.ItemDisplay display) {
        Display.ItemDisplay.ItemRenderState state = display.itemRenderState();
        return state == null ? ItemStack.EMPTY : state.itemStack();
    }

    // Rockmite Mound かどうか。見た目は決まったスキンなので、そのテクスチャで見分ける
    private static boolean isRockmiteMound(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return false;

        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (property == null) continue;
            if (decodeSkinTexture(property.value()).contains(ROCKMITE_MOUND_TEXTURE)) return true;
        }
        return false;
    }

    // スキンの情報は Base64 で包んだ JSON。テクスチャのURLだけ見たいので素の文字列に戻す
    private static String decodeSkinTexture(String value) {
        try {
            return new String(java.util.Base64.getDecoder().decode(value),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }


    // Hideon 系のシュルカーと、その当たり判定のシルバーフィッシュを結び付ける。
    //
    // 止まっている間は両者が重なっているが、動き出すとシュルカーが当たり判定を追いかける形になり
    // 距離が開く。「近くにシュルカーが居るか」で見ていると、離れた隙に当たり判定が独り立ちして、
    // 同じ Haunted で同じ透明シルバーフィッシュを使う Duplico として拾われてしまう。
    // そこでシュルカー側を起点にし、一番近い当たり判定を1体だけ選んで覚えておく。
    // 1体ずつしか結び付かないので、近くに本物の Duplico が居ても巻き添えにしない
    private static void claimShulkerHitboxes(Minecraft client) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Shulker shulker)) continue;
            if (shulkerTarget(shulker) == null) continue;

            Entity hitbox = nearestOfTypeWithin(client, entity, Silverfish.class, SHULKER_COMPANION_RADIUS);
            if (hitbox != null && hitbox.isInvisible()) shulkerClaimedEntities.add(hitbox);
        }
    }

    // すぐ近くに指定した種類のエンティティがあるか。
    // Hypixel は「当たり判定のモブ」と「見た目のエンティティ」を重ねて1体のモブを作るので、
    // 何と一緒にいるかで同じ型のモブを見分けられる
    private static boolean hasNear(Minecraft client, Entity entity, Class<? extends Entity> type) {
        return hasNearWithin(client, entity, type, HITBOX_HEAD_RADIUS);
    }

    private static boolean hasNearWithin(Minecraft client, Entity entity, Class<? extends Entity> type, double radius) {
        AABB box = entity.getBoundingBox().inflate(radius);
        return !client.level.getEntitiesOfClass(type, box, e -> true).isEmpty();
    }

    // すぐ近くに「頭に skull を被せたアーマースタンド」があるか。
    // ある場合、その当たり判定は別のモブの一部なので単独のモブとして扱わない
    private static boolean hasHeadStandNear(Minecraft client, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(HITBOX_HEAD_RADIUS);
        return !client.level.getEntitiesOfClass(ArmorStand.class, box,
                e -> e.getCustomName() == null && !e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()).isEmpty();
    }

    // プレイヤー型のモブ(Grizzly Bear や Hideyho など)の本体を探す
    // 実体名で照合できるプレイヤーエンティティ型モブが、どこかに見つかるか。
    // detectNamedPlayerMobs と同じ照合を使うので、これが偽なら表示も出ない
    private static boolean hasNamedPlayerMob(Minecraft client, MobVisual target) {
        for (Player player : client.level.players()) {
            if (player != client.player && matchesNamedPlayerMob(target, player)) return true;
        }
        return false;
    }

    // アーマースタンドが何かを装備しているか。装備が無いものはネームタグ用の透明なスタンド
    private static boolean hasAnyEquipment(ArmorStand stand) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stand.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    // ネームタグの周りから、連なったヘッドを集める。
    // ヘッドは「頭に skull を被せた名前なしのアーマースタンド」で、当たり判定は別のモブが持っている。
    // glow は描画モデルに沿うため、見た目どおりの輪郭にするにはこのアーマースタンドを光らせる
    private static List<Entity> connectedHeadStands(Minecraft client, Entity anchor, MobVisual target,
                                                    double searchRadius) {
        AABB box = anchor.getBoundingBox().inflate(searchRadius);
        // その種のスキンを被ったヘッドだけを候補にする。
        // これで別のヘッドモブ(Ant や Tiki 系)を巻き込まない
        List<ArmorStand> candidates = new ArrayList<>(client.level.getEntitiesOfClass(ArmorStand.class, box,
                e -> headSkinTarget(e) == target));

        // 同じ種が近くに2体湧くこともあるので、
        // 起点に最も近い節から、隣り合う節だけをたどって集める
        List<Entity> heads = new ArrayList<>();
        Entity start = headStandUnderNameTag(candidates, anchor);
        if (start == null) return heads;
        heads.add(start);
        candidates.remove(start);

        boolean added = true;
        while (added) {
            added = false;
            for (Iterator<ArmorStand> iterator = candidates.iterator(); iterator.hasNext(); ) {
                ArmorStand candidate = iterator.next();
                if (heads.stream().noneMatch(
                        h -> h.distanceToSqr(candidate) <= HEAD_CHAIN_GAP * HEAD_CHAIN_GAP)) continue;

                heads.add(candidate);
                iterator.remove();
                added = true;
            }
        }
        return heads;
    }

    /**
     * ネームプレートに出す体力。読めなければ null。
     *
     * 体力はネームタグの文言にしかないので、本体の近くから探す。
     * 隣の個体を掴まないよう、同じ呼び名のネームタグだけを見る。
     * Critter Safari のモブは体力を出さない
     */
    private static String nameplateHealth(Entity visual, MobVisual target) {
        if (healthNameTags.isEmpty() || isSafariTarget(target)) return null;

        String label = target.plainLabel();
        Entity best = null;
        double bestDistance = HEALTH_NAMETAG_RADIUS * HEALTH_NAMETAG_RADIUS;
        String health = null;
        for (Entity nameTag : healthNameTags) {
            double distance = nameTag.distanceToSqr(visual);
            if (distance > bestDistance) continue;

            // 控えた後にネームタグが消えることもあるので、都度確かめる
            Component name = nameTag.getCustomName();
            if (name == null || !ModConstants.containsIgnoreCase(name.getString(), label)) continue;

            bestDistance = distance;
            best = nameTag;
            health = healthFromNameTag(name.getString());
        }
        return best == null ? null : health;
    }

    // Critter Safari の呼び名か
    private static boolean isSafariTarget(MobVisual target) {
        return target instanceof SafariCavern || target instanceof SafariForest
                || target instanceof SafariHaunted || target instanceof SafariIcy;
    }

    // ネームタグの文字列から「現在HP/最大HP」を取り出す
    private static String healthFromNameTag(String nameStr) {
        Matcher matcher = HEALTH_PATTERN.matcher(nameStr);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Beeheemoth かどうか。他の2種のハチはバニラサイズなので、大きさだけで見分けられる
    private static boolean isBeeheemoth(Entity entity) {
        return entity.getBbHeight() >= BEEHEEMOTH_MIN_HEIGHT;
    }

    /**
     * Mob Visuals に登録している Sea Creature を見つけたら、名前をタイトルで知らせる。
     *
     * 検出の経路は3通り(ネームタグ・エンティティ型・実体名)あり、同じ個体が複数の経路で
     * 見つかることも毎tick見つかることもあるので、一度出した個体は覚えておいて出し直さない。
     */
    private static void announceSeaCreature(Minecraft client, MobVisual target, Entity entity) {
        if (!(target instanceof MobVisual.SeaCreature)) return;
        if (entity == null || !announcedSeaCreatures.add(entity)) return;

        showSeaCreatureTitle(client, target);
    }

    /**
     * 自分が釣り上げた Sea Creature を知らせる。チャットの文言から呼ばれる。
     *
     * 自分で釣った場合は、少し遅れて同じモブがエンティティとしても見つかる。
     * その匹数ぶんだけ後から取り消せるよう、種と時刻と残りの数を控えておく
     *
     * @param doubleHook 一度に2匹釣れたか
     */
    public static void showSeaCreatureCatchTitle(Minecraft client, MobVisual target, boolean doubleHook) {
        caughtSeaCreature = target;
        caughtSeaCreatureMillis = System.currentTimeMillis();
        caughtSeaCreaturePending = doubleHook ? 2 : 1;
        showSeaCreatureTitle(client, target, true, doubleHook ? SEA_CREATURE_DOUBLE_HOOK_SUFFIX : "");
    }

    /** 見つけた Sea Creature を知らせる。エンティティの検知から呼ばれる */
    public static void showSeaCreatureTitle(Minecraft client, MobVisual target) {
        showSeaCreatureTitle(client, target, false, "");
    }

    private static void showSeaCreatureTitle(Minecraft client, MobVisual target, boolean fromCatch, String suffix) {
        if (!ModConfig.INSTANCE.mobVisuals.enableSeaCreatureTitle) return;

        // 自分で釣った分は、チャットの文言で既に知らせている。
        // 引換券のように釣れた匹数ぶんだけ使うので、
        // その間に他の人が釣った同じ種はそのまま知らせる
        if (!fromCatch && target == caughtSeaCreature && caughtSeaCreaturePending > 0
                && System.currentTimeMillis() - caughtSeaCreatureMillis < SEA_CREATURE_CATCH_ECHO_MS) {
            caughtSeaCreaturePending--;
            return;
        }

        // 匹数は色を変えて挟むので、その後ろの "!" はモブの色に戻す
        String color = BossNameplateRenderer.colorCode(target.tracerColorARGB());
        String text = color + "§l" + target.plainLabel() + suffix + color + "§l!";
        // このタイトルだけはフェードを挟まず、出た瞬間に読めるようにする
        client.execute(() -> {
            NotificationUtils.showTitle(client, Component.literal(text), null,
                    SEA_CREATURE_TITLE_FADE, SEA_CREATURE_TITLE_STAY, SEA_CREATURE_TITLE_FADE);
            NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SEA_CREATURE_SOUND_VOLUME, SEA_CREATURE_SOUND_PITCH);
        });
    }

    /**
     * ネームタグから Sea Creature の本体を探す。
     *
     * 本体の型が分かっているものは、その型のうちネームタグの直下にいる個体を本体とする。
     * これらは同じエリアに同じ型の別のモブがいて型だけでは決められないが、
     * ネームタグと組にすれば「どのモブか」を確定できる。
     * 型が分からないものは、ヘッドや Display で見た目を作っている前提の共通処理に任せる。
     */
    private static Entity seaCreatureVisual(Minecraft client, Entity nameTag, MobVisual target) {
        // プレイヤーエンティティ型は実体名照合が本命。
        // ネームタグからは探さず、見つからないことを合図として使う
        if (SEA_CREATURE_PLAYER_TARGETS.contains(target)) return null;

        // スキンで見分ける種は、そのスキンを被ったヘッドだけを本体とする。
        // 「ネームタグの下にいる何か」を本体とみなす保険は置かない。
        // 見つからないことを、スキン差し替えの合図として使いたいため
        if (HEAD_SKIN_SPECIES.contains(target)) return skinnedHeadUnderNameTag(client, nameTag, target);

        Class<? extends Entity> bodyType = seaCreatureBodyType(target);
        if (bodyType == null) return null;

        AABB box = nameTag.getBoundingBox().inflate(NAMETAG_SEARCH_RADIUS);
        Predicate<Entity> filter = body -> !body.isInvisible() && seaCreatureBodyMatches(target, body);
        return headStandUnderNameTag(client.level.getEntitiesOfClass(bodyType, box, filter), nameTag);
    }

    // ネームタグの下から、その Sea Creature のスキンを被ったヘッドを探す。
    // Water Hydra のようにモブ本体が被っていることもあるので、見るのは LivingEntity
    private static Entity skinnedHeadUnderNameTag(Minecraft client, Entity nameTag, MobVisual target) {
        AABB box = nameTag.getBoundingBox().inflate(NAMETAG_SEARCH_RADIUS);
        return headStandUnderNameTag(client.level.getEntitiesOfClass(LivingEntity.class, box,
                wearer -> headSkinTarget(wearer) == target), nameTag);
    }

    /**
     * ネームタグから種の名前を取り出す。
     *
     * "[Lv58] Parched 25,000/25,000❤" なら "Parched" になる。
     * 形が違って引けなくても、知らせないだけなので害はない
     */
    private static String nameTagSpecies(String nameStr) {
        String text = NAMETAG_PREFIX.matcher(nameStr).replaceAll("");
        Matcher health = HEALTH_PATTERN.matcher(text);
        if (health.find()) text = text.substring(0, health.start());

        // 末尾の❤ や記号を落とす
        int end = text.length();
        while (end > 0 && !Character.isLetterOrDigit(text.charAt(end - 1))) end--;
        return text.substring(0, end).trim();
    }

    // 型で判定する走査のどれかが、ネームタグの近くで同じ呼び名に行き着くか
    private static boolean hasTypeTargetNear(Minecraft client, Entity nameTag, MobVisual target) {
        AABB box = nameTag.getBoundingBox().inflate(NAMETAG_SEARCH_RADIUS);
        for (Entity nearby : client.level.getEntitiesOfClass(Entity.class, box, e -> true)) {
            if (typeTargetOf(client, nearby) == target) return true;
        }
        return false;
    }

    // 型で判定する走査を、ひと通り試す。
    // どれもエリアで絞っているので、場違いなら null が返る
    private static MobVisual typeTargetOf(Minecraft client, Entity entity) {
        MobVisual target = areaAnimalTarget(entity);
        if (target != null) return target;

        if (entity instanceof Shulker shulker) {
            target = shulkerTarget(shulker);
            if (target != null) return target;
        }

        target = safariTarget(client, entity);
        return target != null ? target : seaCreatureTypeTarget(entity);
    }

    // ネームタグの下に、その Critter Safari モブの skull が見つかるか。
    // Gazer / Driftling / Shyworm はアーマースタンドが、残りは見た目の Display が被っている
    private static boolean hasSafariSkinNear(Minecraft client, Entity nameTag, MobVisual target) {
        if (skinnedHeadUnderNameTag(client, nameTag, target) != null) return true;

        AABB box = nameTag.getBoundingBox().inflate(NAMETAG_SEARCH_RADIUS);
        for (Display.ItemDisplay display : client.level.getEntitiesOfClass(Display.ItemDisplay.class, box, e -> true)) {
            if (itemSkinTarget(displayItem(display)) == target) return true;
        }
        return false;
    }

    // ネームタグの下から、その skull を被ったヘッドを探す。
    // 同じ skull を使う種が複数あるときは、種の区別はネームタグの名前に任せる
    private static Entity headStandWithSkinUnderNameTag(Minecraft client, Entity nameTag, String skinId) {
        AABB box = nameTag.getBoundingBox().inflate(NAMETAG_SEARCH_RADIUS);
        return headStandUnderNameTag(client.level.getEntitiesOfClass(LivingEntity.class, box,
                wearer -> matchesHeadSkin(wearer, skinId)), nameTag);
    }

    // ネームタグから本体を探すときに見る型。分かっていないものは null
    private static Class<? extends Entity> seaCreatureBodyType(MobVisual target) {
        if (!(target instanceof MobVisual.SeaCreature seaCreature)) return null;
        return switch (seaCreature) {
            case THE_LOCH_EMPEROR -> Guardian.class;
            // Lotus Atoll のカエルは2種。乗り物の有無で見分ける(seaCreatureBodyMatches)
            case PUDDLE_JUMPER, FROG_PRINCE -> Frog.class;
            case SILKBREEZE -> Breeze.class;
            case THUNDER -> ElderGuardian.class;
            case LORD_JAWBUS -> IronGolem.class;
            case REINDRAKE -> EnderDragon.class;
            case MITHRIL_GRUBBER, WATER_WORM, FLAMING_WORM -> Silverfish.class;
            case OASIS_SHEEP -> Sheep.class;
            case OASIS_RABBIT, CARROT_KING -> Rabbit.class;
            case AGARIMOO -> MushroomCow.class;
            case POISONED_WATER_WORM -> Endermite.class;
            case LAVA_BLAZE -> Blaze.class;
            case LAVA_PIGMAN -> ZombifiedPiglin.class;
            case PLHLEGBLAST -> Squid.class;
            default -> null;
        };
    }

    // 同じ型の中でさらに絞る必要があるものだけ、ここで振り分ける
    private static boolean seaCreatureBodyMatches(MobVisual target, Entity entity) {
        if (target == MobVisual.SeaCreature.CARROT_KING) {
            return entity instanceof Rabbit rabbit && rabbit.getVariant() == Rabbit.Variant.EVIL;
        }
        if (target == MobVisual.SeaCreature.OASIS_RABBIT) {
            return entity instanceof Rabbit rabbit && rabbit.getVariant() != Rabbit.Variant.EVIL;
        }
        // GlowSquid はイカの派生型なので、素のイカだけを本体とみなす
        if (target == MobVisual.SeaCreature.PLHLEGBLAST) return !(entity instanceof GlowSquid);
        // The Loch Emperor はスケルトンを乗せたガーディアンだけが本体
        if (target == MobVisual.SeaCreature.THE_LOCH_EMPEROR) return isLochEmperorBody(entity);
        // Frog Prince はスライムに乗ったカエル。Puddle Jumper は何にも乗っていない
        if (target == MobVisual.SeaCreature.FROG_PRINCE) return entity.getVehicle() != null;
        if (target == MobVisual.SeaCreature.PUDDLE_JUMPER) return entity.getVehicle() == null;
        return true;
    }

    /**
     * エンティティ型が分かっている Sea Creature を、型(と一部は変種)から引く。
     *
     * 同じ型のモブが他のエリアにもいるので、まず釣れるエリアかどうかで絞ってから型を見る。
     * ここで扱うのは、そのエリアにその型のモブが1種しかいないと言い切れるものだけ。
     * Special タブの面々はバニラ相当の同じ型のモブと紛れる恐れがあるため、
     * ネームタグを見つけてから本体を探す seaCreatureVisual 側に任せる。
     */
    private static MobVisual seaCreatureTypeTarget(Entity entity) {
        // 当たり判定が透明で型では分けられない面々は、被っているヘッドのスキンで見分ける。
        // アーマースタンド自体は透明でも、被せた skull は描画されるので、
        // 透明を弾くより先に見る
        // スキンで決まるのは Sea Creature とは限らない。
        // Tiki 系ならここでは扱わず、canyonHeadTarget 側に任せる
        MobVisual bySkin = headSkinTarget(entity);
        if (bySkin != null) return bySkin instanceof MobVisual.SeaCreature ? bySkin : null;

        // 残りは見た目のあるモブ。透明なものは別のモブの当たり判定か、飾りのエンティティ
        if (entity.isInvisible()) return null;

        if (GameState.Server.isCrimsonIsle()) {
            if (entity instanceof ElderGuardian) return MobVisual.SeaCreature.THUNDER;
            if (entity instanceof IronGolem) return MobVisual.SeaCreature.LORD_JAWBUS;
            return null;
        }
        if (GameState.Server.isTorrhusCanyon()) {
            return entity instanceof Breeze ? MobVisual.SeaCreature.SILKBREEZE : null;
        }
        if (GameState.Server.isJerrysWorkshop()) {
            return entity instanceof EnderDragon ? MobVisual.SeaCreature.REINDRAKE : null;
        }
        if (GameState.Server.isLotusAtoll()) {
            if (!(entity instanceof Frog frog)) return null;
            // 小さいカエルは Critter の Lotum と Sea Creature の Atoll Croaker。
            // どちらもここでは扱わない
            if (frog.getBbWidth() <= LOTUM_MAX_WIDTH) return null;
            // Frog Prince は土台のスライムに乗ったカエル。Puddle Jumper は乗っていない
            return frog.getVehicle() != null
                    ? MobVisual.SeaCreature.FROG_PRINCE : MobVisual.SeaCreature.PUDDLE_JUMPER;
        }
        if (GameState.Server.isMoongladeMarsh()) {
            // The Loch Emperor はガーディアンの上にスケルトンが乗った組で1体を成す。
            // 土台のガーディアンだけを本体として扱い、上のスケルトンは別のモブにしない
            return isLochEmperorBody(entity) ? MobVisual.SeaCreature.THE_LOCH_EMPEROR : null;
        }
        return null;
    }

    /**
     * 見つけた Sea Creature に Highlight / Tracer / ネームプレート / タイトルを付ける。
     *
     * The Loch Emperor だけはガーディアンの上にスケルトンが乗った組で1体を成すので、
     * 両方を光らせたうえで、Tracer とネームプレートは目線に近い上のスケルトンに付ける。
     */
    private static void applySeaCreatureVisuals(Minecraft client, MobVisual target, Entity body) {
        Entity visual = body;
        // 当たり判定そのものを光らせてよいか。見た目が別のエンティティにあるなら光らせない
        boolean highlightBody = true;

        if (HEAD_BODY_TARGETS.contains(target)) {
            List<Entity> heads = headBodyParts(client, target, body);

            // どのヘッドからたどっても同じ顔ぶれになるので、1つでも表示済みなら同じ個体。
            // 経路(スキン照合とネームタグ)が違うヘッドに行き着いても、ネームプレートは1つで済む
            for (Entity head : heads) {
                if (seaCreaturePartsShown.contains(head)) return;
            }
            seaCreaturePartsShown.addAll(heads);

            if (!heads.isEmpty()) {
                highlightBody = false;
                for (Entity head : heads) {
                    if (target.highlight()) registerHighlight(head, target);
                    // 型判定側で拾い直さないよう、この個体は取られたものとして控える
                    nametagClaimedEntities.add(head);
                }
                heads.sort(Comparator.comparingInt(Entity::getId));
                visual = headBodyAnchor(target, heads);
            }
        }

        if (target == MobVisual.SeaCreature.FROG_PRINCE) {
            // 土台のスライムも見えているので、カエルと一緒に光らせる。
            // ネームプレートと Tracer は、上に乗っているカエルの側に出す
            Entity mount = body.getVehicle();
            if (mount != null) {
                if (target.highlight()) registerHighlight(mount, target);
                // 型判定側で拾い直さないよう、この個体は取られたものとして控える
                nametagClaimedEntities.add(mount);
            }
        }

        if (target == MobVisual.SeaCreature.THE_LOCH_EMPEROR) {
            Entity rider = lochEmperorRider(body);
            if (rider != null) {
                visual = rider;
                // 土台のガーディアンも光らせる
                if (target.highlight()) registerHighlight(body, target);
                // 型判定側で拾い直さないよう、この個体は取られたものとして控える
                nametagClaimedEntities.add(body);
            }
        }

        nametagClaimedEntities.add(visual);
        if (highlightBody && target.highlight()) registerHighlight(visual, target);
        registerTracer(visual, target);
        if (target.nameplate()) {
            String label = BossNameplateRenderer.colorCode(target.tracerColorARGB()) + "§l" + target.plainLabel();
            nameplateEntities.put(visual, BossNameplateRenderer.buildLabel(label,
                            nameplateHealth(visual, target)));
        }
        announceSeaCreature(client, target, visual);
    }

    /**
     * 登録済みのヘッドスキンが見つからなかった Sea Creature を知らせる。
     *
     * スキンはネームタグより遠くまで届くので、
     * ネームタグがあるのに見つからないのは Hypixel 側で差し替えられたときだけ。
     * ただしラグでネームタグだけ先に届くことがあるので、
     * 見つからない状態が続いてから初めて知らせる。
     * 同じネームタグで毎 tick 出さないよう、1度出したらそれで終わり
     */
    private static void reportMissingHeadSkin(Minecraft client, Entity nameTag, MobVisual target) {
        reportMissingVisual(client, nameTag, target.plainLabel(), "known head skin");
    }

    // 実体名で見つかるはずのプレイヤーエンティティ型モブが、見つからないことを知らせる
    private static void reportMissingNamedPlayer(Minecraft client, Entity nameTag, MobVisual target) {
        reportMissingVisual(client, nameTag, target.plainLabel(), "named entity");
    }

    // ネームタグはあるのに、そのモブの本体が見つからないことを知らせる
    private static void reportMissingBody(Minecraft client, Entity nameTag, MobVisual target) {
        reportMissingVisual(client, nameTag, target.plainLabel(), "matching entity");
    }

    // 同じく本体が見つからないが、MobVisual ではないボス用
    private static void reportMissingBody(Minecraft client, Entity nameTag, String bossName) {
        reportMissingVisual(client, nameTag, bossName, "matching entity");
    }

    // 同じく実体名で探すが、MobVisual ではない Crimson Isle のボス用
    private static void reportMissingNamedPlayer(Minecraft client, Entity nameTag, String bossName) {
        reportMissingVisual(client, nameTag, bossName, "named entity");
    }

    private static void reportMissingVisual(Minecraft client, Entity nameTag, String name, String what) {
        if (missingVisualReported.contains(nameTag)) return;

        long now = System.currentTimeMillis();
        MissingWindow window = missingVisualWindows.get(nameTag);
        // 途中で見つかっていた(呼ばれない tick があった)なら、数え直す
        if (window == null || now - window.last > MISSING_VISUAL_GAP_MS) {
            missingVisualWindows.put(nameTag, new MissingWindow(now));
            return;
        }

        window.last = now;
        if (now - window.since < MISSING_VISUAL_GRACE_MS) return;

        missingVisualReported.add(nameTag);
        NotificationUtils.sendSystemChat(client, Component.literal(
                "§c" + name + ": " + what + " not found. "
                        + "§7Hypixel may have changed it."));
    }

    // スキンが見つからない状態の、数え始めと最後に確かめた時刻
    private static final class MissingWindow {
        private final long since;
        private long last;

        private MissingWindow(long now) {
            this.since = now;
            this.last = now;
        }
    }

    /**
     * Tracer とネームプレートを出すヘッドを選ぶ。
     *
     * Titanoboa は頭の skull が分かっているので、届いている間は頭に出す。
     * 頭がまだ届いていない間は、代わりに体の真ん中に1つだけ出す。
     * 表示位置が飛ばないよう、代表は毎 tick 同じヘッドになるように選ぶ。
     * エンティティIDは湧いた順に振られるので、ID順に並べた真ん中がモブの中ほどにあたる
     */
    private static Entity headBodyAnchor(MobVisual target, List<Entity> heads) {
        String headSkin = HEAD_ANCHOR_SKINS.get(target);
        if (headSkin != null) {
            for (Entity head : heads) {
                if (matchesHeadSkin(head, headSkin)) return head;
            }
        }
        return heads.get(heads.size() / 2);
    }

    /**
     * 複数のヘッドで1体を成す Sea Creature の、ヘッドをすべて集める。
     *
     * Titanoboa は登録済みのスキンを被っているものを、
     * それ以外は隣り合って連なっているものを集める。
     * どのヘッドを起点にしても同じ顔ぶれになるので、同じ個体かどうかの判定に使える
     */
    private static List<Entity> headBodyParts(Minecraft client, MobVisual target, Entity anyHead) {
        if (HEAD_SKIN_GROUPED_TARGETS.contains(target)) return skinMatchedHeads(client, anyHead, target);
        return new ArrayList<>(connectedHeadStands(client, anyHead, target, HEAD_PARTS_SEARCH_RADIUS));
    }

    // 近くにある、その Sea Creature のスキンを被ったヘッド。
    // 見た目が複数の部品でできているモブを、1体としてまとめて扱うために使う
    private static List<Entity> skinMatchedHeads(Minecraft client, Entity body, MobVisual target) {
        AABB box = body.getBoundingBox().inflate(HEAD_PARTS_SEARCH_RADIUS);

        List<Entity> parts = new ArrayList<>();
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, box, stand -> true)) {
            if (headSkinTarget(stand) == target) parts.add(stand);
        }
        return parts;
    }

    /**
     * 被っているヘッドのスキンから Sea Creature を引く。ヘッドを被っていなければ null。
     *
     * 当たり判定が同じ形で型では分けられない面々を、見た目のテクスチャで見分けるために使う。
     */
    private static MobVisual headSkinTarget(Entity entity) {
        // ヘッドを被せたアーマースタンドだけでなく、
        // Water Hydra のようにモブ本体が被っていることもある。
        // 毎 tick すべてのエンティティを見るので、見る型は必要なものだけに絞る
        for (String value : headSkinValues(entity)) {
            MobVisual target = cachedHeadSkinTarget(value);
            if (target != null) return target;
        }
        return null;
    }

    // 被っているヘッドが、その skull か。
    // 同じ Sea Creature の中で部位を見分けるのに使う
    private static boolean matchesHeadSkin(Entity entity, String skinId) {
        for (String value : headSkinValues(entity)) {
            if (decodeSkinTexture(value).contains(skinId)) return true;
        }
        return false;
    }

    // 被っているヘッドのテクスチャ値。ヘッドを被っていなければ空
    private static List<String> headSkinValues(Entity entity) {
        if (!(entity instanceof ArmorStand) && !(entity instanceof Zombie)) return List.of();

        LivingEntity wearer = (LivingEntity) entity;
        if (wearer.getCustomName() != null) return List.of();

        return skinValues(wearer.getItemBySlot(EquipmentSlot.HEAD));
    }

    // アイテムに付いている skull からモブを引く。skull でなければ null。
    // 見た目を Display で作っているモブを見分けるのに使う
    private static MobVisual itemSkinTarget(ItemStack stack) {
        for (String value : skinValues(stack)) {
            MobVisual target = cachedHeadSkinTarget(value);
            if (target != null) return target;
        }
        return null;
    }

    // アイテムに付いている skull のテクスチャ値。skull でなければ空
    private static List<String> skinValues(ItemStack stack) {
        if (stack.isEmpty()) return List.of();

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return List.of();

        List<String> values = new ArrayList<>();
        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (property != null) values.add(property.value());
        }
        return values;
    }

    // 当たり判定の近くにある Display が被っている skull から、モブを引く
    private static MobVisual safariDisplaySkinTarget(Minecraft client, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(HITBOX_HEAD_RADIUS);
        for (Display.ItemDisplay display : client.level.getEntitiesOfClass(Display.ItemDisplay.class, box, e -> true)) {
            MobVisual target = itemSkinTarget(displayItem(display));
            if (SAFARI_SKIN_TARGETS.contains(target)) return target;
        }
        return null;
    }

    // スキンのテクスチャ値から Sea Creature を引く。1度見た値は覚えておく
    private static MobVisual cachedHeadSkinTarget(String value) {
        Optional<MobVisual> cached = HEAD_SKIN_CACHE.get(value);
        if (cached != null) return cached.orElse(null);

        // 見たことのないスキンばかりの場面でも、覚える量が増え続けないようにする
        if (HEAD_SKIN_CACHE.size() >= MAX_HEAD_SKIN_CACHE) HEAD_SKIN_CACHE.clear();

        String decoded = decodeSkinTexture(value);
        MobVisual found = null;
        for (Map.Entry<String, MobVisual> entry : HEAD_SKIN_TARGETS.entrySet()) {
            if (decoded.contains(entry.getKey())) {
                found = entry.getValue();
                break;
            }
        }
        HEAD_SKIN_CACHE.put(value, Optional.ofNullable(found));
        return found;
    }

    // The Loch Emperor の上に乗っているスケルトン。見つからなければ null
    private static Entity lochEmperorRider(Entity body) {
        for (Entity passenger : body.getPassengers()) {
            if (passenger instanceof Skeleton) return passenger;
        }
        return null;
    }

    // The Loch Emperor の土台か。スケルトンを乗せたガーディアンで1体を成す
    private static boolean isLochEmperorBody(Entity entity) {
        if (!(entity instanceof Guardian) || entity instanceof ElderGuardian) return false;
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Skeleton) return true;
        }
        return false;
    }

    // Sea Creature として扱う型。エリアと設定で対象が変わるため、毎 tick 作り直す判定に使う
    private static boolean isSeaCreatureTypeEntity(Entity entity) {
        return seaCreatureTypeTarget(entity) != null;
    }

    // エリア固有モブとして扱う型。対象がエリアや変種で変わるため、毎 tick 作り直す判定に使う
    private static boolean isAreaAnimalEntity(Entity entity) {
        return entity instanceof Axolotl || entity instanceof Frog
                || entity instanceof Parrot || entity instanceof Armadillo
                || entity instanceof Rabbit || entity instanceof Creaking
                || entity instanceof Fox || entity instanceof Hoglin
                || entity instanceof Ghast || entity instanceof Goat
                || entity instanceof Vex || entity instanceof Bee
                || entity instanceof Cod || entity instanceof Salmon
                || entity instanceof Dolphin || entity instanceof GlowSquid
                || entity instanceof Turtle || entity instanceof Pufferfish
                || entity instanceof Tadpole || entity instanceof Panda
                || entity instanceof Phantom || entity instanceof TropicalFish
                || entity instanceof Sniffer || entity instanceof Endermite
                || entity instanceof Bat || entity instanceof Drowned
                || entity instanceof Stray
                || entity instanceof Parched
                || entity instanceof Silverfish || entity instanceof CaveSpider
                || entity instanceof PolarBear || entity instanceof SnowGolem;
    }

    // Tracer は Highlight と独立して切り替えられる。対象に入っていれば線の色を登録する
    private static void registerTracer(Entity entity, MobVisual target) {
        registerTracer(entity, target, target.tracerColorARGB());
    }

    private static void registerTracer(Entity entity, MobVisual target, int colorARGB) {
        if (target.tracer()) addTracer(entity, target, colorARGB);
    }

    // Crimson 系はエントリ側の設定(= Mob Visuals のリスト)をそのまま使う
    private static void registerTracer(Entity entity, CrimsonBossEntry boss) {
        if (boss.enableTracer().get()) addTracer(entity, boss.nameTag(), boss.tracerColorARGB());
    }

    // Tracer の対象を登録する。既定では線が乱立しないよう、同じモブ(key)の中では
    // 自分に最も近い1体だけに絞る。走査順は検出方法ごとにばらばらなので、
    // より近い個体が来たら差し替える形で絞り込む。
    // 設定が All のときは絞り込まず、見つかったモブすべてに線を引く
    private static void addTracer(Entity entity, Object key, int colorARGB) {
        if (entity == null) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (ModConfig.INSTANCE.mobVisuals.tracerMode == ModConfig.MobVisualsCategory.TracerMode.ALL) {
            tracerEntities.put(entity, colorARGB);
            return;
        }

        Entity current = tracerNearest.get(key);
        if (current != null) {
            if (current.distanceToSqr(player) <= entity.distanceToSqr(player)) return;
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
    private static String findNearbyHealth(Minecraft client, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(GLARE_NAMETAG_RADIUS);
        Entity closest = null;
        double nearest = Double.MAX_VALUE;
        for (Entity candidate : client.level.getEntitiesOfClass(Entity.class, box, e -> e.getCustomName() != null)) {
            String nameStr = candidate.getCustomName().getString();
            if (!ModConstants.containsIgnoreCase(nameStr, MAGMA_GLARE_NAME)) continue;
            if (!HEALTH_PATTERN.matcher(nameStr).find()) continue;
            double dist = candidate.distanceToSqr(entity.position());
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
