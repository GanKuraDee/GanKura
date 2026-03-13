package com.deeply.gankura.data;

import net.minecraft.util.math.BlockPos;

public class GameState {
    public static String serverId = "Unknown";
    public static String gametype = "Unknown";
    public static String mode = "Unknown";
    public static String map = "Unknown";

    public static String golemStage = ModConstants.STAGE_RESTING;
    public static long stage5TargetTime = 0;
    public static long stage4StartTime = 0;
    public static String golemHealth = null;

    public static boolean isScanning = false;

    // ★追加: ゴーレムが実際にRiseしたかどうか
    public static boolean hasGolemRisen = false;

    // 戦闘統計
    public static long fightStartTime = 0;
    public static long fightEndTime = 0;
    public static long lastFirstPlaceDamage = 0;
    public static int lastZealotKills = 0;

    // ★追加: トップ3のダメージ情報
    public static String top1Name = null;
    public static long top1Damage = 0;
    public static String top2Name = null;
    public static long top2Damage = 0;
    public static String top3Name = null;
    public static long top3Damage = 0;

    // ドロップ通知関連
    public static boolean hasShownDropAlert = false;
    public static boolean isLootScanning = false;

    public static String locationName = "None";
    public static BlockPos locationPos = null;

    // ★追加: 現在アクティブなペット名(カラーコード付き)
    public static String activePetName = null;
    // 変数宣言部に追加
    public static long lastWorldJoinTime = 0;

    // ★追加: アーマースタック情報
    public static int crimsonStack = 0;
    public static boolean isCrimsonBold = false; // ★追加

    public static int terrorStack = 0;
    public static boolean isTerrorBold = false; // ★追加

    public static int hollowStack = 0;
    public static boolean isHollowBold = false; // ★追加

    public static int fervorStack = 0;
    public static boolean isFervorBold = false; // ★追加

    public static int auroraStack = 0;
    public static boolean isAuroraBold = false; // ★追加

    public static long lastArmorStackUpdateTime = 0;

    // ★追加: サーバーリブート情報
    public static boolean isServerClosing = false;
    public static String serverClosingTime = null;

    public static long lastServerTimePacket = 0;
    public static long lastServerPacketArrivalMillis = 0;

    // ★追加: Day30到達時の警告アナウンス済みフラグ
    public static boolean hasAnnouncedDay30 = false;

    // ★変更: Arrow Poisonの所持状態（どちらが先に消費されるか）
    // "NONE", "TOXIC", "TWILIGHT" のいずれかが入る
    public static String activePoison = "NONE";
    // ★追加: 現在アクティブなPoisonの合計所持数
    public static int activePoisonCount = 0;

    // =======================================================
    // ★追加: Dragon Status 関連
    // =======================================================
    public static String dragonEggState = "Scanning..."; // "Ready", "Hatching", "Hatched", "Respawning"
    public static int dragonEyes = 0;
    public static int playerDragonEyes = 0; // ★追加: 自分がはめたEyeの数
    public static String dragonType = null;
    public static long dragonSpawnTargetTime = 0;
    public static long lastDragonChatTime = 0;
    public static long dragonFightStartTime = 0;
    public static long dragonFightEndTime = 0;
    public static String dragonTop1Name = null;
    public static long dragonTop1Damage = 0;
    public static String dragonTop2Name = null;
    public static long dragonTop2Damage = 0;
    public static String dragonTop3Name = null;
    public static long dragonTop3Damage = 0;

    public static void reset() {
        serverId = "Unknown";
        gametype = "Unknown";
        mode = "Unknown";
        map = "Unknown";
        resetGolemStatus();
        // ★変更: null ではなく、スキャン中の仮テキストを入れておく
        activePetName = "§8Scanning...";
        lastWorldJoinTime = System.currentTimeMillis(); // ワールド移動時間を記録

        // ★追加: リセット時にリブート警告も消す
        isServerClosing = false;
        serverClosingTime = null;

        // ★追加: ワールド移動時にアナウンス済みフラグをリセットする
        hasAnnouncedDay30 = false;

        // ★追加: ワールド移動時にアーマースタックの残像を消すために強制リセット
        crimsonStack = 0; isCrimsonBold = false;
        terrorStack = 0;  isTerrorBold = false;
        hollowStack = 0;  isHollowBold = false;
        fervorStack = 0;  isFervorBold = false;
        auroraStack = 0;  isAuroraBold = false;
        lastArmorStackUpdateTime = 0;
        activePoison = "NONE";
        activePoisonCount = 0; // ★追加

        // ★追加: Dragon Status リセット
        dragonEggState = "Scanning...";
        dragonEyes = 0;
        playerDragonEyes = 0; // ★追加
        dragonType = null;
        dragonSpawnTargetTime = 0;
        lastDragonChatTime = 0;
        dragonFightStartTime = 0;
        dragonFightEndTime = 0;
        dragonTop1Name = null; dragonTop1Damage = 0;
        dragonTop2Name = null; dragonTop2Damage = 0;
        dragonTop3Name = null; dragonTop3Damage = 0;
    }

    public static void resetGolemStatus() {
        golemStage = ModConstants.STAGE_RESTING;
        stage5TargetTime = 0;
        stage4StartTime = 0;

        fightStartTime = 0;
        fightEndTime = 0;

        lastFirstPlaceDamage = 0;
        lastZealotKills = 0;

        // ★追加: リセット時にトップ3のデータも消去する
        top1Name = null; top1Damage = 0;
        top2Name = null; top2Damage = 0;
        top3Name = null; top3Damage = 0;

        hasShownDropAlert = false;
        isLootScanning = false;

        // ★追加: リセット時にフラグを戻す
        hasGolemRisen = false;

        locationName = "None";
        locationPos = null;
        isScanning = true;
    }
}