package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityHealthScanner {
    // ★修正: カンマ(,)を含む数値（例: 3,300/6,000）にもマッチするように [\\d\\.,]+ に変更
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.,]+[kM]?/[\\d\\.,]+[kM]?)");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;

            // 1. ★究極の最適化: 「対象のマップにいるか」かつ「ボスが出現中か」を判定
            // (GolemはThe End または Combat 3 の時のみ出現)
            boolean isTheEnd = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
            boolean scanGolem = isTheEnd && ModConstants.STAGE_SUMMONED.equals(GameState.Golem.stage);

            // (BroodmotherはSpider's Denの時のみ出現)
            boolean isSpidersDen = "Spider's Den".equals(GameState.Server.map);
            boolean scanBroodmother = isSpidersDen && "Alive!".equals(GameState.Broodmother.stage);

            // スキャン不要なボスのHPデータはクリアしておく
            if (!scanGolem) GameState.Golem.health = null;
            if (!scanBroodmother) GameState.Broodmother.health = null;

            // どのボスも「自分のいるマップに出現していない」なら、重い処理を完全にスキップ！（超重要）
            if (!scanGolem && !scanBroodmother) return;

            String foundGolemHealth = null;
            String foundBroodmotherHealth = null;

            // 2. スキャンが必要な場合のみ、エンティティを探す
            for (Entity entity : client.world.getEntities()) {
                Text customName = entity.getCustomName();
                if (customName != null) {
                    String nameStr = customName.getString();

                    // --- Golem の HPスキャン ---
                    if (scanGolem && foundGolemHealth == null && nameStr.contains("End Stone Protector")) {
                        Matcher matcher = HEALTH_PATTERN.matcher(nameStr);
                        if (matcher.find()) {
                            foundGolemHealth = matcher.group(1);
                        }
                    }

                    // --- Broodmother の HPスキャン ---
                    if (scanBroodmother && foundBroodmotherHealth == null &&
                            (nameStr.contains("Brood Mother") || nameStr.contains("Broodmother"))) {
                        Matcher matcher = HEALTH_PATTERN.matcher(nameStr);
                        if (matcher.find()) {
                            foundBroodmotherHealth = matcher.group(1);
                        }
                    }

                    // 探しているボスのHPが見つかったら即終了して負荷を下げる
                    if ((!scanGolem || foundGolemHealth != null) && (!scanBroodmother || foundBroodmotherHealth != null)) {
                        break;
                    }
                }
            }

            // 3. 見つかった結果をGameStateに保存
            if (scanGolem) GameState.Golem.health = foundGolemHealth;
            if (scanBroodmother) GameState.Broodmother.health = foundBroodmotherHealth;
        });
    }
}