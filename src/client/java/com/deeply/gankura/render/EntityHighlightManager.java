package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityHighlightManager {

    public static final Set<Entity> highlightedEntities = new HashSet<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> updateHighlights(client));
    }

    private static void updateHighlights(Minecraft client) {
        if (client.level == null || client.player == null) {
            highlightedEntities.clear();
            return;
        }

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConfig.INSTANCE.golem.enableGolemHighlight;

        boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage) && ModConfig.INSTANCE.broodmother.enableBroodmotherHighlight;

        boolean scanDragon = isTheEnd && GameState.Dragon.type != null && ModConfig.INSTANCE.dragon.enableDragonHighlight;

        // 条件が無効になったカテゴリのエンティティを削除
        if (!scanGolem)       highlightedEntities.removeIf(e -> e instanceof IronGolem);
        if (!scanBroodmother) highlightedEntities.removeIf(e -> e instanceof Spider);
        if (!scanDragon)      highlightedEntities.removeIf(e -> e instanceof EnderDragon);

        // ワールドから削除済みのエンティティを削除
        highlightedEntities.removeIf(Entity::isRemoved);

        if (!scanGolem && !scanBroodmother && !scanDragon) return;

        // フラスタム内のエンティティから新たに追加（既存エンティティはそのまま維持）
        for (Entity entity : client.level.entitiesForRendering()) {
            Component customName = entity.getCustomName();
            if (customName == null) continue;
            String nameStr = customName.getString();

            if (scanGolem && nameStr.contains("End Stone Protector")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<IronGolem> golems = client.level.getEntitiesOfClass(IronGolem.class, searchBox, e -> true);
                Entity closest = getClosestEntity(golems, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanBroodmother && nameStr.contains("Broodmother")) {
                AABB searchBox = entity.getBoundingBox().inflate(8.0);
                List<Spider> spiders = client.level.getEntitiesOfClass(Spider.class, searchBox, e -> true);
                Entity closest = getClosestEntity(spiders, entity);
                if (closest != null) highlightedEntities.add(closest);
            }

            if (scanDragon && nameStr.contains("Dragon")) {
                AABB searchBox = entity.getBoundingBox().inflate(32.0);
                List<EnderDragon> dragons = client.level.getEntitiesOfClass(EnderDragon.class, searchBox, e -> true);
                Entity closest = getClosestEntity(dragons, entity);
                if (closest != null) highlightedEntities.add(closest);
            }
        }
    }

    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity e : entities) {
            // Yarn: squaredDistanceTo -> Mojang: distanceToSqr
            double dist = e.distanceToSqr(center);
            if (dist < minDistance) {
                minDistance = dist;
                closest = e;
            }
        }
        return closest;
    }
}