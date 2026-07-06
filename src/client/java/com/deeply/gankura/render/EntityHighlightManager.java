package com.deeply.gankura.render;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.*;

public class EntityHighlightManager {

    public static final Set<Entity> highlightedEntities = new HashSet<>();
    public static final Map<Entity, CrimsonBossEntry> crimsonBossEntities = new HashMap<>();
    public static final Set<Entity> magmaGlareEntities = new HashSet<>();
    public static final Set<Entity> arachneEntities = new HashSet<>();
    public static final Set<Entity> arachneBroodEntities = new HashSet<>();

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
        ClientTickEvents.END_CLIENT_TICK.register(EntityHighlightManager::updateHighlights);
    }

    private static void updateHighlights(MinecraftClient client) {
        highlightedEntities.clear();
        crimsonBossEntities.clear();
        magmaGlareEntities.clear();
        arachneEntities.clear();
        arachneBroodEntities.clear();

        if (client.world == null || client.player == null) return;

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConfig.INSTANCE.theEnd.enableGolemHighlight;
        boolean isSpidersDen = ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage) && ModConfig.INSTANCE.spidersDen.enableBroodmotherHighlight;
        // ARACHNE DOWN! 検知後から次の Calling までは存在しないため、確認自体を行わない
        boolean scanArachne = GameState.Arachne.inSanctuary && !GameState.Arachne.isReady && ModConfig.INSTANCE.spidersDen.enableArachneHighlight;
        boolean scanDragon = isTheEnd && "Hatched".equals(GameState.Dragon.eggState) && ModConfig.INSTANCE.theEnd.enableDragonHighlight;
        boolean isCrimsonIsle = ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map) || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode);
        boolean scanCrimsonBosses = isCrimsonIsle && CRIMSON_BOSSES.stream().anyMatch(b -> b.enableHighlight().get());
        boolean scanMagmaGlare = isCrimsonIsle
                && "Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)
                && ModConfig.INSTANCE.crimsonIsle.enableMagmaBossHighlight;
        boolean scanAshfangFollowers = isCrimsonIsle && ASHFANG_FOLLOWERS.stream().anyMatch(f -> f.enableHighlight().get() || f.enableTracer().get());

        if (!scanGolem && !scanBroodmother && !scanArachne && !scanDragon && !scanCrimsonBosses && !scanMagmaGlare && !scanAshfangFollowers) {
            if (!isCrimsonIsle) {
                for (CrimsonBossEntry boss : CRIMSON_BOSSES) boss.setIsDetected().accept(false);
            }
            return;
        }

        boolean[] bossFound = new boolean[CRIMSON_BOSSES.size()];
        boolean[] followerFound = new boolean[ASHFANG_FOLLOWERS.size()];

        for (Entity entity : client.world.getEntities()) {
            Text customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanGolem && ModConstants.PROTECTOR_ENTITY_NAME_PATTERN.matcher(nameStr).find()) {
                Box box = entity.getBoundingBox().expand(8.0);
                Entity g = getClosestEntity(client.world.getEntitiesByClass(IronGolemEntity.class, box, e -> true), entity);
                if (g != null) highlightedEntities.add(g);
            }

            if (scanBroodmother && ModConstants.containsIgnoreCase(nameStr, "Broodmother")) {
                Box box = entity.getBoundingBox().expand(8.0);
                Entity s = getClosestEntity(client.world.getEntitiesByClass(SpiderEntity.class, box, e -> true), entity);
                if (s != null) highlightedEntities.add(s);
            }

            if (scanArachne && ModConstants.isArachneBossName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne");
                if (visualTarget != null) {
                    highlightedEntities.add(visualTarget);
                    arachneEntities.add(visualTarget);
                }
            }

            if (scanArachne && ModConstants.isArachneBroodName(nameStr)) {
                Entity visualTarget = findVisualEntity(client, entity, "Arachne's Brood");
                if (visualTarget != null) {
                    highlightedEntities.add(visualTarget);
                    arachneBroodEntities.add(visualTarget);
                }
            }

            if (scanMagmaGlare && ModConstants.containsIgnoreCase(nameStr, "Magma Glare")) {
                Box box = entity.getBoundingBox().expand(8.0);
                Entity cube = getClosestEntity(client.world.getEntitiesByClass(MagmaCubeEntity.class, box, e -> true), entity);
                if (cube != null) { highlightedEntities.add(cube); magmaGlareEntities.add(cube); }
            }

            if (scanDragon && ModConstants.containsIgnoreCase(nameStr, "Dragon")) {
                Box box = entity.getBoundingBox().expand(20.0);
                Entity d = getClosestEntity(client.world.getEntitiesByClass(EnderDragonEntity.class, box, e -> true), entity);
                if (d != null) highlightedEntities.add(d);
            }

            if (scanCrimsonBosses) {
                for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                    CrimsonBossEntry boss = CRIMSON_BOSSES.get(i);
                    if (!boss.enableHighlight().get()) continue;
                    if (ModConstants.containsIgnoreCase(nameStr, boss.nameTag())) {
                        Entity visualTarget = findVisualEntity(client, entity, boss.nameTag());
                        if (visualTarget != null) {
                            highlightedEntities.add(visualTarget);
                            crimsonBossEntities.put(visualTarget, boss);
                        }

                        // Bladesoul: Blaze + Wither Skeleton の合体構成のため近傍の Wither Skeleton も対象にする
                        if ("Bladesoul".equals(boss.nameTag())) {
                            Box box = entity.getBoundingBox().expand(10.0);
                            for (WitherSkeletonEntity ws : client.world.getEntitiesByClass(
                                    WitherSkeletonEntity.class, box, e -> true)) {
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

        if (scanDragon && highlightedEntities.stream().noneMatch(e -> e instanceof EnderDragonEntity)) {
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof EnderDragonEntity) highlightedEntities.add(entity);
            }
        }

        // isDetected 状態を更新（タイトルなし）
        if (isCrimsonIsle) {
            for (int i = 0; i < CRIMSON_BOSSES.size(); i++) {
                CRIMSON_BOSSES.get(i).setIsDetected().accept(bossFound[i]);
            }
            for (int i = 0; i < ASHFANG_FOLLOWERS.size(); i++) {
                ASHFANG_FOLLOWERS.get(i).setIsDetected().accept(followerFound[i]);
            }
        }
    }

    private static Entity findVisualEntity(MinecraftClient client, Entity namedEntity, String bossName) {
        if (!(namedEntity instanceof ArmorStandEntity)) return namedEntity;
        Box box = namedEntity.getBoundingBox().expand(8.0);

        // Magma Boss: MagmaCubeEntity のみを対象とし、ジャンプ中の位置ずれに対応するため広めに探索
        // Kill the Magmas フェーズ中は単体エンティティが存在せず Magma Glare と誤マッチするためスキップ
        if ("Magma Boss".equals(bossName)) {
            if ("Kill the Magmas".equals(GameState.MagmaBoss.spawnStatus)) return null;
            Box wideBox = namedEntity.getBoundingBox().expand(20.0);
            return getClosestEntity(client.world.getEntitiesByClass(MagmaCubeEntity.class, wideBox, e -> true), namedEntity);
        }

        // PlayerEntity型ボスは周辺MobによるマッチングをスキップしPlayerEntityを直接検索する
        boolean isPlayerTypeBoss = "Barbarian Duke X".equals(bossName) || "Mage Outlaw".equals(bossName);
        if (!isPlayerTypeBoss) {
            Entity closest = getClosestEntity(client.world.getEntitiesByClass(MobEntity.class, box, e -> true), namedEntity);
            if (closest != null) return closest;
        }

        Entity closest = getClosestEntity(client.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != client.player), namedEntity);
        if (closest != null) return closest;

        return getClosestEntity(client.world.getEntitiesByClass(ArmorStandEntity.class, box,
                e -> e != namedEntity && e.getCustomName() == null), namedEntity);
    }

    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            double d = e.squaredDistanceTo(center);
            if (d < minDist) { minDist = d; closest = e; }
        }
        return closest;
    }
}
