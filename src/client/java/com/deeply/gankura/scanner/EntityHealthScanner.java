package com.deeply.gankura.scanner;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityHealthScanner {
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");
    private static final Pattern MAGMA_PERCENT_PATTERN = Pattern.compile("Boss: (\\d{1,3})%", Pattern.CASE_INSENSITIVE);
    private static String previousMagmaSpawnStatus = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> scan(client));
    }

    private static void scan(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);
        boolean isSpidersDen = ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);
        // Arachneがスポーンすると基準座標に蜘蛛の巣ブロックが出現するため、これでSpawnedを確定する。
        // 遠すぎてチャンクが読み込まれていない場合は判定不能(Scanning...)として区別する
        GameState.Arachne.webAreaLoaded = isSpidersDen && client.world.isPosLoaded(ModConstants.ARACHNE_WEB_POS);
        GameState.Arachne.cobwebDetected = GameState.Arachne.webAreaLoaded && client.world.getBlockState(ModConstants.ARACHNE_WEB_POS).isOf(Blocks.COBWEB);
        // Sanctuary内かつ蜘蛛の巣を検知できている間のみスキャンする(スポーン前・撃破後は存在しないため)
        boolean scanArachne = GameState.Arachne.inSanctuary && GameState.Arachne.cobwebDetected;
        // 蜘蛛の巣もカウントダウンもSoon表示も無い完全なReady状態になったら、
        // ARACHNE DOWN!を見逃していても古いSizeの表示が残らないようクリアする(判定不能の間はクリアしない)
        if (GameState.Arachne.webAreaLoaded && !GameState.Arachne.cobwebDetected
                && !GameState.Arachne.isSummoning && !GameState.Arachne.arachneMessageSeen) {
            GameState.Arachne.size = null;
        }
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map) || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);

        if (!scanGolem) GameState.Golem.health = null;
        if (!scanBroodmother) GameState.Broodmother.health = null;
        if (!scanArachne) { GameState.Arachne.health = null; GameState.Arachne.broodCount = 0; }

        // Crimson Isle bosses: clear health when not in area
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
                boss.setHealth().accept(null);
            }
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !isCrimsonIsle) return;

        if (isCrimsonIsle) scanMagmaBossScoreboard(client);

        // Determine which Crimson Isle bosses need HP scanning
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
        int foundBroodCount = 0;
        String foundSize = GameState.Arachne.size;
        String[] foundCrimsonHealth = new String[EntityHighlightManager.CRIMSON_BOSSES.size()];

        Box scanBox = client.player.getBoundingBox().expand(50.0);
        for (Entity entity : client.world.getEntitiesByClass(Entity.class, scanBox, e -> true)) {
            Text customName = entity.getCustomName();
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
            GameState.Arachne.broodCount = foundBroodCount;
            GameState.Arachne.size = foundSize;
        }
        if (anyCrimsonScan) {
            for (int i = 0; i < EntityHighlightManager.CRIMSON_BOSSES.size(); i++) {
                if (scanCrimson[i]) EntityHighlightManager.CRIMSON_BOSSES.get(i).setHealth().accept(foundCrimsonHealth[i]);
            }
        }
    }

    // Crimson Isle 滞在中に毎 tick スコアボードをスキャンし Magma Boss の状態を更新する
    private static void scanMagmaBossScoreboard(MinecraftClient client) {
        if (client.world == null) { GameState.MagmaBoss.spawnStatus = null; return; }
        String found = null;
        for (Team team : client.world.getScoreboard().getTeams()) {
            String line = (team.getPrefix().getString() + team.getSuffix().getString())
                    .replaceAll("§[0-9a-fk-or]", "");
            Matcher m = MAGMA_PERCENT_PATTERN.matcher(line);
            if (m.find()) { found = m.group(1) + "%"; break; }
            if (ModConstants.containsIgnoreCase(line, "Kill the Magmas:"))       { found = "Kill the Magmas"; break; }
            if (ModConstants.containsIgnoreCase(line, "Boss Health:"))           { found = "Final Stage";     break; }
            if (ModConstants.containsIgnoreCase(line, "The boss is reforming!")) { found = "Reforming...";    break; }
        }
        // Final Stage 以外で値が変化した場合にタイトル表示
        if (found != null && !"Final Stage".equals(found) && !found.equals(previousMagmaSpawnStatus)
                && ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTitle) {
            MutableText title = Text.literal(found).formatted(Formatting.RED, Formatting.BOLD);
            NotificationUtils.showTitle(client, title, null);
        }
        previousMagmaSpawnStatus = found;
        GameState.MagmaBoss.spawnStatus = found;
    }

    // "Magma Boss" 以降のセグメントからバー文字のみをホワイトリストで抽出する
    // 装飾文字（括弧・記号など）は文字種で除外するため、未知の閉じ文字も自動的に除去される
    private static String extractBarHealth(Text text) {
        StringBuilder bar = new StringBuilder();
        boolean[] afterBossName = {false};
        text.visit((Style style, String str) -> {
            if (!afterBossName[0]) {
                if (ModConstants.containsIgnoreCase(str, "Magma Boss")) afterBossName[0] = true;
                return Optional.empty();
            }
            // バー文字のみを取り込む（それ以外は無視）
            StringBuilder filtered = new StringBuilder();
            for (char ch : str.toCharArray()) {
                if (isBarChar(ch)) filtered.append(ch);
            }
            if (filtered.length() == 0) return Optional.empty();
            // 色コードを §x 形式で付与
            TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getRgb();
                for (Formatting f : Formatting.values()) {
                    if (f.isColor() && f.getColorValue() != null && f.getColorValue() == rgb) {
                        bar.append('§').append(f.getCode());
                        break;
                    }
                }
            }
            bar.append(filtered);
            return Optional.empty();
        }, Style.EMPTY);
        return bar.length() > 0 ? "BAR:" + bar : null;
    }

    // バー文字として認める文字種（ブロック・幾何図形・ボックス描画・パイプ）
    private static boolean isBarChar(char ch) {
        return ch == '|'
            || (ch >= '▀' && ch <= '▟')  // Block Elements: █ ░ ▌ 等
            || (ch >= '■' && ch <= '◿')  // Geometric Shapes: ■ □ ▪ 等
            || (ch >= '─' && ch <= '╿'); // Box Drawing Characters
    }
}
