package com.deeply.gankura.data;

import net.minecraft.util.math.BlockPos;

public class GameState {

    public static class Server {
        public static String id = "Unknown";
        public static String gametype = "Unknown";
        public static String mode = "Unknown";
        public static String map = "Unknown";

        public static long lastTimePacket = 0;
        public static long lastPacketArrivalMillis = 0;

        public static double tps = 20.0;
        public static long tpsWindowStartMillis = 0;
        public static long tpsWindowStartTicks = 0;

        public static boolean isClosing = false;
        public static String closingTime = null;
        public static long lastWorldJoinTime = 0;

        public static void reset() {
            id = "Unknown"; gametype = "Unknown"; mode = "Unknown"; map = "Unknown";
            isClosing = false; closingTime = null;
            lastWorldJoinTime = System.currentTimeMillis();
        }
    }

    public static class Player {
        public static String locationName = "None";
        public static BlockPos locationPos = null;
        public static String activePetName = "§8Scanning...";

        public static int crimsonStack = 0; public static boolean isCrimsonBold = false;
        public static int terrorStack = 0;  public static boolean isTerrorBold = false;
        public static int hollowStack = 0;  public static boolean isHollowBold = false;
        public static int fervorStack = 0;  public static boolean isFervorBold = false;
        public static int auroraStack = 0;  public static boolean isAuroraBold = false;
        public static long lastArmorStackUpdateTime = 0;

        public static String activePoison = "NONE";
        public static int activePoisonCount = 0;

        public static boolean hasShownDropAlert = false;
        public static boolean isLootScanning = false;

        public static void reset() {
            locationName = "None"; locationPos = null; activePetName = "§8Scanning...";
            crimsonStack = 0; isCrimsonBold = false; terrorStack = 0; isTerrorBold = false;
            hollowStack = 0; isHollowBold = false; fervorStack = 0; isFervorBold = false;
            auroraStack = 0; isAuroraBold = false; lastArmorStackUpdateTime = 0;
            activePoison = "NONE"; activePoisonCount = 0;
            hasShownDropAlert = false; isLootScanning = false;
        }
    }

    public static class Golem {
        public static String stage = ModConstants.STAGE_RESTING;
        public static boolean isScanning = true;
        public static boolean hasRisen = false;
        public static long stage4StartTime = 0;
        public static long stage5TargetTime = 0;
        public static String health = null;
        public static boolean hasAnnouncedDay30 = false;

        public static long fightStartTime = 0; public static long fightEndTime = 0;
        public static long lastFirstPlaceDamage = 0; public static int lastZealotKills = 0;

        public static String top1Name = null; public static long top1Damage = 0;
        public static String top2Name = null; public static long top2Damage = 0;
        public static String top3Name = null; public static long top3Damage = 0;

        public static void reset() {
            stage = ModConstants.STAGE_RESTING; isScanning = true; hasRisen = false;
            stage4StartTime = 0; stage5TargetTime = 0; health = null; hasAnnouncedDay30 = false;
            fightStartTime = 0; fightEndTime = 0; lastFirstPlaceDamage = 0; lastZealotKills = 0;
            top1Name = null; top1Damage = 0; top2Name = null; top2Damage = 0; top3Name = null; top3Damage = 0;
        }
    }

    public static class Dragon {
        public static String eggState = "Scanning...";
        public static int eyes = 0;
        public static int playerEyes = 0;
        public static String type = null;
        public static long spawnTargetTime = 0;
        public static long lastChatTime = 0;
        public static long fightStartTime = 0; public static long fightEndTime = 0;

        public static String top1Name = null; public static long top1Damage = 0;
        public static String top2Name = null; public static long top2Damage = 0;
        public static String top3Name = null; public static long top3Damage = 0;

        public static void reset() {
            eggState = "Scanning..."; eyes = 0; playerEyes = 0; type = null;
            spawnTargetTime = 0; lastChatTime = 0; fightStartTime = 0; fightEndTime = 0;
            top1Name = null; top1Damage = 0; top2Name = null; top2Damage = 0; top3Name = null; top3Damage = 0;
        }
    }

    public static class Broodmother {
        public static String stage = "Scanning...";
        public static long stage4StartTime = 0;
        public static String health = null;

        public static void reset() {
            stage = "Scanning...";
            stage4StartTime = 0;
            health = null;
        }
    }

    public static class Arachne {
        public static boolean isSummoning = false;
        public static long spawnTargetTime = 0;
        public static boolean hasSpawned = false;
        public static boolean inSanctuary = false; // スコアボードに「Arachne's Sanctuary」の行が検出された場合 true
        public static boolean isReady = false; // 「ARACHNE DOWN!」検知後、次の召喚まで true
        public static boolean isDetected = false; // Sanctuary内でArachneエンティティを検知できている間 true
        public static String health = null;
        public static int broodCount = 0; // Arachne's Brood の残りエンティティ数
        public static String size = null; // Lvl300/Lvl100 なら "Small"、Lvl500/Lvl200 なら "Big"
        public static boolean awaitingCrystalParticles = false; // Arachne Crystal使用後、Quick/Normal判定のパーティクル受信待ちか
        public static void reset() {
            isSummoning = false; spawnTargetTime = 0; hasSpawned = false; inSanctuary = false;
            isReady = false; isDetected = false; health = null; broodCount = 0; size = null;
            awaitingCrystalParticles = false;
        }
    }

    public static class BarbarianDukeX {
        public static boolean isDetected = false;
        public static String health = null;
        public static long respawnEndTime = 0;
        public static void reset() { isDetected = false; health = null; respawnEndTime = 0; }
    }

    public static class Bladesoul {
        public static boolean isDetected = false;
        public static String health = null;
        public static long respawnEndTime = 0;
        public static void reset() { isDetected = false; health = null; respawnEndTime = 0; }
    }

    public static class MageOutlaw {
        public static boolean isDetected = false;
        public static String health = null;
        public static long respawnEndTime = 0;
        public static void reset() { isDetected = false; health = null; respawnEndTime = 0; }
    }

    public static class Ashfang {
        public static boolean isDetected = false;
        public static String health = null;
        public static long respawnEndTime = 0;
        public static void reset() { isDetected = false; health = null; respawnEndTime = 0; }
    }

    public static class AshfangFollower {
        public static boolean isDetected = false;
        public static void reset() { isDetected = false; }
    }

    public static class AshfangAcolyte {
        public static boolean isDetected = false;
        public static void reset() { isDetected = false; }
    }

    public static class AshfangUnderling {
        public static boolean isDetected = false;
        public static void reset() { isDetected = false; }
    }

    public static class MagmaBoss {
        public static boolean isDetected = false;
        public static String health = null;
        public static long respawnEndTime = 0;
        /** スコアボードから検出したスポーン状態。null=未スポーン、"75%"/"Kill the Magmas"/"Final Stage" */
        public static String spawnStatus = null;
        public static void reset() { isDetected = false; health = null; respawnEndTime = 0; spawnStatus = null; }
    }

    public static class CrimsonDrop {
        public static boolean isScanning = false;
        public static String killedBoss = null;
        public static boolean hasShownAlert = false;
        public static void reset() { isScanning = false; killedBoss = null; hasShownAlert = false; }
    }

    public static class Warp {
        public static long cooldownEndAt = 0;
        public static String queuedCommand = null;
        public static boolean awaitingConfirmation = false;
        public static long awaitingConfirmationSince = 0;
        public static void reset() { cooldownEndAt = 0; queuedCommand = null; awaitingConfirmation = false; awaitingConfirmationSince = 0; }
    }

    public static void resetAll() {
        Server.reset();
        Player.reset();
        Golem.reset();
        Dragon.reset();
        Broodmother.reset();
        Arachne.reset();
        BarbarianDukeX.reset();
        Bladesoul.reset();
        MageOutlaw.reset();
        Ashfang.reset();
        AshfangFollower.reset();
        AshfangAcolyte.reset();
        AshfangUnderling.reset();
        MagmaBoss.reset();
        CrimsonDrop.reset();
        // Warp のクールダウンは /warp 自体がワールド移動を伴うため、resetAll() の対象から除外する
    }
}