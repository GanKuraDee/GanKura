package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// Doomspiral の儀式でともしたキャンドルの本数と、4本ともした後の状態を表示する
public class DoomspiralHud extends HudElement {

    // 他のHUDと同じ行間
    private static final int LINE_HEIGHT = 12;

    public DoomspiralHud() {
        super("doomspiral", 460, 130, 1.0f, 170, 24,
                () -> ModConfig.INSTANCE.foraging.showDoomspiralHud,
                () -> GameState.Server.isSafari());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        int lit = isPreview ? ModConstants.DOOMSPIRAL_CANDLE_TOTAL : GameState.Doomspiral.litCandles;
        String status = isPreview ? GameState.Doomspiral.STATUS_SPAWNED : GameState.Doomspiral.status;

        text(graphics, font, "§5§lDoomspiral Status", 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, candleLine(lit, status), 0, LINE_HEIGHT, 0xFFFFFFFF, true);
    }

    private static String candleLine(int lit, String status) {
        int total = ModConstants.DOOMSPIRAL_CANDLE_TOTAL;
        // 状態が確定している間は Wumpa と同じく行全体をその色にする
        String lineColor = lineColor(status);
        if (lineColor != null) {
            return lineColor + "Candles: " + lit + "/" + total + " (" + status + ")";
        }

        // 区切りの "/" は Wumpa Status と同じく灰色にする
        String value = "§e" + lit + "§7/§e" + total;
        if (status != null) value += " " + statusColor(status) + "(" + status + ")";
        return "§fCandles§7: " + value;
    }

    // 行全体を塗る状態。Spawning は儀式の途中なので、ステータス部分だけの色付けに留める
    private static String lineColor(String status) {
        return switch (status == null ? "" : status) {
            case GameState.Doomspiral.STATUS_SPAWNED   -> "§c";
            case GameState.Doomspiral.STATUS_CAPTURED  -> "§a";
            case GameState.Doomspiral.STATUS_DESPAWNED -> "§7";
            default -> null;
        };
    }

    private static String statusColor(String status) {
        return switch (status) {
            case GameState.Doomspiral.STATUS_SPAWNING  -> "§e";
            case GameState.Doomspiral.STATUS_SPAWNED   -> "§c";
            case GameState.Doomspiral.STATUS_CAPTURED  -> "§a";
            case GameState.Doomspiral.STATUS_DESPAWNED -> "§7";
            default -> "§f";
        };
    }
}
