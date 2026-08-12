package com.deeply.gankura.data;

import net.minecraft.core.BlockPos;

public class GameState {

    public static class Server {
        // id/map はタブリストの "Server: " / "Area: " 行、gametype はサイドバーのタイトルから取得する。
        // 取得元が毎tick読めるため、locraw のように参加時1回きりで取りこぼす心配がない
        public static String id = "Unknown";
        public static String gametype = "Unknown";
        public static String map = "Unknown";

        public static long lastTimePacket = 0;
        public static long lastPacketArrivalMillis = 0;

        public static double tps = 20.0;
        public static long tpsWindowStartMillis = 0;
        public static long tpsWindowStartTicks = 0;

        public static boolean isClosing = false;
        public static String closingTime = null;
        public static long lastWorldJoinTime = 0;
        public static long dayTime = 0;

        public static void reset() {
            id = "Unknown"; gametype = "Unknown"; map = "Unknown";
            isClosing = false; closingTime = null;
            lastWorldJoinTime = System.currentTimeMillis();
        }

        // エリア判定はここに集約する。表示名(Area:行の値)が唯一の判定材料なので、
        // Hypixel側の名称が変わった場合もModConstantsの定数1箇所を直せば済むようにしておく
        public static boolean isSkyblock() {
            return ModConstants.GAME_TYPE_SKYBLOCK.equals(gametype);
        }

        public static boolean isTheEnd() {
            return ModConstants.MAP_THE_END.equals(map);
        }

        public static boolean isSpidersDen() {
            return ModConstants.MAP_SPIDERS_DEN.equals(map);
        }

        public static boolean isCrimsonIsle() {
            return ModConstants.MAP_CRIMSON_ISLE.equals(map);
        }

        public static boolean isSafari() {
            return ModConstants.MAP_SAFARI.equals(map);
        }
    }

    public static class Player {
        public static String locationName = "None";
        public static BlockPos locationPos = null;
        public static String activePetName = "§8Scanning...";
        public static long petLevelUpTime = 0;

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
            locationName = "None"; locationPos = null; activePetName = "§8Scanning..."; petLevelUpTime = 0;
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
        // スコアボードの「Dragon HP: 4,824,217❤」から取得する現在HP(カンマ区切りの生文字列)
        public static String health = null;
        public static long spawnTargetTime = 0;
        public static long lastChatTime = 0;

        public static long fightStartTime = 0; public static long fightEndTime = 0;

        public static String top1Name = null; public static long top1Damage = 0;
        public static String top2Name = null; public static long top2Damage = 0;
        public static String top3Name = null; public static long top3Damage = 0;

        public static void reset() {
            eggState = "Scanning..."; eyes = 0; playerEyes = 0; type = null; health = null;
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
        public static long spawnTargetTime = 0; // 目標ワールドTick数 (Golem/Dragonと同様のTPS考慮方式)
        public static boolean inSanctuary = false; // スコアボードに「Arachne's Sanctuary」の行が検出された場合 true
        public static boolean cobwebDetected = false; // 蜘蛛の巣ブロックが基準座標に存在する間 true。これでSpawnedを判定し、エンティティスキャンの実行条件にもなる
        public static boolean webAreaLoaded = false; // 基準座標のチャンクが読み込まれている間 true。falseなら判定不能(Scanning...)
        public static boolean arachneMessageSeen = false; // 「[BOSS] Arachne」で始まるメッセージを検知した間 true(蜘蛛の巣未検知時のSoon表示に使用)
        public static boolean downConfirmed = false; // 「ARACHNE DOWN!」検知で true。次のCalling/Crystalまではチャンク未読み込みでもScanning...にせずReady扱いにする
        public static String health = null;
        public static int broodCount = 0; // Arachne's Brood の残りエンティティ数
        public static String size = null; // Lvl300/Lvl100 なら "Small"、Lvl500/Lvl200 なら "Big"
        public static boolean awaitingCrystalParticles = false; // Arachne Crystal使用後、Quick/Normal判定のパーティクル観測中か
        public static long crystalMessageTime = 0; // Arachne Crystal検知時刻(ミリ秒)
        public static int particleBurstCounter = 0; // 観測中のDUSTパーティクル数
        public static boolean everConfirmed = false; // Sanctuary内で一度でも状態(Ready/Spawning/Spawned)を確定できたか。falseの間はエリア外でUnknown表示
        public static boolean lastConfirmedWasReady = false; // 直近確定した状態がReadyだったか。falseならSpawning/Spawned(エリア外ではSpawned/Killed表示に使う)
        public static void reset() {
            isSummoning = false; spawnTargetTime = 0; inSanctuary = false;
            cobwebDetected = false; webAreaLoaded = false; arachneMessageSeen = false; downConfirmed = false; health = null; broodCount = 0; size = null;
            awaitingCrystalParticles = false; crystalMessageTime = 0; particleBurstCounter = 0;
            everConfirmed = false; lastConfirmedWasReady = false;
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
        public static String health = null;
        public static void reset() { isDetected = false; health = null; }
    }

    public static class AshfangAcolyte {
        public static boolean isDetected = false;
        public static String health = null;
        public static void reset() { isDetected = false; health = null; }
    }

    public static class AshfangUnderling {
        public static boolean isDetected = false;
        public static String health = null;
        public static void reset() { isDetected = false; health = null; }
    }

    public static class MagmaBoss {
        public static boolean isDetected = false;
        public static String health = null;
        // HP HUDのタイトル差し替え。Kill the Magmasフェーズ中のみ非nullになる(nullなら既定の「Magma Boss HP」)
        public static String healthLabel = null;
        // サイドバーに「Magma Chamber」が出ているか(= 戦闘エリア内)。
        // 判定用のフェーズ行が1つも無い状態を「撃破済み」と確定してよいかの前提になる
        public static boolean inArena = false;
        public static long respawnEndTime = 0;
        public static String spawnStatus = null;
        public static void reset() { isDetected = false; health = null; healthLabel = null; inArena = false; respawnEndTime = 0; spawnStatus = null; }
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

    // Doomspiral (Haunted Biome) の儀式の進行状況
    public static class Doomspiral {
        public static final String STATUS_SPAWNING = "Spawning...";
        public static final String STATUS_SPAWNED = "Spawned";
        public static final String STATUS_CAPTURED = "Captured";
        public static final String STATUS_DESPAWNED = "Despawned";

        // ともしたキャンドルの数
        public static int litCandles = 0;
        // 4本ともした後の状態。儀式の途中は null
        public static String status = null;
        // Critter Capsule を当てた回数。湧き直すたびに数え直す
        public static int capsuleHits = 0;

        public static void reset() {
            litCandles = 0;
            status = null;
            capsuleHits = 0;
        }
    }

    // Critter Safari (Icy Biome) のキャプチャ進捗
    public static class CritterSafari {
        public static final String STATUS_SPAWNED = "Spawned";
        public static final String STATUS_CAPTURED = "Captured";

        // キャプチャ済みの Critter 名。8種すべて揃うと Wumpa がスポーンする
        private static final java.util.Set<String> captured = new java.util.HashSet<>();
        // Wumpa 自体の状態。8種そろう前は null
        public static String wumpaStatus = null;
        // Critter Capsule を当てた回数。湧き直すたびに数え直す
        public static int wumpaCapsuleHits = 0;

        public static void markCaptured(String critterName) {
            captured.add(critterName);
        }

        // 複数人で狩っていると自分にキャプチャのメッセージが来ないことがあるため、
        // Wumpa のスポーンが確認できた時点で8種そろったものとして扱う
        public static void markAllCaptured() {
            captured.addAll(ModConstants.ICY_BIOME_CRITTERS);
        }

        public static boolean isCaptured(String critterName) {
            return captured.contains(critterName);
        }

        public static int capturedCount() {
            return captured.size();
        }

        // Wumpa が湧いていて、まだキャプチャされていない状態か
        public static boolean isWumpaSpawned() {
            return STATUS_SPAWNED.equals(wumpaStatus);
        }

        public static void reset() {
            captured.clear();
            wumpaStatus = null;
            wumpaCapsuleHits = 0;
        }
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
        CritterSafari.reset();
        Doomspiral.reset();
        // Warp のクールダウンは /warp 自体がワールド移動を伴うため、resetAll() の対象から除外する
    }
}