package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.golem.IronGolem;
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
        highlightedEntities.clear();

        if (client.level == null || client.player == null) return;

        // ★修正: 設定がOFFの場合はそもそもスキャン対象外にする
        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConfig.INSTANCE.golem.enableGolemHighlight;

        boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage) && ModConfig.INSTANCE.broodmother.enableBroodmotherHighlight;

        if (!scanGolem && !scanBroodmother) return;

        // Yarn: client.world.getEntities() -> Mojang: client.level.entitiesForRendering()
        for (Entity entity : client.level.entitiesForRendering()) {
            Component customName = entity.getCustomName();
            if (customName != null) {
                String nameStr = customName.getString();

                if (scanGolem && nameStr.contains("End Stone Protector")) {
                    // Yarn: expand(8.0) -> Mojang: inflate(8.0)
                    AABB searchBox = entity.getBoundingBox().inflate(8.0);
                    // Yarn: getEntitiesByClass -> Mojang: getEntitiesOfClass
                    List<IronGolem> golems = client.level.getEntitiesOfClass(IronGolem.class, searchBox, e -> true);

                    Entity closestGolem = getClosestEntity(golems, entity);
                    if (closestGolem != null) {
                        highlightedEntities.add(closestGolem);
                    }
                }

                if (scanBroodmother && nameStr.contains("Broodmother")) {
                    AABB searchBox = entity.getBoundingBox().inflate(8.0);

                    List<Spider> spiders = client.level.getEntitiesOfClass(Spider.class, searchBox, e -> true);

                    Entity closestSpider = getClosestEntity(spiders, entity);
                    if (closestSpider != null) {
                        highlightedEntities.add(closestSpider);
                    }
                }
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