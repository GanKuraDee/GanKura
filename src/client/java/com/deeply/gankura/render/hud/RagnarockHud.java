package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.RagnarockHandler;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Ragnarock の詠唱が終わるまでの残り時間。詠唱している間だけ出す。
 *
 * 詠唱の3秒の間は持ち替えもダメージも許されないので、あとどれだけ耐えればよいかを小数で見せる
 */
public class RagnarockHud extends HudElement {

    private static final String LABEL = "§6Ragnarock: ";
    private static final double PREVIEW_SECONDS = 2.4;

    public RagnarockHud() {
        super("ragnarock", 460, 240, 1.0f, 90, 12,
                () -> ModConfig.Combat.showRagnarockCountdownHud,
                () -> GameState.Server.isSkyblock() && RagnarockHandler.secondsLeft() != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        Double secondsLeft = isPreview ? Double.valueOf(PREVIEW_SECONDS) : RagnarockHandler.secondsLeft();
        if (secondsLeft == null) return;

        // 残り1秒を切ったら、もう少しだと分かるよう色を変える
        String color = secondsLeft <= 1.0 ? "§a" : "§e";
        text(graphics, font, LABEL + color + String.format(Locale.ROOT, "%.1fs", secondsLeft), 0, 0, 0xFFFFFFFF, true);
    }
}
