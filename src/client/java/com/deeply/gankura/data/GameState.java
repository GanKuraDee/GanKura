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

    public static void resetAll() {
        Server.reset();
        Player.reset();
        Golem.reset();
        Dragon.reset();
        Broodmother.reset();
    }
}