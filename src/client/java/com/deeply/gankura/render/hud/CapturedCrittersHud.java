package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.MobVisual;
import com.deeply.gankura.data.MobVisual.SafariCavern;
import com.deeply.gankura.data.MobVisual.SafariForest;
import com.deeply.gankura.data.MobVisual.SafariHaunted;
import com.deeply.gankura.data.MobVisual.SafariIcy;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

// Critter Safari の全 Critter を、バイオームごとの列に並べてキャプチャ済みかどうかを出す。
// 1体でも捕まえればその種はチェック済みになる
public class CapturedCrittersHud extends HudElement {

    private static final int LINE_HEIGHT = 12;
    // タイトルとバイオーム名の2行分を空けてから一覧を並べる
    private static final int FIRST_LINE_Y = LINE_HEIGHT * 2;
    // 列と列の間隔
    private static final int COLUMN_GAP = 10;
    private static final String MARK_CAPTURED = "§a✔";
    private static final String MARK_MISSING = "§c✖";

    // 列の並び。見出しの色はそのバイオームのイメージに合わせる
    private record Column(String title, String titleColor, List<? extends MobVisual> critters) {}

    private static final List<Column> COLUMNS = List.of(
            new Column("Cavern", "§6", List.of(SafariCavern.values())),
            new Column("Forest", "§a", List.of(SafariForest.values())),
            new Column("Haunted", "§5", List.of(SafariHaunted.values())),
            new Column("Icy", "§b", List.of(SafariIcy.values())));

    public CapturedCrittersHud() {
        super("captured_critters", 215, 10, 1.0f, 380, 150,
                () -> ModConfig.INSTANCE.foraging.showCapturedCrittersHud,
                () -> GameState.Server.isSafari());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        int total = 0;
        int done = 0;
        int x = 0;

        for (Column column : COLUMNS) {
            List<? extends MobVisual> critters = column.critters();
            String heading = column.titleColor() + "§l" + column.title();
            text(graphics, font, heading, x, LINE_HEIGHT, 0xFFFFFFFF, true);

            int columnWidth = font.width(heading);
            int y = FIRST_LINE_Y;

            for (int i = 0; i < critters.size(); i++) {
                boolean captured = isCaptured(critters.get(i), i, critters.size(), isPreview);
                total++;
                if (captured) done++;

                String line = (captured ? MARK_CAPTURED : MARK_MISSING)
                        + " " + (captured ? "§f" : "§7") + critters.get(i).plainLabel();
                text(graphics, font, line, x, y, 0xFFFFFFFF, true);
                columnWidth = Math.max(columnWidth, font.width(line));
                y += LINE_HEIGHT;
            }

            x += columnWidth + COLUMN_GAP;
        }

        text(graphics, font, "§6§lCaptured Critters §r§7- §e" + done + "§7/§e" + total, 0, 0, 0xFFFFFFFF, true);
    }

    // プレビューでは全て未キャプチャだと味気ないので、各列の半分だけ捕まえた状態を見せる
    private static boolean isCaptured(MobVisual critter, int index, int size, boolean isPreview) {
        if (isPreview) return index < size / 2;
        return GameState.CritterSafari.isCaptured(critter.plainLabel());
    }
}
