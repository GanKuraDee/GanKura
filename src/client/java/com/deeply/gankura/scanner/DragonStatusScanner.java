package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.regex.Matcher;

public class DragonStatusScanner {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.map) || ModConstants.MODE_COMBAT_3.equals(GameState.mode);
            if (!isTargetMap) {
                GameState.dragonEggState = "Scanning...";
                return;
            }

            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (networkHandler == null) return;

            Collection<PlayerListEntry> entries = networkHandler.getPlayerList();
            if (entries.isEmpty()) return;

            Scoreboard scoreboard = client.world.getScoreboard();

            boolean foundEyePlaced = false;
            boolean foundDragonSpawned = false;
            boolean foundEggRespawning = false;
            int scannedEyes = 0;
            String scannedType = null;

            for (PlayerListEntry entry : entries) {
                if (entry.getProfile() == null || entry.getProfile().name() == null) continue;

                String profileName = entry.getProfile().name();
                Text displayName = entry.getDisplayName();
                Text nameText = displayName != null ? displayName : Text.literal(profileName);

                Team team = scoreboard.getScoreHolderTeam(profileName);
                Text decoratedText = team != null ? team.decorateName(nameText) : nameText;

                String line = decoratedText.getString().replaceAll("§[0-9a-fk-or]", "").trim();
                if (line.isEmpty()) continue;

                String lowerLine = line.toLowerCase();

                if (lowerLine.contains("egg respawning")) {
                    foundEggRespawning = true;
                }
                if (lowerLine.contains("dragon spawned")) {
                    foundDragonSpawned = true;
                }

                Matcher eyeMatcher = ModConstants.EYE_PLACED_TAB_PATTERN.matcher(line);
                if (eyeMatcher.find()) {
                    foundEyePlaced = true;
                    try {
                        scannedEyes = Integer.parseInt(eyeMatcher.group(1));
                    } catch (Exception ignored) {}
                }

                Matcher typeMatcher = ModConstants.DRAGON_TYPE_TAB_PATTERN.matcher(line);
                if (typeMatcher.find()) {
                    scannedType = typeMatcher.group(1);
                }
            }

            // =======================================================
            // ★究極にシンプルになったステートマシン
            // =======================================================
            if ("Scanning...".equals(GameState.dragonEggState)) {
                // 1. The Endのロビーにアクセスした時の初回スキャン（完全同期）
                if (foundEggRespawning) {
                    GameState.dragonEggState = "Respawning";
                    GameState.dragonEyes = 0;
                    GameState.playerDragonEyes = 0; // ★追加
                    GameState.dragonType = null;
                    GameState.dragonSpawnTargetTime = 0;
                } else if (foundDragonSpawned) {
                    if (scannedType != null) {
                        GameState.dragonEggState = "Hatched";
                        GameState.dragonType = scannedType;
                        GameState.dragonEyes = 8;
                        GameState.dragonSpawnTargetTime = 0;

                        // ★追加: ロビーに入った時点で既にドラゴンが居た場合、設定がONならタイトルを出す！
                        if (ModConfig.enableDragonAlerts) {
                            // ★修正: ラムダ式内で使うために、実質的finalな変数に一度コピーする
                            final String finalType = scannedType;
                            client.execute(() -> NotificationUtils.showDragonSpawnAlert(client, finalType));
                        }
                    } else {
                        GameState.dragonEggState = "Hatching";
                        GameState.dragonEyes = 8;
                        GameState.dragonType = null;
                    }
                } else if (foundEyePlaced) {
                    GameState.dragonEggState = "Ready";
                    GameState.dragonEyes = scannedEyes;
                    GameState.dragonType = null;
                    GameState.dragonSpawnTargetTime = 0;
                }
            } else {
                // 2. それ以降は、Eyeが設置・Removeされた時の数だけを同期する
                if ("Ready".equals(GameState.dragonEggState)) {
                    // ★追加: チャットでEyeが設置されてから3秒間(3000ms)は、
                    // サーバーのタブリスト更新が遅れている可能性があるため上書きをブロックする！
                    if (System.currentTimeMillis() - GameState.lastDragonChatTime > 3000) {
                        GameState.dragonEyes = scannedEyes;
                        // ★追加: 誰もEyeを置いていない状態(0/8)になったら、自分の設置数も0にリセットする
                        if (scannedEyes == 0) {
                            GameState.playerDragonEyes = 0;
                        }
                    }
                }
            }
        });
    }
}