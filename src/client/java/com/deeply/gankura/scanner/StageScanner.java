package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.regex.Matcher;

public class StageScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("StageScanner");
    private static int scanTickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ★削除: クライアント側での減算処理は廃止（サーバー時間依存に戻すため）

            // 1秒(20Tick)ごとのスキャン
            if (scanTickCounter++ < 20) return;
            scanTickCounter = 0;
            scanTabList(client);
        });
    }

    public static void setStageToSummoned(MinecraftClient client) {
        if (GameState.isScanning) GameState.isScanning = false;

        if (client.world != null) {
            // 現在のサーバー時間 + 400Tick (20秒) をセット
            GameState.stage5TargetTime = client.world.getTime() + 400;
        }

        updateStage(client, ModConstants.STAGE_SUMMONED);
    }

    private static void scanTabList(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) return;

        Collection<PlayerListEntry> entries = networkHandler.getPlayerList();

        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            Matcher matcher = ModConstants.PROTECTOR_PATTERN.matcher(line);

            if (matcher.find()) {
                boolean wasScanning = GameState.isScanning;

                if (GameState.isScanning) {
                    GameState.isScanning = false;
                }

                String rawState = matcher.group(1).trim().split("\\s+")[0];
                String stageName = normalizeStageName(rawState);

                // 初回スキャンで既にSummonedだった場合
                if (wasScanning && ModConstants.STAGE_SUMMONED.equals(stageName)) {
                    // ターゲット時間を「過去(1)」にして即Spawned表示にする
                    GameState.stage5TargetTime = 1;
                }

                if (ModConstants.STAGE_SUMMONED.equals(GameState.golemStage)
                        && ModConstants.STAGE_AWAKENING.equals(stageName)) {
                    break;
                }

                updateStage(client, stageName);
                break;
            }
        }
    }

    private static String normalizeStageName(String raw) {
        return switch (raw) {
            case "Resting" -> ModConstants.STAGE_RESTING;
            case "Dormant" -> ModConstants.STAGE_DORMANT;
            case "Agitated" -> ModConstants.STAGE_AGITATED;
            case "Disturbed" -> ModConstants.STAGE_DISTURBED;
            case "Awakening" -> ModConstants.STAGE_AWAKENING;
            default -> raw;
        };
    }

    private static void updateStage(MinecraftClient client, String newStage) {
        String oldStage = GameState.golemStage;
        if (oldStage.equals(newStage)) return;

        GameState.golemStage = newStage;
        LOGGER.info("Stage updated: {} -> {}", oldStage, newStage);

        if (ModConstants.STAGE_AWAKENING.equals(newStage)) {
            NotificationUtils.showAwakeningAlert(client);
            NotificationUtils.playAwakeningSound(client);
        } else if (ModConstants.STAGE_SUMMONED.equals(newStage)) {
            // タイマー未設定の場合のみセット
            if (GameState.stage5TargetTime == 0 && client.world != null) {
                GameState.stage5TargetTime = client.world.getTime() + 400;
            }
            NotificationUtils.showSummonedAlert(client);
            NotificationUtils.playSummonedSound(client);
        }
    }
}