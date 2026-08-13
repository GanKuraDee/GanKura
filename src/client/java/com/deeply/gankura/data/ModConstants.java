package com.deeply.gankura.data;

import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.regex.Pattern;

public class ModConstants {
    public static final String LOGGER_NAME = "HypixelMod";

    // HP文字列の先頭に付ける印。サイドバーから取得済みで色コードも含んでいるため、
    // 表示側で数値パース・色付けを行わずそのまま描画することを示す
    public static final String RAW_HEALTH_PREFIX = "RAW:";

    // エリア判定用。値はタブリストの "Area: " 行に出る表示名そのもの
    public static final String GAME_TYPE_SKYBLOCK = "SKYBLOCK";
    public static final String MAP_THE_END = "The End";
    public static final String MAP_SPIDERS_DEN = "Spider's Den";
    public static final String MAP_CRIMSON_ISLE = "Crimson Isle";
    // Critter Safari のエリア名はタブリスト上では "Safari" とだけ表示される
    public static final String MAP_SAFARI = "Safari";
    // シュルカー(Hideon系)が敵として出現するエリア
    public static final String MAP_MOONGLADE_MARSH = "Moonglade Marsh";
    public static final String MAP_TORRHUS_CANYON = "Torrhus Canyon";

    // タブリスト上で現在地とサーバーIDを載せている行の接頭辞
    public static final String TAB_AREA_PREFIX = "Area:";
    public static final String TAB_SERVER_PREFIX = "Server:";

    // サイドバーのタイトル。"SKYBLOCK CO-OP" のような派生表記があるため部分一致で判定する
    public static final String SIDEBAR_SKYBLOCK_TITLE = "SKYBLOCK";

    // ステージ名
    public static final String STAGE_RESTING = "Resting";
    public static final String STAGE_DORMANT = "Dormant";
    public static final String STAGE_AGITATED = "Agitated";
    public static final String STAGE_DISTURBED = "Disturbed";
    public static final String STAGE_AWAKENING = "Awakening"; // Stage 4
    public static final String STAGE_SUMMONED = "Summoned";   // Stage 5

    // 正規表現・メッセージ
    // Hypixel側の表記ゆれ（大文字小文字）に対応するため、chat/tabの文言にマッチする正規表現には
    // 基本的に Pattern.CASE_INSENSITIVE を付与する
    public static final Pattern PROTECTOR_PATTERN = Pattern.compile("Protector:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    // ★追加: Broodmother のタブリストスキャン用パターン
    public static final Pattern BROODMOTHER_PATTERN = Pattern.compile("Broodmother:\\s+(.+)", Pattern.CASE_INSENSITIVE);

    // Hypixel側の表記ゆれ ("End Stone Protector" / "Endstone Protector") 両方に対応する共通の名前パターン
    private static final String PROTECTOR_NAME_REGEX = "End ?Stone Protector";

    // エンティティ名でのマッチング用 (EntityHealthScanner, EntityHighlightManager)
    public static final Pattern PROTECTOR_ENTITY_NAME_PATTERN = Pattern.compile(PROTECTOR_NAME_REGEX, Pattern.CASE_INSENSITIVE);

    // Stage 5 (Spawn Timer Start)
    public static final Pattern GOLEM_SPAWN_PATTERN = Pattern.compile(
            "The ground begins to shake as an " + PROTECTOR_NAME_REGEX + " rises from below!", Pattern.CASE_INSENSITIVE);

    // DPS計測用 (Fight Start / End)
    public static final Pattern GOLEM_RISE_PATTERN = Pattern.compile(
            "BEWARE - An " + PROTECTOR_NAME_REGEX + " has risen!", Pattern.CASE_INSENSITIVE);
    public static final Pattern GOLEM_DOWN_PATTERN = Pattern.compile(
            PROTECTOR_NAME_REGEX + " DOWN!", Pattern.CASE_INSENSITIVE);

    // ダメージ取得用 ("Your Damage: 1,234,567 (Position #5)")
    public static final Pattern DAMAGE_PATTERN = Pattern.compile("Your Damage: ([\\d,]+) \\(Position #([\\d,]+)\\)", Pattern.CASE_INSENSITIVE);

    // 1位〜3位のDPS計算 & Loot Quality用
    // "1st Damager - [MVP+] Name - 1,000,000" や "2nd Damager - Name - 800,000" に対応
    public static final Pattern TOP_DAMAGER_PATTERN = Pattern.compile("(1st|2nd|3rd) Damager - (?:.*\\] )?([A-Za-z0-9_]+) - ([\\d,]+)", Pattern.CASE_INSENSITIVE);

    // "Zealots Contributed: 50/100"
    public static final Pattern ZEALOT_PATTERN = Pattern.compile("Zealots Contributed: ([\\d,]+)/100", Pattern.CASE_INSENSITIVE);

    // Crimson Isle ボスの固定スポーン座標
    public static final BlockPos BLADESOUL_POS        = new BlockPos(-295, 83,  -518);
    public static final BlockPos ASHFANG_POS           = new BlockPos(-485, 137, -1016);
    public static final BlockPos MAGMA_BOSS_POS        = new BlockPos(-369, 66,  -805);
    public static final BlockPos MAGE_OUTLAW_POS       = new BlockPos(-181, 106, -860);
    public static final BlockPos BARBARIAN_DUKE_X_POS  = new BlockPos(-537, 117, -905);

    // ゴーレムの祭壇の座標リスト
    public record GolemSpot(String name, BlockPos pos) {}
    public static final List<GolemSpot> GOLEM_SPOTS = List.of(
            new GolemSpot("Middle Front", new BlockPos(-644, 5, -267)),
            new GolemSpot("Right Front", new BlockPos(-639, 5, -326)),
            new GolemSpot("Right Behind", new BlockPos(-678, 5, -330)),
            new GolemSpot("Left", new BlockPos(-649, 5, -217)),
            new GolemSpot("Middle Behind", new BlockPos(-727, 5, -282)),
            new GolemSpot("Middle Center", new BlockPos(-689, 5, -271))
    );

    // =======================================================
    // ドラゴン関連の正規表現
    // =======================================================
    public static final Pattern EYE_PLACED_CHAT_PATTERN = Pattern.compile("placed a Summoning Eye! \\((\\d)/8\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern EYE_PLACED_8_CHAT_PATTERN = Pattern.compile("placed a Summoning Eye! Brace yourselves! \\(8/8\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern EYE_PLACED_TAB_PATTERN = Pattern.compile("Eyes placed: (\\d)/8", Pattern.CASE_INSENSITIVE);
    public static final Pattern DRAGON_TYPE_TAB_PATTERN = Pattern.compile("Dragon: \\((.+)\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern DRAGON_SPAWN_PATTERN = Pattern.compile("The .*?(Protector|Old|Unstable|Young|Strong|Wise|Superior) Dragon has spawned!", Pattern.CASE_INSENSITIVE);
    public static final Pattern DRAGON_DOWN_PATTERN = Pattern.compile("([A-Za-z]+) DRAGON DOWN!", Pattern.CASE_INSENSITIVE);
    public static final String DRAGON_EGG_SPAWNED_MSG = "The Dragon Egg has spawned!";

    // =======================================================
    // Arachne (Spider's Den) 関連のメッセージ
    // =======================================================
    public static final String ARACHNE_CALLING_MSG = "placed an Arachne's Calling! Something is awakening! (4/4)"; // Small
    public static final String ARACHNE_CRYSTAL_MSG = "placed an Arachne Crystal! Something is awakening!"; // Big
    public static final String ARACHNE_DOWN_MSG = "ARACHNE DOWN!";
    // このプレフィックスで始まるメッセージが来たら、蜘蛛の巣未検知時の「間もなくスポーン」の合図として使う
    public static final String ARACHNE_BOSS_MSG_PREFIX = "[BOSS] Arachne";
    // 戦闘中・撃破時のセリフもこのプレフィックスに一致してしまうため、Soon判定から除外する
    private static final String[] ARACHNE_EXCLUDED_QUOTES = {
            "No, this is impossible...",
            "I will be back, even stronger!",
            "Stop resisting.",
            "How can this be...what a humiliation.",
            "This isn't the end. You will learn how resourceful spiders are."
    };

    public static boolean isArachneExcludedQuote(String msg) {
        for (String quote : ARACHNE_EXCLUDED_QUOTES) {
            if (containsIgnoreCase(msg, quote)) return true;
        }
        return false;
    }

    // Arachne Crystal(Big)のQuick/Normal Spawn判定用に観測するパーティクルの基準座標(祭壇)
    public static final BlockPos ARACHNE_ALTAR_POS = new BlockPos(-283, 51, -179);
    // Arachneがスポーンすると、この座標に蜘蛛の巣ブロックが出現する(スポーン確定の判定に使用)
    public static final BlockPos ARACHNE_WEB_POS = new BlockPos(-283, 48, -210);

    // スコアボードに「Arachne's Sanctuary」の行がそのまま表示されるので、それを直接検知する
    public static final String AREA_ARACHNES_SANCTUARY = "Arachne's Sanctuary";
    public static final String AREA_MAGMA_CHAMBER = "Magma Chamber";

    // =======================================================
    // Foraging 関連のチャット
    // =======================================================
    // 通常は木を最後まで切らないと倒せないが、特定の効果が発動すると途中でも一度に切り倒せる
    public static final String TREE_FELLED_MSG = "You felled the entire Tree!";
    // 切り倒した木からモブが降ってきたときに流れる。モブ名は複数種類あるのでキャプチャして表示に使う
    public static final Pattern TREE_MOB_FELL_PATTERN =
            Pattern.compile("A (.+?) fell from the Tree!", Pattern.CASE_INSENSITIVE);

    // Critter Capsule を当てた回数の上限。20回以内に必ずキャプチャできる
    public static final int CAPSULE_MAX_THROWS = 20;
    // "You threw a Critter Capsule at the Wumpa!"
    public static final Pattern CAPSULE_THROW_PATTERN =
            Pattern.compile("You threw a Critter Capsule at the (.+?)!", Pattern.CASE_INSENSITIVE);
    public static final String WUMPA_NAME = "Wumpa";
    public static final String DOOMSPIRAL_NAME = "Doomspiral";

    // Wumpa のキャプチャ成功の合図
    public static final String WUMPA_CAPTURED_MSG = "The cave opens up again";

    // Wumpaに敗れた後、戦闘エリアへ戻るための小さな穴。ウェイポイントとして目印を出す
    public static final BlockPos WUMPA_REENTER_POS = new BlockPos(-95, 84, -65);

    // Doomspiral の儀式。キャンドル4本をともすと出現する。
    // 本数はメッセージ後半の文面から確定させるので、途中を取りこぼしても次の1本で復帰できる
    public static final String DOOMSPIRAL_CANDLE_MSG = "You used the Soothing Incense to light the candle!";
    private static final String[] DOOMSPIRAL_CANDLE_SUFFIXES = {
            "You begin to feel uneasy.",
            "Something is off about this place...",
            "Are you sure you wish to continue?",
            "The ground beneath your feet starts to shift..."
    };
    public static final int DOOMSPIRAL_CANDLE_TOTAL = DOOMSPIRAL_CANDLE_SUFFIXES.length;

    // ともした本数(1〜4)。キャンドルのメッセージでなければ0
    public static int doomspiralCandleCount(String msg) {
        if (!containsIgnoreCase(msg, DOOMSPIRAL_CANDLE_MSG)) return 0;
        for (int i = 0; i < DOOMSPIRAL_CANDLE_SUFFIXES.length; i++) {
            if (containsIgnoreCase(msg, DOOMSPIRAL_CANDLE_SUFFIXES[i])) return i + 1;
        }
        return 0;
    }

    public static final String DOOMSPIRAL_SUMMON_MSG = "Your ritual summoned a Doomspiral into this world";
    // 実際の文面は "The darkness in the Haunted Biome  fades away..." だが、
    // 空白の数が揺れても拾えるよう前半だけを一致条件にする
    public static final String DOOMSPIRAL_CAPTURED_MSG = "The darkness in the Haunted Biome";
    public static final String DOOMSPIRAL_DESPAWN_MSG = "The Doomspiral retreats back underground";

    // Icy Biome に出現する Critter(Wumpa を除く8種)。
    // これらをすべてキャプチャすると Wumpa がスポーンする
    public static final List<String> ICY_BIOME_CRITTERS = List.of(
            "Strongarm", "Tepid", "Polaris", "Shuddersquid",
            "Billygoat", "Mantis Shrimp", "Nozzlenose", "Troodon");

    // "CAPTURE! You caught a Troodon and gained a Troodon Shard!"
    // シャードが複数落ちると "gained 2x Troodon Shard!" のように個数表記へ変わるため、
    // 獲得側の文面は個数・冠詞を問わず素通しにし、捕まえた側の名前だけを取る
    public static final Pattern CRITTER_CAPTURE_PATTERN =
            Pattern.compile("CAPTURE! You caught an? (.+?) and gained .+? Shard!", Pattern.CASE_INSENSITIVE);

    // キャプチャ表示名から対象の Critter を引く。名前は ICY_BIOME_CRITTERS を唯一の定義とし、
    // Hypixel 側の表記ゆれに備えて大文字小文字は無視する
    public static String findIcyBiomeCritter(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        for (String critter : ICY_BIOME_CRITTERS) {
            if (critter.equalsIgnoreCase(trimmed)) return critter;
        }
        return null;
    }

    // Critter Safari の Wumpa スポーン告知。
    // 全文は "You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?" だが、
    // 末尾の煽り文が変わっても拾えるよう、核となる部分だけを一致条件にする
    public static final String WUMPA_SPAWN_MSG = "massive footsteps echoing through the Icy Biome";

    // Hypixel側の大文字小文字の表記ゆれに対応するための共通ヘルパー
    public static boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null
                && source.toLowerCase(java.util.Locale.ROOT).contains(target.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean startsWithIgnoreCase(String source, String prefix) {
        return source != null && prefix != null
                && source.toLowerCase(java.util.Locale.ROOT).startsWith(prefix.toLowerCase(java.util.Locale.ROOT));
    }

    // Sanctuary内には「Arachne」を名前に含む雑魚も存在するため、
    // 実際のボス(Lv300 または Lv500)かどうかをレベル表記の有無で判別する
    public static boolean isArachneBossName(String nameStr) {
        return containsIgnoreCase(nameStr, "Arachne")
                && (containsIgnoreCase(nameStr, "Lv300") || containsIgnoreCase(nameStr, "Lv500"));
    }

    // Arachneはダメージを受けると複数体の Arachne's Brood に分裂する。レベル表記(Lv100/Lv200)の有無で判別する
    public static boolean isArachneBroodName(String nameStr) {
        return containsIgnoreCase(nameStr, "Arachne's Brood")
                && (containsIgnoreCase(nameStr, "Lv100") || containsIgnoreCase(nameStr, "Lv200"));
    }
}