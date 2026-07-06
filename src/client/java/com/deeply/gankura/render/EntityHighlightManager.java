package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class EntityHighlightManager {

    public static final Set<Entity> highlightedEntities = new HashSet<>();
    public static final Map<Entity, CrimsonBossEntry> crimsonBossEntities = new HashMap<>();
    public static final Set<Entity> magmaGlareEntities = new HashSet<>();
    public static final Set<Entity> arachneEntities = new HashSet<>();

    // Ashfang のフォロワー3種（ハイライト・トレーサーの個別設定付き）
    public static final List<CrimsonBossEntry> ASHFANG_FOLLOWERS = List.of(
        new CrimsonBossEntry("Ashfang Follower",
            0x555555, 0xFF555555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangFollowerHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangFollowerTracer,
            () -> GameState.AshfangFollower.isDetected,
            d -> GameState.AshfangFollower.isDetected = d,
            () -> null,
            h -> {}),
        new CrimsonBossEntry("Ashfang Acolyte",
            0x5555FF, 0xFF5555FF,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangAcolyteHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangAcolyteTracer,
            () -> GameState.AshfangAcolyte.isDetected,
            d -> GameState.AshfangAcolyte.isDetected = d,
            () -> null,
            h -> {}),
        new CrimsonBossEntry("Ashfang Underling",
            0xFF5555, 0xFFFF5555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangUnderlingHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangUnderlingTracer,
            () -> GameState.AshfangUnderling.isDetected,
            d -> GameState.AshfangUnderling.isDetected = d,
            () -> null,
            h -> {})
    );

    public static final List<CrimsonBossEntry> CRIMSON_BOSSES = List.of(
        new CrimsonBossEntry("Bladesoul",
            0x555555, 0xFF555555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBladesoulHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBladesoulTracer,
            () -> GameState.Bladesoul.isDetected,
            d -> GameState.Bladesoul.isDetected = d,
            () -> GameState.Bladesoul.health,
            h -> GameState.Bladesoul.health = h),
        new CrimsonBossEntry("Barbarian Duke X",
            0xFF5555, 0xFFFF5555,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBarbarianHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableBarbarianTracer,
            () -> GameState.BarbarianDukeX.isDetected,
            d -> GameState.BarbarianDukeX.isDetected = d,
            () -> GameState.BarbarianDukeX.health,
            h -> GameState.BarbarianDukeX.health = h),
        new CrimsonBossEntry("Mage Outlaw",
            0xAA00AA, 0xFFAA00AA,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMageOutlawHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMageOutlawTracer,
            () -> GameState.MageOutlaw.isDetected,
            d -> GameState.MageOutlaw.isDetected = d,
            () -> GameState.MageOutlaw.health,
            h -> GameState.MageOutlaw.health = h),
        new CrimsonBossEntry("Ashfang",
            0xAAAAAA, 0xFFAAAAAA,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableAshfangTracer,
            () -> GameState.Ashfang.isDetected,
            d -> GameState.Ashfang.isDetected = d,
            () -> GameState.Ashfang.health,
            h -> GameState.Ashfang.health = h),
        new CrimsonBossEntry("Magma Boss",
            0xFFAA00, 0xFFFFAA00,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMagmaBossHighlight,
            () -> ModConfig.INSTANCE.crimsonIsle.enableMagmaBossTracer,
            () -> GameState.MagmaBoss.isDetected,
            d -> GameState.MagmaBoss.isDetected = d,
            () -> GameState.MagmaBoss.health,
            h -> GameState.MagmaBoss.health = h)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> updateHighlights(client));
    }

    private static void updateHighlights(Minecraft client) {
        // Crimson ボス関連マップは毎 tick クリアして再検出（エンティティ参照が変わるため）
        highlightedEntities.removeIf(e -> crimsonBossEntities.containsKey(e) || magmaGlareEntities.contains(e));
        crimsonBossEntities.clear();
        magmaGlareEntities.clear();

        if (client.level == null || client.player == null) {
            highlightedEntities.clear();
            return;
        }

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConfig.INSTANCE.theEnd.enableGolemHighlight;

        boolean isSpidersDen = ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage) && ModConfig.INSTANCE.spidersDen.enableBroodmotherHighlight;
        boolean scanArachne = GameState.Arachne.inSanctuary && ModConfig.INSTANCE.spidersDen.enableArachneHighlight;

        boolean scanDragon = isTheEnd && GameState.Dragon.type != null && ModConfig.INSTANCE.theEnd.enableDragonHighlight;

        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map) || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        boolean scanCrimsonBosses = isCrimsonIsle && CRIMSON_BOSSES.stream().anyMatch(b -> b.enableHighlight().get());
        boolean scanMagmaGlare = isCrimsonIsle
                && "Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)
                && ModConfig.INSTANCE.crimsonIsle.enableMagmaBossHighlight;
        boolean scanAshfangFollowers = isCrimsonIsle
                && ASHFANG_FOLLOWERS.stream().anyMatch(f -> f.enableHighlight().get() || f.enableTracer().get());

        // 条件が無効になったカテゴリのエンティティを削除
        if (!scanGolem)       highlightedEntities.removeIf(e -> e instanceof IronGolem);
        if (!scanBroodmother) highlightedEntities.removeIf(e -> e instanceof Spider);
        if (!scanDragon)      highlightedEntities.removeIf(e -> e instanceof EnderDragon);
        if (!scanArachne)     { highlightedEntities.removeAll(arachneEntities); arachneEntities.clear(); }

        // ワールドから削除済みのエンティティを削除
        highlightedEntities.removeIf(Entity::isRemoved);
        arachneEntities.removeIf(Entity::isRemoved);

        // Crimson Isle にいない場合は isDetected をリセット
        if (!isCrimsonIsle) {
            for (CrimsonBossEntry boss : CRIMSON_BOSSES) boss.setIsDetected().accept(false);
        }

        if (!scanGolem && !scanBroodmother && !scanArachne && !scanDragon && !scanCrimsonBosses && !scanMagmaGlare && !scanAshfangFollowers) return;

        boolean[] bossFound = new boolean[CRIMSON_BOSSES.size()];
        boolean[] followerFound = new boolean[ASHFANG_FOLLOWERS.size()];

        for (Entity entity : client.level.entitiesForRendering()) {
            Component customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanGolem && ModConstants.PROTECTOR_ENTITY_NAME_PATTERN.matcher(nameStr).find()) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<IronGolem> golems = client.level.getEntitiesOfClass(IronGolem.class, searchBox, e -> true);
                Entity closest = getClosestEntity(golems, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanBroodmother && ModConstants.containsIgnoreCase(nameStr, "Broodmother")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<Spider> spiders = client.level.getEntitiesOfClass(Spider.class, searchBox, e -> true);
                Entity closest = getClosestEntity(spiders, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanArachne && ModConstants.containsIgnoreCase(nameStr, "Arachne")) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne");
                if (visualTarget != null) {
                    highlightedEntities.add(visualTarget);
                    arachneEntities.add(visualTarget);
                }
            }

            if (scanMagmaGlare && ModConstants.containsIgnoreCase(nameStr, "Magma Glare")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                Entity cube = getClosestEntity(client.level.getEntitiesOfClass(MagmaCube.class, searchBox, e -> true), entity);
                if (cube != null) { highlightedEntities.add(cube); magmaGlareEntities.add(cube); }
            }

            if (scanDragon && ModConstants.containsIgnoreCase(nameStr, "Dragon")) {
                AABB searchBox = entity.getBoundingBox().inflate(32.0);
                List<EnderDragon> dragons = client.level.getEntitiesOfClass(EnderDragon.class, searchBox, e -> true);
                Entity closest = getClosestEntity(dragons, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanCrimsonBosses) {
                for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                    CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                    if (!boss.enableHighlight().get()) continue;
                    // "Kill the Magmas" フェーズ中は Magma Boss 本体をスキップし Magma Glare のみ対象にする
                    if (scanMagmaGlare && "Magma Boss".equals(boss.nameTag())) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, boss.nameTag());
                        if (visualTarget != null) {
                            highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, boss);
                        }

                        // Bladesoul: Blaze + Wither Skeleton の合体構成のため近傍の Wither Skeleton も対象にする
                        if ("Bladesoul".equals(boss.nameTag())) {
                            AABB box = entity.getBoundingBox().inflate(10.0);
                            for (WitherSkeleton ws : client.level.getEntitiesOfClass(WitherSkeleton.class, box, e -> true)) {
                                highlightedEntities.add(ws);
                                crimsonBossEntities.put(ws, boss);
                            }
                        }

                        bossFound[i] = true;
                    }
                }
            }

            if (scanAshfangFollowers) {
                for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                    CrimsonBossEntry follower = ASHFANG_FOLLOWERS.get(i);
                    if (!follower.enableHighlight().get() && !follower.enableTracer().get()) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, follower.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, follower.nameTag());
                        if (visualTarget != null) {
                            if (follower.enableHighlight().get()) highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, follower);
                            followerFound[i] = true;
                        }
                    }
                }
            }
        }

        if (scanDragon && highlightedEntities.stream().noneMatch(e -> e instanceof EnderDragon)) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof EnderDragon) highlightedEntities.add(entity);
            }
        }

        // isDetected 状態を更新
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CRIMSON_BOSSES.get(i).setIsDetected().accept(bossFound[i]);
            }
            for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                ASHFANG_FOLLOWERS.get(i).setIsDetected().accept(followerFound[i]);
            }
        }
    }

    private static Entity findVisualEntity(Minecraft client, Entity namedEntity, String bossName) {
        if (!(namedEntity instanceof ArmorStand)) return namedEntity;
        AABB box = namedEntity.getBoundingBox().inflate(8.0);

        // Magma Boss: MagmaCube のみを対象とし、ジャンプ中の位置ずれに対応するため広めに探索
        // Kill the Magmas フェーズ中は単体エンティティが存在せず Magma Glare と誤マッチするためスキップ
        if ("Magma Boss".equals(bossName)) {
            if ("Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)) return null;
            AABB wideBox = namedEntity.getBoundingBox().inflate(20.0);
            return getClosestEntity(client.level.getEntitiesOfClass(MagmaCube.class, wideBox, e -> true), namedEntity);
        }

        // Barbarian Duke X と Mage Outlaw は Player エンティティ型のボス。
        // Mob 検索をスキップして直接 Player を探す（近傍 Mob に誤ってマッチするのを防ぐ）
        boolean isPlayerTypeBoss = "Barbarian Duke X".equals(bossName) || "Mage Outlaw".equals(bossName);
        if (!isPlayerTypeBoss) {
            // Skeleton 系はボスの視覚エンティティとして扱わない
            // （Bladesoul の Wither Skeleton は別途明示的に追加されるため影響なし）
            Entity closest = getClosestEntity(
                    client.level.getEntitiesOfClass(Mob.class, box, e -> !(e instanceof AbstractSkeleton)),
                    namedEntity);
            if (closest != null) return closest;
        }

        Entity closest = getClosestEntity(
                client.level.getEntitiesOfClass(Player.class, box, e -> e != client.player),
                namedEntity);
        if (closest != null) return closest;

        return getClosestEntity(client.level.getEntitiesOfClass(ArmorStand.class, box,
                e -> e != namedEntity && e.getCustomName() == null), namedEntity);
    }

    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity e : entities) {
            double dist = e.distanceToSqr(center);
            if (dist < minDistance) {
                minDistance = dist;
                closest = e;
            }
        }
        return closest;
    }
}
