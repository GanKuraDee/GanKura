package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants; // ★追加
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GolemHealthScanner {
    // 「数値/数値」のパターン
    private static final Pattern HEALTH_PATTERN = Pattern.compile("([\\d\\.]+[kM]?/[\\d\\.]+[kM]?)");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;

            // ★追加: Stage 5 (Summoned) 以外はスキャンせず、データもクリアする
            if (!ModConstants.STAGE_SUMMONED.equals(GameState.golemStage)) {
                GameState.golemHealth = null;
                return;
            }

            // 以下、Stage 5 の間のみ実行される
            String foundHealth = null;

            for (Entity entity : client.world.getEntities()) {
                Text customName = entity.getCustomName();
                if (customName != null) {
                    String nameStr = customName.getString();

                    if (nameStr.contains("End Stone Protector")) {
                        Matcher matcher = HEALTH_PATTERN.matcher(nameStr);
                        if (matcher.find()) {
                            foundHealth = matcher.group(1);
                            break;
                        }
                    }
                }
            }

            GameState.golemHealth = foundHealth;
        });
    }
}