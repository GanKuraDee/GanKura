package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

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
            boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);
            if (isTargetMap) {
                long currentDay = client.world.getTimeOfDay() / 24000L;

                // 設定がON、且つ条件を満たしている場合のみ実行
                if (ModConfig.INSTANCE.golem.enableDay30Alert && currentDay >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage) && !GameState.Golem.hasAnnouncedDay30) {
                    GameState.Golem.hasAnnouncedDay30 = true;

                    client.execute(() -> {
                        // 汎用化したUtilsを使ってプレフィックス付きの警告メッセージを送信
                        Text warningMsg = Text.literal("§cWarning: This server might restart before the golem spawns. (Day " + currentDay + ")");
                        NotificationUtils.sendSystemChat(client, warningMsg);
                    });
                }
            }
            // ========================================================

            // ★復元: チャットでリブート警告を受信した状態でのみスコアボードをスキャンする (負荷軽減)
            if (!GameState.Server.isClosing) return;

            // ★復元: 毎tickではなく、0.5秒(10tick)に1回の頻度で確認して負荷を下げる
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
                    GameState.Server.closingTime = m.group(1); // "02:30" などを取得
                    found = true;
                    break; // 見つかったらループ終了
                }
            }

            // もしスコアボードからカウントダウンが消えたら表示を消す
            if (!found) {
                GameState.Server.closingTime = null;
            }
        });
    }

    public static void handleChat(String unformattedMsg, MinecraftClient client) {
        // 設定がOFFなら何もしない
        if (!ModConfig.INSTANCE.misc.enableRebootAlert) return;

        if (unformattedMsg.startsWith("[Important] This server will restart soon:")) {
            GameState.Server.isClosing = true;
            if (client.player != null) {
                // ★復元: アイアンゴーレムのデス音を1回鳴らす (音量最大)
                client.player.playSound(SoundEvents.ENTITY_IRON_GOLEM_DEATH, 1.0f, 1.0f);
            }
        }
    }
}