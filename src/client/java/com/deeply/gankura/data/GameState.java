package com.deeply.gankura.data;

import net.minecraft.util.math.BlockPos;

public class GameState {
    // ... (既存の変数はそのまま) ...
    public static String serverId = "Unknown";
    public static String gametype = "Unknown";
    public static String mode = "Unknown";
    public static String map = "Unknown";

    public static String golemStage = ModConstants.STAGE_RESTING;
    public static long stage5TargetTime = 0;

    public static boolean isScanning = false;

    // 戦闘統計
    public static long fightStartTime = 0;
    public static long fightEndTime = 0;
    public static long lastFirstPlaceDamage = 0;
    public static int lastZealotKills = 0;

    // ドロップ通知関連
    public static boolean hasShownDropAlert = false;

    // ★追加: ルートスキャン中かどうか
    public static boolean isLootScanning = false;

    public static String locationName = "None";
    public static BlockPos locationPos = null;

    public static void reset() {
        serverId = "Unknown";
        gametype = "Unknown";
        mode = "Unknown";
        map = "Unknown";
        resetGolemStatus();
    }

    public static void resetGolemStatus() {
        golemStage = ModConstants.STAGE_RESTING;
        stage5TargetTime = 0;

        fightStartTime = 0;
        fightEndTime = 0;

        lastFirstPlaceDamage = 0;
        lastZealotKills = 0;

        hasShownDropAlert = false;

        // ★リセット
        isLootScanning = false;

        locationName = "None";
        locationPos = null;
        isScanning = true;
    }
}