package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class ArrowPoisonScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // SKYBLOCK内でのみスキャン
            if (!"SKYBLOCK".equals(GameState.Server.gametype)) {
                GameState.Player.activePoison = "NONE";
                GameState.Player.activePoisonCount = 0;
                return;
            }

            String firstFound = "NONE";
            int toxicTotal = 0;
            int twilightTotal = 0;

            // マイクラのインベントリスロット番号（ホットバー最優先、その後左上から）
            int[] slotOrder = {
                    0, 1, 2, 3, 4, 5, 6, 7, 8,          // ホットバー (最優先)
                    9, 10, 11, 12, 13, 14, 15, 16, 17,  // 1段目
                    18, 19, 20, 21, 22, 23, 24, 25, 26, // 2段目
                    27, 28, 29, 30, 31, 32, 33, 34, 35  // 3段目
            };

            // ★変更: breakせずに全てのスロットをカウントしつつ、最初に見つけた種類も記憶する
            for (int i : slotOrder) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String name = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "");

                    if (name.contains("Toxic Arrow Poison")) {
                        if ("NONE".equals(firstFound)) firstFound = "TOXIC";
                        toxicTotal += stack.getCount(); // 個数を加算
                    } else if (name.contains("Twilight Arrow Poison")) {
                        if ("NONE".equals(firstFound)) firstFound = "TWILIGHT";
                        twilightTotal += stack.getCount(); // 個数を加算
                    }
                }
            }

            // 結果をGameStateに反映
            GameState.Player.activePoison = firstFound;
            if ("TOXIC".equals(firstFound)) {
                GameState.Player.activePoisonCount = toxicTotal;
            } else if ("TWILIGHT".equals(firstFound)) {
                GameState.Player.activePoisonCount = twilightTotal;
            } else {
                GameState.Player.activePoisonCount = 0;
            }
        });
    }
}