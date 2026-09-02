package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 決まった文面のチャットを、出さずに捨てる。
 *
 * どれも Hypixel が繰り返し流すもので、読み飛ばす前提の行しか対象にしない
 */
public final class ChatFilterHandler {

    // ワールドに入るたびに流れる2行
    private static final String PROFILE_NAME_PREFIX = "You are playing on profile:";
    private static final String PROFILE_ID_PREFIX = "Profile ID:";

    /**
     * Stash の催促。1分ごとに、アイテムなら2行、素材なら3行まとめて流れてくる。
     *   "You have 553 materials stashed away!"
     *   "(This totals 2 types of materials stashed!)"
     *   ">>> CLICK HERE to pick them up! <<<"
     * 数が1つのときは単数形になり、行頭には余白が入る。
     * 最後の行は他の場面の "Click here" と紛れないよう、
     * 大文字の見出しと「拾う」という言い回しの両方が揃ったときだけ弾く
     */
    private static final List<Pattern> STASH_PATTERNS = List.of(
            Pattern.compile("^You have [\\d,]+ (?:item|material)(?:\\(s\\)|s)? stashed away!"),
            Pattern.compile("^\\(This totals [\\d,]+ type(?:\\(s\\)|s)? of material(?:\\(s\\)|s)? stashed!?\\)"),
            Pattern.compile(">>> CLICK HERE to pick .*? up!"));

    private ChatFilterHandler() {
    }

    /**
     * @return チャットに出さないなら true
     */
    public static boolean shouldHide(String unformattedMsg) {
        if (!GameState.Server.isSkyblock()) return false;

        ModConfig.ChatFilterCategory config = ModConfig.INSTANCE.chatFilter;
        String message = unformattedMsg.trim();

        // 見出しの前後に挟まる空行。何も書かれていないので、消しても読み落としはない
        if (config.hideBlankMessages && message.isEmpty()) return true;

        if (config.hideProfileMessage && message.startsWith(PROFILE_NAME_PREFIX)) return true;
        if (config.hideProfileIdMessage && message.startsWith(PROFILE_ID_PREFIX)) return true;
        if (!config.hideStashMessage) return false;

        for (Pattern pattern : STASH_PATTERNS) {
            if (pattern.matcher(message).find()) return true;
        }
        return false;
    }
}
