package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 畑に害虫が湧いたことを画面の真ん中に出す。
 *
 * 知らせは湧いた数で文面が変わり、頭の言葉も GROSS!/EWW!/YUCK! と揺れる。
 * <pre>
 * GROSS! A ✿ Pest has appeared in Plot - 4!
 * YUCK! 4 ✿ Pests have spawned in Plot - 14!
 * </pre>
 * 数の書かれない行は「1匹」として読む。
 * 場所は "Plot - 4" のほか "The Barn" と書かれることもあるので、そのまま出す
 */
public final class PestSpawnHandler {

    // 害虫の絵文字は私用領域の字で、増えたり変わったりし得る。
    // 1文字と決めつけず、空白でない塊として読み飛ばす
    private static final Pattern ONE_PEST = Pattern.compile(
            "^\\w+! An? (?:\\S+ )?Pest has appeared in (?<where>.+)!$");
    private static final Pattern MANY_PESTS = Pattern.compile(
            "^\\w+! (?<amount>\\d+) (?:\\S+ )?Pests? have spawned in (?<where>.+)!$");

    // "Pest" だけ緑、数は赤のまま。数の方が目に入るようにしておく
    private static final String TITLE_PREFIX = "§2Pest §cx";
    // 場所は Hypixel の知らせと同じ水色にして、"in" とは色で分ける
    private static final String SUBTITLE_PREFIX = "§ein §b";
    // 場所は "Plot - 14" と書かれてくるが、番号だけで通じるので飾りは落とす
    private static final String PLOT_PREFIX = "Plot - ";

    // タブリストの Cooldown の行。害虫が上限まで湧くと、ここが埋まって次が湧かなくなる
    private static final String COOLDOWN_PREFIX = "Cooldown:";
    // Pests の下に並ぶ行。Plots は湧いているときにしか出ない
    private static final String[] STATUS_PREFIXES = {"Alive:", "Plots:"};
    private static final String MAX_PESTS_LINE = "Cooldown: MAX PESTS";
    private static final String MAX_TITLE = "§2Pest §cMAX";
    private static final String MAX_SUBTITLE = "§eNo more will spawn";

    // 耕している最中に出るので、じわりと出入りされるより、ぱっと出てぱっと消える方がよい
    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 70;

    // 直前に見たときに上限だったか。上限に達した瞬間にだけ知らせる
    private static boolean maxPests = false;
    // タブリストに出ている害虫の "Cooldown" の行。HUD に出すために控えておく
    private static String cooldownLine = null;
    // タブリストに出ている "Alive" と "Plots" の行。HUD に並べるために控えておく
    private static List<String> statusLines = List.of();

    private PestSpawnHandler() {
    }

    /** タブリストの害虫の "Cooldown" の行。まだ読めていなければ null */
    public static String cooldownLine() {
        return cooldownLine;
    }

    /** タブリストの害虫の様子の行。まだ読めていなければ空 */
    public static List<String> statusLines() {
        return statusLines;
    }

    public static void handleMessage(String unformattedMessage, Minecraft client) {
        if (!ModConfig.INSTANCE.farming.garden.showPestSpawnTitle) return;
        if (!GameState.Server.isGarden()) return;

        String message = unformattedMessage.trim();

        Matcher one = ONE_PEST.matcher(message);
        if (one.matches()) {
            show(client, 1, one.group("where"));
            return;
        }

        Matcher many = MANY_PESTS.matcher(message);
        if (many.matches()) show(client, count(many.group("amount")), many.group("where"));
    }

    /**
     * 害虫が上限まで湧いたことを画面の真ん中に出す。
     *
     * 上限のあいだは新しく湧かず、Farming Fortune も削られたままなので、
     * 吸いに行く合図になる。タブリストは中身が変わったときだけ渡ってくるが、
     * 出入りのたびに出し直さないよう、達した瞬間かどうかはこちらでも見る
     */
    public static void processTabList(List<String> formattedLines, List<String> unformattedLines,
                                      Minecraft client) {
        if (!GameState.Server.isGarden()) {
            maxPests = false;
            cooldownLine = null;
            statusLines = List.of();
            return;
        }

        boolean full = false;
        cooldownLine = null;
        List<String> status = new ArrayList<>();

        // タブリストの行はこちらに届く順が決まっていないので、並べる順はこちらで決める
        for (String prefix : STATUS_PREFIXES) {
            for (int i = 0; i < unformattedLines.size(); i++) {
                String line = unformattedLines.get(i).trim();
                if (!line.startsWith(prefix)) continue;

                // HUD にはタブリストの見た目をそのまま出したいので、色の付いた方を控える
                status.add(formattedLines.get(i).trim());
                break;
            }
        }
        statusLines = List.copyOf(status);

        for (int i = 0; i < unformattedLines.size(); i++) {
            String line = unformattedLines.get(i).trim();
            if (!line.startsWith(COOLDOWN_PREFIX)) continue;

            cooldownLine = formattedLines.get(i).trim();
            full = line.equals(MAX_PESTS_LINE);
            break;
        }

        if (full && !maxPests && ModConfig.INSTANCE.farming.garden.showMaxPestsTitle) {
            NotificationUtils.showTitle(client,
                    Component.literal(MAX_TITLE), Component.literal(MAX_SUBTITLE),
                    TITLE_FADE, TITLE_STAY, TITLE_FADE);
        }
        maxPests = full;
    }

    private static void show(Minecraft client, int amount, String where) {
        NotificationUtils.showTitle(client,
                Component.literal(TITLE_PREFIX + amount),
                Component.literal(SUBTITLE_PREFIX + place(where)),
                TITLE_FADE, TITLE_STAY, TITLE_FADE);
    }

    /** 出す場所の名前。"The Barn" のように番号でない場所は、書かれたまま出す */
    private static String place(String where) {
        String name = where.trim();
        return name.startsWith(PLOT_PREFIX) ? name.substring(PLOT_PREFIX.length()).trim() : name;
    }

    private static int count(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
