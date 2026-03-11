package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerRestartHandler {

    // スコアボードから時間を抽出する正規表現 (例: "Server closing: 02:30")
    private static final Pattern CLOSING_PATTERN = Pattern.compile("Server closing: (\\d{1,2}:\\d{2})");
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            // ========================================================
            // The EndでのDay30到達 ＆ Stage4 アナウンス処理
            // ========================================================
            boolean isTargetMap = "The End".equals(GameState.map) || "Combat 3".equals(GameState.mode);
            if (isTargetMap) {
                long currentDay = client.world.getTimeOfDay() / 24000L;

                // ★修正: 定数名を STAGE_AWAKENING に変更
                // ★修正: 設定がON、且つ条件を満たしている場合のみ実行
                if (ModConfig.enableDay30Alert && currentDay >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.golemStage) && !GameState.hasAnnouncedDay30) {
                    GameState.hasAnnouncedDay30 = true;

                    net.minecraft.text.MutableText warningMsg = com.deeply.gankura.util.NotificationUtils.getGanKuraPrefix();
                    warningMsg.append(net.minecraft.text.Text.literal("§cWarning: This server might restart before the golem spawns (Day " + currentDay +  ")."));

                    client.player.sendMessage(warningMsg, false);
                }
            }
            // ========================================================

            // チャットでリブート警告を受信した状態でのみスコアボードをスキャンする
            if (!GameState.isServerClosing) return;

            // 毎tickではなく、0.5秒(10tick)に1回の頻度で確認して負荷を下げる
            if (tickCounter++ < 10) return;
            tickCounter = 0;

            Scoreboard scoreboard = client.world.getScoreboard();
            boolean found = false;

            // HypixelのスコアボードはTeamのPrefix/Suffixを使って表示されているため、全Teamのテキストを結合して探す
            for (Team team : scoreboard.getTeams()) {
                String prefix = team.getPrefix().getString();
                String suffix = team.getSuffix().getString();
                // 色コード等の装飾を抜いた文字列にする
                String unformatted = (prefix + suffix).replaceAll("§[0-9a-fk-or]", "");

                Matcher m = CLOSING_PATTERN.matcher(unformatted);
                if (m.find()) {
                    GameState.serverClosingTime = m.group(1); // "02:30" などを取得
                    found = true;
                    break; // 見つかったらループ終了
                }
            }

            // もしスコアボードからカウントダウンが消えたら表示を消す
            if (!found) {
                GameState.serverClosingTime = null;
            }
        });
    }

    public static void handleChat(String unformattedMsg, MinecraftClient client) {
        // ★追加: 設定がOFFなら何もしない
        if (!ModConfig.enableRebootAlert) return;

        if (unformattedMsg.startsWith("[Important] This server will restart soon:")) {
            GameState.isServerClosing = true;
            if (client.player != null) {
                // アイアンゴーレムのデス音を1回鳴らす (音量最大)
                client.player.playSound(SoundEvents.ENTITY_IRON_GOLEM_DEATH, 1.0f, 1.0f);
            }
        }
    }
}