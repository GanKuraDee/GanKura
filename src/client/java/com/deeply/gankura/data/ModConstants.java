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

    // ステージ名
    public static final String STAGE_RESTING = "Resting";
    public static final String STAGE_DORMANT = "Dormant";
    public static final String STAGE_AGITATED = "Agitated";
    public static final String STAGE_DISTURBED = "Disturbed";
    public static final String STAGE_AWAKENING = "Awakening"; // Stage 4
    public static final String STAGE_SUMMONED = "Summoned";   // Stage 5

    // 正規表現・メッセージ
    public static final Pattern PROTECTOR_PATTERN = Pattern.compile("Protector:\\s*(.+)");

    // Stage 5 (Spawn Timer Start)
    public static final String GOLEM_SPAWN_MSG = "The ground begins to shake as an End Stone Protector rises from below!";

    // DPS計測用 (Fight Start / End)
    public static final String GOLEM_RISE_MSG = "BEWARE - An Endstone Protector has risen!";
    public static final String GOLEM_DOWN_MSG = "END STONE PROTECTOR DOWN!";

    // ダメージ取得用 ("Your Damage: 1,234,567 (Position #5)")
    public static final Pattern DAMAGE_PATTERN = Pattern.compile("Your Damage: ([\\d,]+) \\(Position #([\\d,]+)\\)");

    // ★追加: Loot Quality計算用
    // "1st Damager - Name - 1,000,000"
    public static final Pattern FIRST_PLACE_PATTERN = Pattern.compile("1st Damager - .+ - ([\\d,]+)");

    // "Zealots Contributed: 50/100"
    public static final Pattern ZEALOT_PATTERN = Pattern.compile("Zealots Contributed: ([\\d,]+)/100");

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
    // ★追加: ドラゴンスポーン検知用
    public static final Pattern DRAGON_SPAWN_PATTERN = Pattern.compile("The .*?(Protector|Old|Unstable|Young|Strong|Wise|Superior) Dragon has spawned!");
}