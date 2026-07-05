package com.deeply.gankura.data;

import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.regex.Pattern;

public class ModConstants {
    public static final String LOGGER_NAME = "HypixelMod";

    // エリア判定用
    public static final String GAME_TYPE_SKYBLOCK = "SKYBLOCK";
    public static final String MODE_COMBAT_3 = "combat_3"; // The End
    public static final String MAP_THE_END = "The End";

    // ★追加: Spider's Den 判定用
    public static final String MODE_COMBAT_1 = "combat_1";
    public static final String MAP_SPIDERS_DEN = "Spider's Den";

    // ★追加: Crimson Isle 判定用
    public static final String MODE_CRIMSON_ISLE = "crimson_isle";
    public static final String MAP_CRIMSON_ISLE = "Crimson Isle";

    // ステージ名
    public static final String STAGE_RESTING = "Resting";
    public static final String STAGE_DORMANT = "Dormant";
    public static final String STAGE_AGITATED = "Agitated";
    public static final String STAGE_DISTURBED = "Disturbed";
    public static final String STAGE_AWAKENING = "Awakening"; // Stage 4
    public static final String STAGE_SUMMONED = "Summoned";   // Stage 5

    // 正規表現・メッセージ
    public static final Pattern PROTECTOR_PATTERN = Pattern.compile("Protector:\\s*(.+)");

    // ★追加: Broodmother のタブリストスキャン用パターン
    public static final Pattern BROODMOTHER_PATTERN = Pattern.compile("Broodmother:\\s+(.+)");

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
    public static final Pattern DAMAGE_PATTERN = Pattern.compile("Your Damage: ([\\d,]+) \\(Position #([\\d,]+)\\)");

    // 1位〜3位のDPS計算 & Loot Quality用
    // "1st Damager - [MVP+] Name - 1,000,000" や "2nd Damager - Name - 800,000" に対応
    public static final Pattern TOP_DAMAGER_PATTERN = Pattern.compile("(1st|2nd|3rd) Damager - (?:.*\\] )?([A-Za-z0-9_]+) - ([\\d,]+)");

    // "Zealots Contributed: 50/100"
    public static final Pattern ZEALOT_PATTERN = Pattern.compile("Zealots Contributed: ([\\d,]+)/100");

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
    public static final Pattern EYE_PLACED_CHAT_PATTERN = Pattern.compile("placed a Summoning Eye! \\((\\d)/8\\)");
    public static final Pattern EYE_PLACED_8_CHAT_PATTERN = Pattern.compile("placed a Summoning Eye! Brace yourselves! \\(8/8\\)");
    public static final Pattern EYE_PLACED_TAB_PATTERN = Pattern.compile("Eyes placed: (\\d)/8");
    public static final Pattern DRAGON_TYPE_TAB_PATTERN = Pattern.compile("Dragon: \\((.+)\\)");
    public static final Pattern DRAGON_SPAWN_PATTERN = Pattern.compile("The .*?(Protector|Old|Unstable|Young|Strong|Wise|Superior) Dragon has spawned!");
    public static final Pattern DRAGON_DOWN_PATTERN = Pattern.compile("([A-Za-z]+) DRAGON DOWN!");
    public static final String DRAGON_EGG_SPAWNED_MSG = "The Dragon Egg has spawned!";
}