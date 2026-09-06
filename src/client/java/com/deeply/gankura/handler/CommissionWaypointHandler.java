package com.deeply.gankura.handler;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 受けている Gemstone の依頼に合わせて、そのジェムストーンが採れる場所を控える。
 *
 * Glacite Tunnels のジェムストーンは湧く場所が決まっているので、
 * その依頼を受けている間だけ、場所にラベルを出せるようにしておく。
 *
 * 受けている依頼はタブリストの Commissions の欄から読む。
 * 場所の一覧は Skyblocker(LGPL-3.0) が集めたものを使わせてもらっている
 */
public final class CommissionWaypointHandler {

    /** ラベル1つ分。名前と色は種類ごとに決まっている */
    public record Label(String name, int argb, BlockPos pos) {
    }

    /**
     * ジェムストーンと、それが採れる場所。
     *
     * 色はそのジェムストーンの色味に寄せてあるが、暗い色は文字にすると読めないので、
     * 地の色ではなく明るい方に振ってある
     */
    private enum Gemstone {
        AQUAMARINE("Aquamarine", 0xFF5599FF,
                new BlockPos(20, 136, 370), new BlockPos(-14, 132, 386),
                new BlockPos(6, 137, 411), new BlockPos(50, 117, 302)),
        ONYX("Onyx", 0xFFAAAAAA,
                new BlockPos(4, 127, 307), new BlockPos(-3, 139, 434),
                new BlockPos(77, 118, 411), new BlockPos(-68, 130, 404)),
        PERIDOT("Peridot", 0xFF99CC33,
                new BlockPos(66, 144, 284), new BlockPos(94, 154, 284),
                new BlockPos(-62, 147, 303), new BlockPos(-77, 119, 283),
                new BlockPos(87, 122, 394), new BlockPos(-73, 122, 456)),
        CITRINE("Citrine", 0xFFCC9944,
                new BlockPos(-86, 143, 261), new BlockPos(74, 150, 327),
                new BlockPos(63, 137, 343), new BlockPos(38, 119, 386),
                new BlockPos(55, 150, 400), new BlockPos(-45, 127, 415),
                new BlockPos(-60, 144, 424), new BlockPos(-54, 132, 410));

        private final String label;
        private final int argb;
        private final BlockPos[] positions;

        Gemstone(String label, int argb, BlockPos... positions) {
            this.label = label;
            this.argb = argb;
            this.positions = positions;
        }
    }

    // 依頼の行。"Aquamarine Gemstone Collector: 27.4%" の形で書かれている
    private static final Pattern COMMISSION =
            Pattern.compile("^(?<name>[^:]+): (?<progress>DONE|[\\d.,]+%)$");
    // 終わった依頼の印。もう掘る必要がないので、場所は出さない
    private static final String DONE_MARK = "DONE";

    private static volatile List<Label> labels = List.of();

    private CommissionWaypointHandler() {
    }

    /** 今出す場所。依頼を受けていなければ空 */
    public static List<Label> labels() {
        return labels;
    }

    /** タブリストの中身が変わったときに呼ばれる (TabListScanner から) */
    public static void processTabList(List<String> unformattedLines) {
        List<Label> found = new ArrayList<>();

        for (String line : unformattedLines) {
            Matcher commission = COMMISSION.matcher(line.trim());
            if (!commission.matches()) continue;
            if (commission.group("progress").equals(DONE_MARK)) continue;

            add(found, commission.group("name"));
        }

        labels = List.copyOf(found);
    }

    // "Aquamarine Gemstone Collector" のように、依頼の名前にジェムストーンの名前が入っている
    private static void add(List<Label> found, String commissionName) {
        for (Gemstone gemstone : Gemstone.values()) {
            if (!commissionName.contains(gemstone.label)) continue;

            for (BlockPos pos : gemstone.positions) {
                found.add(new Label(gemstone.label, gemstone.argb, pos));
            }
        }
    }
}
