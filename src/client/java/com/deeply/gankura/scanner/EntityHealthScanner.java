package com.deeply.gankura.scanner;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityHealthScanner {
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");
    private static final Pattern MAGMA_PERCENT_PATTERN = Pattern.compile("Boss: (\\d{1,3})%", Pattern.CASE_INSENSITIVE);
    // ARACHNE DOWN!もSanctuary内でしか表示されないため、検知できないだけで撃破済みと誤判定しないよう
    // この時間継続して未検知の場合のみ撃破済みと確定する
    private static final long NOT_DETECTED_CONFIRM_KILL_MS = 3000L;
    private static String previousMagmaSpawnStatus = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    private static void scan(Minecraft client) {
        if (client.level == null || client.player == null) return;

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);

        boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);
        // ARACHNE DOWN! 検知後から次の Calling までは存在しないため、確認自体を行わない
        boolean scanArachne = GameState.Arachne.inSanctuary && !GameState.Arachne.isReady;

        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);

        if (!scanGolem) GameState.Golem.health = null;
        if (!scanBroodmother) GameState.Broodmother.health = null;
        if (!scanArachne) {
            GameState.Arachne.health = null; GameState.Arachne.isDetected = false; GameState.Arachne.broodCount = 0;
            GameState.Arachne.notDetectedSinceMillis = 0;
        }

        // Crimson Isle にいない場合は全ボスの HP をクリア
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
                boss.setHealth().accept(null);
            }
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !isCrimsonIsle) return;

        if (isCrimsonIsle) scanMagmaBossScoreboard(client);

        // Crimson Isle ボスごとにスキャン対象かを確認
        boolean[] scanCrimson = new boolean[EntityHighlightManager.CRIMSON_BOSSES.size()];
        boolean anyCrimsonScan = false;
        if (isCrimsonIsle) {
            for (int i = 0; i < EntityHighlightManager.CRIMSON_BOSSES.size(); i++) {
                scanCrimson[i] = EntityHighlightManager.CRIMSON_BOSSES.get(i).getIsDetected().get();
                if (scanCrimson[i]) anyCrimsonScan = true;
            }
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !anyCrimsonScan) return;

        String foundGolemHealth = null;
        String foundBroodmotherHealth = null;
        String foundArachneHealth = null;
        boolean foundArachne = false;
        int foundBroodCount = 0;
        String foundSize = GameState.Arachne.size;
        String[] foundCrimsonHealth = new String[EntityHighlightManager.CRIMSON_BOSSES.size()];

        AABB scanBox = client.player.getBoundingBox().inflate(50.0);
        for (Entity entity : client.level.getEntitiesOfClass(Entity.class, scanBox, e -> true)) {
            Component customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanGolem && foundGolemHealth == null && ModConstants.PROTECTOR_ENTITY_NAME_PATTERN.matcher(nameStr).find()) {
                Matcher m = HEALTH_PATTERN.matcher(nameStr);
                if (m.find()) foundGolemHealth = m.group(1);
            }

            if (scanBroodmother && foundBroodmotherHealth == null
                    && (ModConstants.containsIgnoreCase(nameStr, "Brood Mother") || ModConstants.containsIgnoreCase(nameStr, "Broodmother"))) {
                Matcher m = HEALTH_PATTERN.matcher(nameStr);
                if (m.find()) foundBroodmotherHealth = m.group(1);
            }

            if (scanArachne && ModConstants.isArachneBossName(nameStr)) {
                foundArachne = true;
                if (foundArachneHealth == null) {
                    Matcher m = HEALTH_PATTERN.matcher(nameStr);
                    if (m.find()) foundArachneHealth = m.group(1);
                }
                if (foundSize == null) {
                    if (ModConstants.containsIgnoreCase(nameStr, "Lv300")) foundSize = "Small";
                    else if (ModConstants.containsIgnoreCase(nameStr, "Lv500")) foundSize = "Big";
                }
            }

            if (scanArachne && ModConstants.isArachneBroodName(nameStr)) {
                foundBroodCount++;
                if (foundSize == null) {
                    if (ModConstants.containsIgnoreCase(nameStr, "Lv100")) foundSize = "Small";
                    else if (ModConstants.containsIgnoreCase(nameStr, "Lv200")) foundSize = "Big";
                }
            }

            if (anyCrimsonScan) {
                for (int i = 0; i < EntityHighlightManager.CRIMSON_BOSSES.size(); i++) {
                    if (!scanCrimson[i] || foundCrimsonHealth[i] != null) continue;
                    CrimsonBossEntry boss = EntityHighlightManager.CRIMSON_BOSSES.get(i);
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Matcher m = HEALTH_PATTERN.matcher(nameStr);
                        if (m.find()) {
                            foundCrimsonHealth[i] = m.group(1);
                        } else if ("Magma Boss".equals(boss.nameTag())) {
                            String bar = extractBarHealth(customName);
                            if (bar != null) foundCrimsonHealth[i] = bar;
                        }
                    }
                }
            }
        }

        if (scanGolem) GameState.Golem.health = foundGolemHealth;
        if (scanBroodmother) GameState.Broodmother.health = foundBroodmotherHealth;
        if (scanArachne) {
            GameState.Arachne.health = foundArachneHealth;
            // Arachne本体だけでなくBroodの検知もSpawned判定に含める(分裂後は本体エンティティが消えるため)
            boolean nowDetected = foundArachne || foundBroodCount > 0;
            GameState.Arachne.isDetected = nowDetected;
            GameState.Arachne.broodCount = foundBroodCount;
            GameState.Arachne.size = foundSize;

            if (nowDetected) {
                GameState.Arachne.notDetectedSinceMillis = 0;
                // スポーン確定メッセージはSanctuary内でしかチャットに表示されないため、
                // エリア外にいた間に見逃していても、実際にArachne本体かBroodを検知できた時点でスポーン済みと確定する。
                // ただし撃破直後にすぐ次の召喚が始まった場合、消え際の古いエンティティを新しいスポーンの
                // 確定として誤検知しないよう、目標スポーン時刻に達してから初めて確定する
                boolean spawnTimeReached = !GameState.Arachne.awaitingCrystalParticles
                        && client.level.getGameTime() >= GameState.Arachne.spawnTargetTime;
                if (GameState.Arachne.isSummoning && !GameState.Arachne.hasSpawned && spawnTimeReached) {
                    GameState.Arachne.hasSpawned = true;
                }
            } else {
                // ARACHNE DOWN!も同様にSanctuary内でしか表示されないため、既にスポーンしているはず(hasSpawned)、
                // あるいは目標時刻を過ぎている(isSummoning)のに検知できない場合はエリア外で撃破された可能性がある。
                // パケット到着順のズレ等による誤検知を避けるため、一定時間継続して未検知の場合のみ撃破済みと確定する
                boolean shouldBeVisible = GameState.Arachne.hasSpawned
                        || (GameState.Arachne.isSummoning && !GameState.Arachne.awaitingCrystalParticles
                                && client.level.getGameTime() >= GameState.Arachne.spawnTargetTime);
                if (shouldBeVisible) {
                    if (GameState.Arachne.notDetectedSinceMillis == 0) {
                        GameState.Arachne.notDetectedSinceMillis = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - GameState.Arachne.notDetectedSinceMillis > NOT_DETECTED_CONFIRM_KILL_MS) {
                        GameState.Arachne.isReady = true;
                        GameState.Arachne.isSummoning = false;
                        GameState.Arachne.hasSpawned = false;
                        GameState.Arachne.size = null;
                        GameState.Arachne.notDetectedSinceMillis = 0;
                    }
                } else {
                    GameState.Arachne.notDetectedSinceMillis = 0;
                }
            }
        }
        if (anyCrimsonScan) {
            for (int i = 0; i < EntityHighlightManager.CRIMSON_BOSSES.size(); i++) {
                if (scanCrimson[i]) EntityHighlightManager.CRIMSON_BOSSES.get(i).setHealth().accept(foundCrimsonHealth[i]);
            }
        }
    }

    // Crimson Isle 滞在中に毎 tick スコアボードをスキャンして Magma Boss の状態を更新する
    private static void scanMagmaBossScoreboard(Minecraft client) {
        if (client.level == null) { GameState.MagmaBoss.spawnStatus = null; return; }
        String found = null;
        for (PlayerTeam team : client.level.getScoreboard().getPlayerTeams()) {
            String line = (team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString())
                    .replaceAll("§[0-9a-fk-or]", "");
            Matcher m = MAGMA_PERCENT_PATTERN.matcher(line);
            if (m.find()) { found = m.group(1) + "%"; break; }
            if (ModConstants.containsIgnoreCase(line, "Kill the Magmas:"))       { found = "Kill the Magmas"; break; }
            if (ModConstants.containsIgnoreCase(line, "Boss Health:"))           { found = "Final Stage";     break; }
            if (ModConstants.containsIgnoreCase(line, "The boss is reforming!")) { found = "Reforming...";    break; }
        }
        // Final Stage 以外で値が変化した場合にタイトル表示
        if (found != null && !"Final Stage".equals(found) && !found.equals(previousMagmaSpawnStatus)) {
            if (ModConfig.INSTANCE.crimsonIsle.enableMagmaBossSpawnTitle) {
                MutableComponent title = Component.literal("§c§l" + found);
                NotificationUtils.showTitle(client, title, null);
            }
        }
        previousMagmaSpawnStatus = found;
        GameState.MagmaBoss.spawnStatus = found;
    }

    // "Magma Boss" 以降のセグメントからバー文字のみをホワイトリストで抽出する
    private static String extractBarHealth(Component text) {
        StringBuilder bar = new StringBuilder();
        boolean[] afterBossName = {false};
        text.visit((Style style, String str) -> {
            if (!afterBossName[0]) {
                if (ModConstants.containsIgnoreCase(str, "Magma Boss")) afterBossName[0] = true;
                return Optional.empty();
            }
            StringBuilder filtered = new StringBuilder();
            for (char ch : str.toCharArray()) {
                if (isBarChar(ch)) filtered.append(ch);
            }
            if (filtered.length() == 0) return Optional.empty();
            TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getValue();
                for (ChatFormatting f : ChatFormatting.values()) {
                    TextColor legacy = TextColor.fromLegacyFormat(f);
                    if (legacy != null && legacy.getValue() == rgb) {
                        bar.append(f);
                        break;
                    }
                }
            }
            bar.append(filtered);
            return Optional.empty();
        }, Style.EMPTY);
        return bar.length() > 0 ? "BAR:" + bar : null;
    }

    // バー文字として認める文字種
    private static boolean isBarChar(char ch) {
        return ch == '|'
            || (ch >= '▀' && ch <= '▟')
            || (ch >= '■' && ch <= '◿')
            || (ch >= '─' && ch <= '╿');
    }
}
