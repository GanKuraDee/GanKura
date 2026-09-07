package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.VisitorHandler;
import com.deeply.gankura.render.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * タブリストの来客まわりの行を、そのまま並べて出す。
 *
 * いま何人待たせていて、次が来るまでどれくらいかが、タブを開かずに分かる。
 * 色と中身は Hypixel のものをそのまま使い、
 * 何の数か分かるよう見出しだけ "Waiting Visitors" に差し替える
 */
public class VisitorStatusHud extends HudElement {

    private static final String HUD_TITLE = "§b§lVisitors Status";

    // タブリストでは見出しが白、値だけに色が付く。プレビューもそれに合わせる
    private static final List<String> PREVIEW = List.of("Next Visitor: §b8m", "§fWaiting Visitors: §e2");

    // 行の見出しだけ差し替える。何の数なのかが一目で分かるように
    private static final String VISITORS_LABEL = "Visitors:";
    private static final String WAITING_LABEL = "Waiting Visitors:";
    // 人数は "(2)" と括弧付きで書かれている。並べたときに他の行と揃わないので外す
    private static final String[] BRACKETS = {"(", ")"};
    // タブでは見出しが太字の水色だが、他の行と揃えたいので白の細字で書き直す。
    // 色を指す字は太字も一緒に解くので、これだけで元の飾りは落ちる
    private static final String PLAIN = "§f";

    // 待たせている人数は、埋まると次が来なくなるので、
    // あと何人で打ち止めかが色だけで分かるようにする。5人で満杯
    private static final String[] WAITING_COLORS = {"§7", "§e", "§4"};
    private static final int WAITING_FULL = 5;

    // ウィジェットを切っていると、タブリストにこの行そのものが出ない。
    // 何も出さずに黙っていると HUD の故障に見えるので、出し方を案内する
    private static final List<String> MISSING = List.of(
            "§cMissing Visitors Widget!", "§7(/widget -> Enable Visitors Widget)");

    private static final int LINE_HEIGHT = 12;

    public VisitorStatusHud() {
        super("visitor_status", 10, 140, 1.0f, 120, LINE_HEIGHT * 3,
                () -> ModConfig.INSTANCE.farming.garden.showVisitorStatusHud,
                GameState.Server::isGarden);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = isPreview ? PREVIEW : VisitorHandler.statusLines();
        // 読めていないのは、庭にいる以上ウィジェットが切られているとき
        if (lines.isEmpty()) lines = MISSING;

        // 行数は来客の有無で変わる。掴む範囲も一緒に変えないと、エディタで枠が余る
        this.height = (lines.size() + 1) * LINE_HEIGHT;

        text(graphics, font, HUD_TITLE, 0, 0, 0xFFFFFFFF, true);

        int y = LINE_HEIGHT;
        for (String line : lines) {
            text(graphics, font, isPreview ? line : display(line), 0, y, 0xFFFFFFFF, true);
            y += LINE_HEIGHT;
        }
    }

    /** 出す形に整えた行。人数の行だけ、見出しを書き直して括弧を外す */
    private static String display(String line) {
        int label = line.indexOf(VISITORS_LABEL);
        if (label < 0) return line;

        String value = ChatFormatting.stripFormatting(line.substring(label + VISITORS_LABEL.length()));
        if (value == null) return line;

        for (String bracket : BRACKETS) value = value.replace(bracket, "");
        value = value.trim();
        try {
            return PLAIN + WAITING_LABEL + " " + waitingColor(Integer.parseInt(value)) + value;
        } catch (NumberFormatException e) {
            // 数でない書き方に変わったら、読めたものをそのまま出す
            return line;
        }
    }

    private static String waitingColor(int waiting) {
        if (waiting <= 0) return WAITING_COLORS[0];
        if (waiting < WAITING_FULL) return WAITING_COLORS[1];
        return WAITING_COLORS[2];
    }
}
