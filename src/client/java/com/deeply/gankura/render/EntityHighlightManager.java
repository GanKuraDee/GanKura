package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityHighlightManager {

    public static final Set<Entity> highlightedEntities = new HashSet<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> updateHighlights(client));
    }

    private static void updateHighlights(MinecraftClient client) {
        highlightedEntities.clear();

        if (client.world == null || client.player == null) return;

        // ★修正: 設定がOFFの場合はそもそもスキャン対象外にする
        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage) && ModConfig.INSTANCE.golem.enableGolemHighlight;

        boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage) && ModConfig.INSTANCE.broodmother.enableBroodmotherHighlight;

        if (!scanGolem && !scanBroodmother) return;

        for (Entity entity : client.world.getEntities()) {
            Text customName = entity.getCustomName();
            if (customName != null) {
                String nameStr = customName.getString();

                if (scanGolem && nameStr.contains("End Stone Protector")) {
                    Box searchBox = entity.getBoundingBox().expand(8.0);
                    List<IronGolemEntity> golems = client.world.getEntitiesByClass(IronGolemEntity.class, searchBox, e -> true);

                    Entity closestGolem = getClosestEntity(golems, entity);
                    if (closestGolem != null) {
                        highlightedEntities.add(closestGolem);
                    }
                }

                if (scanBroodmother && nameStr.contains("Broodmother")) {
                    Box searchBox = entity.getBoundingBox().expand(8.0);

                    List<SpiderEntity> spiders = client.world.getEntitiesByClass(SpiderEntity.class, searchBox, e -> true);

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
            double dist = e.squaredDistanceTo(center);
            if (dist < minDistance) {
                minDistance = dist;
                closest = e;
            }
        }
        return closest;
    }
}