package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.util.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;

// 現在地(エリア・サーバーID・ゲームタイプ)の判定元。
// locrawのように参加時1回だけ問い合わせるのではなく、タブリストの "Area: " / "Server: " 行と
// サイドバーのタイトルを毎tick読む。取りこぼしても次のtickで復帰でき、
// エリアの入り直しやサーバー移動も即座に検知できる
public class LocationScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LocationScanner::scanSidebar);
    }

    // サイドバーのタイトルからSkyBlock内かどうかを判定する
    private static void scanSidebar(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        String title = ScoreboardUtils.stripColor(ScoreboardUtils.getSidebarTitle(client)).trim();
        // ワールド遷移直後はサイドバーがまだ無い。ここでUnknownに落とすとHUDが一瞬消えてしまうため、
        // 読めなかった間は前回の値を保持する
        if (title.isEmpty()) return;

        GameState.Server.gametype =
                ModConstants.containsIgnoreCase(title, ModConstants.SIDEBAR_SKYBLOCK_TITLE)
                        ? ModConstants.GAME_TYPE_SKYBLOCK
                        : "Unknown";
    }

    // タブリストからエリア名とサーバーIDを読む (TabListScannerから内容に変化があったときだけ呼ばれる)
    public static void processTabList(List<String> unformattedLines) {
        String area = null;
        String serverId = null;

        for (String line : unformattedLines) {
            String trimmed = line.trim();
            if (area == null && ModConstants.startsWithIgnoreCase(trimmed, ModConstants.TAB_AREA_PREFIX)) {
                area = trimmed.substring(ModConstants.TAB_AREA_PREFIX.length()).trim();
            } else if (serverId == null && ModConstants.startsWithIgnoreCase(trimmed, ModConstants.TAB_SERVER_PREFIX)) {
                serverId = trimmed.substring(ModConstants.TAB_SERVER_PREFIX.length()).trim();
            }
            if (area != null && serverId != null) break;
        }

        // 行が揃っていないタイミングでは更新しない。空で上書きすると、
        // 遷移中の数tickだけ全機能が「エリア外」と判定されて表示が明滅する
        if (area != null && !area.isEmpty()) GameState.Server.map = area;
        if (serverId != null && !serverId.isEmpty()) applyServerId(serverId);
    }

    // サーバーが変わったら、前のサーバーで積み上げた検知結果は無効なので破棄する
    private static void applyServerId(String newId) {
        if (newId.equals(GameState.Server.id)) return;

        GameState.Server.id = newId;
        GameState.Golem.reset();
        GameState.BarbarianDukeX.reset();
        GameState.Bladesoul.reset();
        GameState.MageOutlaw.reset();
        GameState.Ashfang.reset();
        GameState.MagmaBoss.reset();
        // 不在計測とラッチはGameStateの外にあるため個別に消す
        EntityHighlightManager.resetCrimsonBossTracking();
    }
}
