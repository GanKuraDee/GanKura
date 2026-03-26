package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
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

        boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
        boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);

        boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
        boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);

        if (!scanGolem && !scanBroodmother) return;

        for (Entity entity : client.world.getEntities()) {
            Text customName = entity.getCustomName();
            if (customName != null) {
                String nameStr = customName.getString();

                // --- Golem の単体ハイライト ---
                if (scanGolem && nameStr.contains("End Stone Protector")) {
                    Box searchBox = entity.getBoundingBox().expand(8.0);
                    List<IronGolemEntity> golems = client.world.getEntitiesByClass(IronGolemEntity.class, searchBox, e -> true);

                    // ★修正: 範囲内のゴーレムの中から、ホログラムに一番近い「1体だけ」を取得する
                    Entity closestGolem = getClosestEntity(golems, entity);
                    if (closestGolem != null) {
                        highlightedEntities.add(closestGolem);
                    }
                }

                // --- Broodmother の単体ハイライト ---
                if (scanBroodmother && nameStr.contains("Broodmother")) {
                    Box searchBox = entity.getBoundingBox().expand(8.0);

                    // マイクラの仕様上、SpiderEntityで検索すればCaveSpider(洞窟グモ)も自動的に含まれます
                    List<SpiderEntity> spiders = client.world.getEntitiesByClass(SpiderEntity.class, searchBox, e -> true);

                    // ★修正: 範囲内のクモの中から、ホログラムに一番近い「1体だけ」を取得する
                    Entity closestSpider = getClosestEntity(spiders, entity);
                    if (closestSpider != null) {
                        highlightedEntities.add(closestSpider);
                    }
                }
            }
        }
    }

    // =======================================================
    // ★追加: リストの中から、基準点(ホログラム)に最も近いエンティティを探す計算メソッド
    // =======================================================
    private static Entity getClosestEntity(List<? extends Entity> entities, Entity center) {
        Entity closest = null;
        double minDistance = Double.MAX_VALUE; // 初期値は最大にしておく

        for (Entity e : entities) {
            // エンティティ同士の距離(の2乗)を計算
            double dist = e.squaredDistanceTo(center);
            if (dist < minDistance) {
                minDistance = dist;
                closest = e;
            }
        }
        return closest;
    }
}