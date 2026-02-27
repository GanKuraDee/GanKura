package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class StageScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("StageScanner");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            scanTabList(client);
        });
    }

    public static void setStageToSummoned(MinecraftClient client) {
        if (GameState.isScanning) GameState.isScanning = false;

        if (client.world != null) {
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

                if (wasScanning && ModConstants.STAGE_SUMMONED.equals(stageName)) {
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

        // --- Stage 4 開始判定 ---
        if (ModConstants.STAGE_AWAKENING.equals(newStage)) {
            GameState.stage4StartTime = System.currentTimeMillis();

            if (ModConfig.enableStageAlerts) {
                NotificationUtils.showAwakeningAlert(client);
                NotificationUtils.playAwakeningSound(client);
            }
        }

        // --- Stage 5 開始判定 (Stage 4 終了) ---
        else if (ModConstants.STAGE_SUMMONED.equals(newStage)) {
            if (ModConstants.STAGE_AWAKENING.equals(oldStage) && GameState.stage4StartTime > 0) {
                long durationMillis = System.currentTimeMillis() - GameState.stage4StartTime;
                long seconds = durationMillis / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;

                if (ModConfig.showStage4Duration) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            client.execute(() -> {
                                if (client.player != null) {
                                    MutableText message = NotificationUtils.getGanKuraPrefix();
                                    message.append(Text.literal(String.format("§aStage 4 Duration: %dm %ds", minutes, remainingSeconds)));
                                    client.player.sendMessage(message, false);
                                }
                            });
                        }
                    }, 100);
                }
            }
            GameState.stage4StartTime = 0;

            if (GameState.stage5TargetTime == 0 && client.world != null) {
                GameState.stage5TargetTime = client.world.getTime() + 400;
            }
            if (ModConfig.enableStageAlerts) {
                NotificationUtils.showSummonedAlert(client);
                NotificationUtils.playSummonedSound(client);
            }
        }

        // --- それ以外 (Stage 0~3) へ遷移した場合 ---
        else {
            // タイマーリセット
            GameState.stage4StartTime = 0;

            // ★追加: ゴーレムが倒されてステージがリセットされたら、Riseフラグも元に戻す！
            // これにより、同ワールドで2回目以降のゴーレムでも正常に(Soon)が表示されます。
            GameState.hasGolemRisen = false;
        }
    }
}