package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.PestSpawnHandler;
import com.deeply.gankura.render.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * タブリストの害虫まわりの行を、そのまま並べて出す。
 *
 * 次に湧くまでの間隔と、いま何匹どこに湧いているかがタブを開かずに分かる。
 * 色と中身は Hypixel のものをそのまま使い、
 * 何の待ち時間か分かるよう "Cooldown" の見出しだけ差し替える。
 * Plots の行は湧いていないときには出ないので、そのときは並びが1行減る
 */
public class PestStatusHud extends HudElement {

    private static final String HUD_TITLE = "§2§lPests Status";

    // タブリストでは見出しが白、値だけに色が付く。プレビューもそれに合わせる
    private static final List<String> PREVIEW = List.of("Next Pests: §a§lREADY", "Alive: §e4", "Plots: §b9, 21");

    // 行の見出しだけ差し替える。何の待ち時間かが一目で分かるように
    private static final String COOLDOWN_LABEL = "Cooldown:";
    private static final String NEXT_PESTS_LABEL = "Next Pests:";

    // 湧いている数は、放っておくと Farming Fortune が削られていくので、
    // 何匹でどれくらい不味いのかが色だけで分かるようにする。
    // 4匹から削られはじめ、8匹で打ち止めになる
    private static final String ALIVE_LABEL = "Alive:";
    private static final String PLAIN = "§f";
    private static final String[] ALIVE_COLORS = {"§7", "§e", "§c", "§4"};
    private static final int ALIVE_MILD = 4;
    private static final int ALIVE_BAD = 7;

    // ウィジェットを切っていると、タブリストにこの行そのものが出ない。
    // 何も出さずに黙っていると HUD の故障に見えるので、出し方を案内する
    private static final List<String> MISSING = List.of(
            "§cMissing Pests Widget!", "§7(/widget -> Enable Pests Widget)");

    private static final int LINE_HEIGHT = 12;

    public PestStatusHud() {
        super("pest_status", 10, 188, 1.0f, 120, LINE_HEIGHT * 4,
                () -> ModConfig.INSTANCE.farming.garden.showPestStatusHud,
                GameState.Server::isGarden);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = isPreview ? PREVIEW : lines();
        // 読めていないのは、庭にいる以上ウィジェットが切られているとき
        if (lines.isEmpty()) lines = MISSING;

        // 行数は湧き具合で変わる。掴む範囲も一緒に変えないと、エディタで枠が余る
        this.height = (lines.size() + 1) * LINE_HEIGHT;

        text(graphics, font, HUD_TITLE, 0, 0, 0xFFFFFFFF, true);

        int y = LINE_HEIGHT;
        for (String line : lines) {
            text(graphics, font, isPreview ? line : alive(line), 0, y, 0xFFFFFFFF, true);
            y += LINE_HEIGHT;
        }
    }

    /** 湧いている数の行だけ、数の多さに応じて色を塗り直す。他の行はそのまま */
    private static String alive(String line) {
        int label = line.indexOf(ALIVE_LABEL);
        if (label < 0) return line;

        String value = ChatFormatting.stripFormatting(line.substring(label + ALIVE_LABEL.length()));
        if (value == null) return line;

        value = value.trim();
        try {
            return PLAIN + ALIVE_LABEL + " " + aliveColor(Integer.parseInt(value)) + value;
        } catch (NumberFormatException e) {
            // 数でない書き方に変わったら、読めたものをそのまま出す
            return line;
        }
    }

    private static String aliveColor(int alive) {
        if (alive <= 0) return ALIVE_COLORS[0];
        if (alive <= ALIVE_MILD) return ALIVE_COLORS[1];
        if (alive <= ALIVE_BAD) return ALIVE_COLORS[2];
        return ALIVE_COLORS[3];
    }

    /** 出す行。読めているものだけを、タブリストと同じ並びで返す */
    private static List<String> lines() {
        List<String> lines = new ArrayList<>();

        String cooldown = PestSpawnHandler.cooldownLine();
        if (cooldown != null) lines.add(cooldown.replace(COOLDOWN_LABEL, NEXT_PESTS_LABEL));

        lines.addAll(PestSpawnHandler.statusLines());
        return lines;
    }
}
