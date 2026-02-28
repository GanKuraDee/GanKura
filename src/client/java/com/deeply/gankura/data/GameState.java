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

    // ドロップ通知関連
    public static boolean hasShownDropAlert = false;
    public static boolean isLootScanning = false;

    public static String locationName = "None";
    public static BlockPos locationPos = null;

    // ★追加: 現在アクティブなペット名(カラーコード付き)
    public static String activePetName = null;

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

    public static long lastServerTimePacket = 0;
    public static long lastServerPacketArrivalMillis = 0;

    public static void reset() {
        serverId = "Unknown";
        gametype = "Unknown";
        mode = "Unknown";
        map = "Unknown";
        resetGolemStatus();
        // ★変更: null ではなく、スキャン中の仮テキストを入れておく
        activePetName = "§8Scanning...";
    }

    public static void resetGolemStatus() {
        golemStage = ModConstants.STAGE_RESTING;
        stage5TargetTime = 0;
        stage4StartTime = 0;

        fightStartTime = 0;
        fightEndTime = 0;

        lastFirstPlaceDamage = 0;
        lastZealotKills = 0;

        hasShownDropAlert = false;
        isLootScanning = false;

        // ★追加: リセット時にフラグを戻す
        hasGolemRisen = false;

        locationName = "None";
        locationPos = null;
        isScanning = true;
    }
}