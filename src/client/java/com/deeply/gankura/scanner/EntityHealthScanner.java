package com.deeply.gankura.scanner;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.util.ScoreboardUtils;
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityHealthScanner {
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");
    private static final Pattern MAGMA_PERCENT_PATTERN = Pattern.compile("Boss: (\\d{1,3})%", Pattern.CASE_INSENSITIVE);
    // Dragonのネームタグには残りHPが載らないため、サイドバーの「Dragon HP: 4,824,217❤」から取得する
    private static final Pattern DRAGON_HP_PATTERN = Pattern.compile("Dragon HP:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);
    // Golem(Endstone Protector)は遠距離だとネームタグを読めないため、サイドバーの
    // 「Protector HP: 1,234,567❤」を代替に使う(ネームタグが読めた場合はそちらを優先)
    private static final Pattern PROTECTOR_HP_PATTERN = Pattern.compile("Protector HP:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);
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
        // Sanctuary外ではスキャンを行わない(エリア外は常にUnknown扱いとし、Sanctuary入場時に改めて判定する)
        GameState.Arachne.webAreaLoaded = GameState.Arachne.inSanctuary && client.world.isPosLoaded(ModConstants.ARACHNE_WEB_POS);
        GameState.Arachne.cobwebDetected = GameState.Arachne.webAreaLoaded && client.world.getBlockState(ModConstants.ARACHNE_WEB_POS).isOf(Blocks.COBWEB);
        // Sanctuary内かつ蜘蛛の巣を検知できている間のみスキャンする(スポーン前・撃破後は存在しないため)
        boolean scanArachne = GameState.Arachne.inSanctuary && GameState.Arachne.cobwebDetected;
        // 蜘蛛の巣もカウントダウンもSoon表示も無い完全なReady状態になったら、
        // ARACHNE DOWN!を見逃していても古いSizeの表示が残らないようクリアする(判定不能の間はクリアしない)
        boolean confirmedReadyNow = GameState.Arachne.webAreaLoaded && !GameState.Arachne.cobwebDetected
                && !GameState.Arachne.isSummoning && !GameState.Arachne.arachneMessageSeen;
        if (confirmedReadyNow) {
            GameState.Arachne.size = null;
        }
        // Sanctuary内で状態を確定できたら、エリア外に出た後の表示に使うためRead/Spawned系状態をラッチしておく
        if (GameState.Arachne.inSanctuary && GameState.Arachne.cobwebDetected) {
            GameState.Arachne.everConfirmed = true;
            GameState.Arachne.lastConfirmedWasReady = false;
        } else if (confirmedReadyNow) {
            GameState.Arachne.everConfirmed = true;
            GameState.Arachne.lastConfirmedWasReady = true;
        }
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map) || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);

        // Dragonのみサイドバー由来のため、他のスキャン条件に関係なく毎tick更新する
        GameState.Dragon.health = isTheEnd ? findSidebarHealth(client, DRAGON_HP_PATTERN) : null;
        // Golemはネームタグ優先。読めなかったときのフォールバックとして先に取得しておく
        String golemSidebarHealth = isTheEnd ? findSidebarHealth(client, PROTECTOR_HP_PATTERN) : null;

        if (!scanGolem) GameState.Golem.health = null;
        if (!scanBroodmother) GameState.Broodmother.health = null;
        if (!scanArachne) { GameState.Arachne.health = null; GameState.Arachne.broodCount = 0; }

        // Crimson Isle bosses: clear health when not in area
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : EntityHighlightManager.CRIMSON_BOSSES) {
                boss.setHealth().accept(null);
            }
            GameState.MagmaBoss.healthLabel = null;
            GameState.MagmaBoss.inArena = false;
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
                    // Magma Boss はサイドバー由来なので、ネームタグからは読まない
                    if ("Magma Boss".equals(boss.nameTag())) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Matcher m = HEALTH_PATTERN.matcher(nameStr);
                        if (m.find()) foundCrimsonHealth[i] = m.group(1);
                    }
                }
            }
        }

        // ネームタグは「現在HP/最大HP」まで取れるので優先し、遠距離等で読めない場合のみサイドバーの現在HPを使う
        if (scanGolem) GameState.Golem.health = foundGolemHealth != null ? foundGolemHealth : golemSidebarHealth;
        if (scanBroodmother) GameState.Broodmother.health = foundBroodmotherHealth;
        if (scanArachne) {
            GameState.Arachne.health = foundArachneHealth;
            GameState.Arachne.broodCount = foundBroodCount;
            GameState.Arachne.size = foundSize;
        }
        if (anyCrimsonScan) {
            for (int i = 0; i < EntityHighlightManager.CRIMSON_BOSSES.size(); i++) {
                CrimsonBossEntry boss = EntityHighlightManager.CRIMSON_BOSSES.get(i);
                // Magma Boss は scanMagmaBossScoreboard 側で更新済みのため上書きしない
                if ("Magma Boss".equals(boss.nameTag())) continue;
                if (scanCrimson[i]) boss.setHealth().accept(foundCrimsonHealth[i]);
            }
        }
    }

    // Crimson Isle 滞在中に毎 tick スコアボードをスキャンし Magma Boss の状態を更新する
    // サイドバーの「<名前> HP: <数値>❤」形式の行から現在HPを取り出す(見つからなければ null)。
    // ボスが生存している間のみ表示される行なので、消えたら未検出として扱ってよい
    private static String findSidebarHealth(MinecraftClient client, Pattern pattern) {
        if (client.world == null) return null;
        for (Team team : client.world.getScoreboard().getTeams()) {
            String line = (team.getPrefix().getString() + team.getSuffix().getString())
                    .replaceAll("§[0-9a-fk-or]", "");
            Matcher m = pattern.matcher(line);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    // Crimson Isle 滞在中に毎 tick サイドバーをスキャンして Magma Boss の状態とHP表示を更新する。
    //
    // Magma Boss はネームタグに残りHPが載らないため、Dragon と同じくサイドバー由来にしている。
    // サイドバーの構成(いずれも「ラベル行の1つ下」がプログレスバー):
    //   §7Boss: §c45% / §7Damage Soaked: / §a▎▎▎▎▎§7▎▎▎▎▎...   … 本体が分裂していない状態
    //   §6Kill the Magmas: / §a▎▎▎§7▎▎▎...                      … 分裂中
    //   §cThe boss is reforming!                                 … 再結合中(HPは非表示)
    //   §7Boss Health: / §e389.6k§f/§a10M§c❤                     … 最終フェーズ
    private static void scanMagmaBossScoreboard(MinecraftClient client) {
        if (client.world == null) {
            GameState.MagmaBoss.spawnStatus = null;
            GameState.MagmaBoss.health = null;
            GameState.MagmaBoss.healthLabel = null;
            GameState.MagmaBoss.inArena = false;
            return;
        }

        List<String> lines = ScoreboardUtils.getSidebarLines(client);
        boolean inArena = false;
        String percent = null;
        String soakedBar = null;
        String killMagmasBar = null;
        String bossHealthBar = null;
        boolean reforming = false;

        for (int i = 0; i < lines.size(); i++) {
            String plain = ScoreboardUtils.stripColor(lines.get(i));
            // ラベル行の1つ下が実際のバー。色コードを含んだまま利用する
            String next = (i + 1 < lines.size()) ? lines.get(i + 1) : null;

            Matcher m = MAGMA_PERCENT_PATTERN.matcher(plain);
            if (m.find()) percent = m.group(1) + "%";

            if (ModConstants.containsIgnoreCase(plain, ModConstants.AREA_MAGMA_CHAMBER)) inArena = true;
            if (ModConstants.containsIgnoreCase(plain, "Damage Soaked:"))        soakedBar = next;
            if (ModConstants.containsIgnoreCase(plain, "Kill the Magmas:"))      killMagmasBar = next;
            if (ModConstants.containsIgnoreCase(plain, "Boss Health:"))          bossHealthBar = next;
            if (ModConstants.containsIgnoreCase(plain, "The boss is reforming!")) reforming = true;
        }

        // フェーズの進行順に沿って優先度を決める(古い行が残っていても後段のフェーズを優先する)
        String found;
        String health;
        String label = null;
        if (reforming) {
            found = "Reforming...";
            health = null;                       // 再結合中はHP HUDを隠す
        } else if (bossHealthBar != null) {
            found = "Final Stage";
            health = raw(bossHealthBar);
        } else if (killMagmasBar != null) {
            found = "Kill the Magmas";
            health = raw(killMagmasBar);
            label = "Kill the Magmas";           // タイトルを差し替える
        } else {
            found = percent;
            health = raw(soakedBar);
        }

        // Final Stage 以外で値が変化した場合にタイトル表示
        if (found != null && !"Final Stage".equals(found) && !found.equals(previousMagmaSpawnStatus)
                && ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTitle) {
            MutableText title = Text.literal(found).formatted(Formatting.RED, Formatting.BOLD);
            NotificationUtils.showTitle(client, title, null);
        }
        previousMagmaSpawnStatus = found;
        GameState.MagmaBoss.spawnStatus = found;
        GameState.MagmaBoss.health = health;
        GameState.MagmaBoss.healthLabel = label;
        GameState.MagmaBoss.inArena = inArena;
    }

    // サイドバーの行は色コード込みで完成しているため、整形せずそのまま表示する印を付ける
    private static String raw(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        return trimmed.isEmpty() ? null : ModConstants.RAW_HEALTH_PREFIX + trimmed;
    }

}
