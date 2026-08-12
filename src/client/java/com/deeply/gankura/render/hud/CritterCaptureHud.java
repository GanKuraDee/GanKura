package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

// Icy Biome の Critter 8種をキャプチャ済みかどうかを一覧で表示する。
// 8種すべて揃うと Wumpa がスポーンするため、あと何が残っているかが分かればよい
public class CritterCaptureHud extends HudElement {

    private static final int LINE_HEIGHT = 12;
    // タイトルと合計行の2行分を空けてから一覧を並べる
    private static final int FIRST_LINE_Y = LINE_HEIGHT * 2;
    private static final String MARK_CAPTURED = "§a✔";
    private static final String MARK_MISSING = "§c✖";

    public CritterCaptureHud() {
        super("critter_capture", 460, 10, 1.0f, 130, 120,
                () -> ModConfig.INSTANCE.foraging.showCritterCaptureHud,
                () -> GameState.Server.isSafari());
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        List<String> critters = ModConstants.ICY_BIOME_CRITTERS;

        // プレビューでは全て未キャプチャだと味気ないので、半分だけ捕まえた状態を見せる
        int previewCaptured = critters.size() / 2;
        int captured = isPreview ? previewCaptured : GameState.CritterSafari.capturedCount();

        drawTextWithShadow(context, tr, "§b§lWumpa Status", 0, 0, 0xFFFFFFFF);
        String status = isPreview ? GameState.CritterSafari.STATUS_SPAWNED : GameState.CritterSafari.wumpaStatus;
        String countLine = countLine(captured, critters.size(), status);
        drawTextWithShadow(context, tr, countLine, 0, LINE_HEIGHT, 0xFFFFFFFF);

        int y = FIRST_LINE_Y;
        for (int i = 0; i < critters.size(); i++) {
            String critter = critters.get(i);
            boolean done = isPreview ? i < previewCaptured : GameState.CritterSafari.isCaptured(critter);
            String mark = done ? MARK_CAPTURED : MARK_MISSING;
            drawTextWithShadow(context, tr, mark + " " + (done ? "§f" : "§7") + critter, 0, y, 0xFFFFFFFF);
            y += LINE_HEIGHT;
        }
    }

    private static String countLine(int captured, int total, String status) {
        // 湧いている間は行全体を赤くして目に留まりやすくする
        if (GameState.CritterSafari.STATUS_SPAWNED.equals(status)) {
            return "§cCaptured Critters: " + captured + "/" + total + " (" + status + ")";
        }

        String line = "§fCaptured Critters§7: §e" + captured + "§7/§e" + total;
        if (GameState.CritterSafari.STATUS_CAPTURED.equals(status)) line += " §a(" + status + ")";
        return line;
    }
}
