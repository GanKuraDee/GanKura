package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

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
    private static final String SOON_TITLE = "§2Pest §eSOON";
    private static final String SOON_SUBTITLE_PREFIX = "§eSpawns in §b";

    // 畑に向いたまま耕していると題は目に入らないので、音でも知らせる
    private static final float SOON_SOUND_VOLUME = 1.0f;
    private static final float SOON_SOUND_PITCH = 1.0f;
    private static final double MILLIS = 1000.0;

    // 待ち時間の書き方。"12s" のほか "1m 30s" のように単位ごとに分かれて書かれる
    private static final Pattern COOLDOWN_PART = Pattern.compile("(\\d+)\\s*([hms])");
    private static final int MINUTE = 60;
    private static final int HOUR = 60 * MINUTE;

    // 耕している最中に出るので、じわりと出入りされるより、ぱっと出てぱっと消える方がよい
    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 70;

    // 直前に見たときに上限だったか。上限に達した瞬間にだけ知らせる
    private static boolean maxPests = false;
    // 次の湧きが近いことを既に知らせたか。何度も出し直さないための目印
    private static boolean soonShown = false;
    // 次に湧く時刻の見込み。届いた残り秒を今の時刻に足して置く。0 なら数えるものがない
    private static long soonDeadlineMillis = 0;
    // タブリストに出ている害虫の "Cooldown" の行。HUD に出すために控えておく
    private static String cooldownLine = null;
    // タブリストに出ている "Alive" と "Plots" の行。HUD に並べるために控えておく
    private static List<String> statusLines = List.of();

    private PestSpawnHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PestSpawnHandler::tick);
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
            resetSoon();
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

        String remaining = null;
        for (int i = 0; i < unformattedLines.size(); i++) {
            String line = unformattedLines.get(i).trim();
            if (!line.startsWith(COOLDOWN_PREFIX)) continue;

            cooldownLine = formattedLines.get(i).trim();
            remaining = line.substring(COOLDOWN_PREFIX.length()).trim();
            full = line.equals(MAX_PESTS_LINE);
            break;
        }

        if (full && !maxPests && ModConfig.INSTANCE.farming.garden.showMaxPestsTitle) {
            NotificationUtils.showTitle(client,
                    Component.literal(MAX_TITLE), Component.literal(MAX_SUBTITLE),
                    TITLE_FADE, TITLE_STAY, TITLE_FADE);
        }
        maxPests = full;

        trackSoon(remaining);
    }

    /**
     * 次に湧く時刻を控える。
     *
     * タブリストは毎秒届くわけではなく、実測では3秒ほど間が空く。
     * 届いた値だけで知らせると、設定した秒をまたいだことに気付くのが
     * その間隔ぶん遅れるので、時刻に直して置いておき、あいだは自前で数える
     */
    private static void trackSoon(String remaining) {
        // "READY" と "MAX PESTS" には数が入っていない。どちらも待ち時間ではない
        int seconds = remaining == null ? -1 : seconds(remaining);
        if (seconds < 0) {
            resetSoon();
            return;
        }

        soonDeadlineMillis = System.currentTimeMillis() + (long) (seconds * MILLIS);

        // まだ設定した秒より先なら、次に近づいたときにまた知らせる
        if (seconds > ModConfig.INSTANCE.farming.garden.pestSoonSeconds) soonShown = false;
    }

    private static void resetSoon() {
        soonShown = false;
        soonDeadlineMillis = 0;
    }

    /**
     * 次の湧きが近いことを画面の真ん中に出す。
     *
     * 湧くのを待って畑を空けておくか、先に吸って回るかを決める合図になる。
     * 控えた時刻を毎 Tick 見るので、設定した秒をまたいだその場で出せる。
     * 一度出したら READY になるか、待ち時間が伸び直すまで黙る
     */
    private static void tick(Minecraft client) {
        ModConfig.GardenCategory config = ModConfig.INSTANCE.farming.garden;
        if (!config.showPestSoonTitle || !GameState.Server.isGarden()) {
            resetSoon();
            return;
        }
        if (soonShown || soonDeadlineMillis == 0) return;

        // 残りは切り上げる。タブリストに出ている数と同じ数え方にしておく
        int seconds = (int) Math.ceil((soonDeadlineMillis - System.currentTimeMillis()) / MILLIS);
        if (seconds > config.pestSoonSeconds) return;

        soonShown = true;
        NotificationUtils.showTitle(client,
                Component.literal(SOON_TITLE),
                Component.literal(SOON_SUBTITLE_PREFIX + Math.max(seconds, 0) + "s"),
                TITLE_FADE, TITLE_STAY, TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SOON_SOUND_VOLUME, SOON_SOUND_PITCH);
    }

    /** 待ち時間を秒に直す。数の書かれていない行は -1 */
    private static int seconds(String text) {
        Matcher part = COOLDOWN_PART.matcher(text);

        int total = -1;
        while (part.find()) {
            int value = count(part.group(1));
            total = Math.max(total, 0) + switch (part.group(2)) {
                case "h" -> value * HOUR;
                case "m" -> value * MINUTE;
                default -> value;
            };
        }
        return total;
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
