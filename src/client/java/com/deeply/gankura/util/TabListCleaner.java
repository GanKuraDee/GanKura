package com.deeply.gankura.util;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tab の一覧から、空きばかりの部分を取り除く。
 *
 * Hypixel は 20 行ぴったりの列を並べて画面を作っており、
 * 足りない行は空欄で埋めている。名前は "!A-a" から "!D-t" まで、
 * 列と行の順に振られている。
 *
 * これに加えて、実際のプレイヤーの行が末尾に付いてくる。
 * こちらは左端の列に出ている顔ぶれと同じ内容で、そのまま数えると
 * 全体が 80 行を超えるため、バニラの「件数から列数を決める」計算が
 * 1列 19 行に変わり、Hypixel の組んだ列が丸ごとずれてしまう。
 * 重複を外すと 80 行に戻り、元の 4 列 × 20 行に収まる
 */
public final class TabListCleaner {

    // バニラが1列に詰める行数。Hypixel の列もこれに合わせて作られている
    private static final int COLUMN_ROWS = PlayerTabOverlay.MAX_ROWS_PER_COL;

    // 画面を組むための行に付いている名前の頭
    private static final String LAYOUT_PREFIX = "!";

    // 列として残す中身の数。見出しだけの列は、消しても読めるものが減らない
    private static final int MIN_CONTENT = 2;

    /**
     * その行に顔を出すか。
     *
     * Hypixel は情報欄にも架空のプレイヤーを使っており、
     * アカウント名はどの行も差し替えられていて手掛かりにならない。
     * プレイヤーの行だけがスカイブロックレベルから始まる
     * ("[591] GanKuraDee") ので、表示名の形で見分ける
     */
    public static boolean hasHead(PlayerTabOverlay overlay, PlayerInfo entry) {
        if (entry == null) return true;

        return PLAYER_ROW.matcher(overlay.getNameForDisplay(entry).getString()).find();
    }

    private static final Pattern PLAYER_ROW =
            Pattern.compile("^\\s*(?:§.)*\\[\\d+](?:§.)*\\s+\\S");

    private TabListCleaner() {
    }

    public static List<PlayerInfo> clean(List<PlayerInfo> entries, PlayerTabOverlay overlay) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableTabListTweaks || !config.compactTabList
                || !GameState.Server.isSkyblock()) {
            return entries;
        }

        List<PlayerInfo> layout = new ArrayList<>();
        for (PlayerInfo entry : entries) {
            if (entry.getProfile().name().startsWith(LAYOUT_PREFIX)) layout.add(entry);
        }

        // Hypixel の作りと違うときは触らない
        if (layout.isEmpty() || layout.size() % COLUMN_ROWS != 0) return entries;

        return dropEmptyColumns(layout, overlay);
    }

    /**
     * 中身のない列を落とす。
     *
     * 残りが 20 の倍数のままなので、バニラは今まで通り
     * 1列 20 行として並べ直してくれる
     */
    private static List<PlayerInfo> dropEmptyColumns(List<PlayerInfo> layout, PlayerTabOverlay overlay) {
        List<PlayerInfo> kept = new ArrayList<>();

        for (int start = 0; start < layout.size(); start += COLUMN_ROWS) {
            List<PlayerInfo> column = layout.subList(start, start + COLUMN_ROWS);
            if (content(column, overlay) >= MIN_CONTENT) kept.addAll(column);
        }

        // すべて空きだった場合に、一覧ごと消えてしまわないようにする
        return kept.isEmpty() ? layout : kept;
    }

    private static int content(List<PlayerInfo> column, PlayerTabOverlay overlay) {
        int count = 0;
        for (PlayerInfo entry : column) {
            if (!overlay.getNameForDisplay(entry).getString().isBlank()) count++;
        }
        return count;
    }
}
