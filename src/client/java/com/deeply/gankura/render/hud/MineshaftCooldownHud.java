package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.MineshaftHandler;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Glacite Mineshaft に入った直後の30秒クールダウンを表示する HUD。
 */
public class MineshaftCooldownHud extends HudElement {

    private static final String LABEL = "§bMineshaft Cooldown: ";
    private static final double PREVIEW_SECONDS = 18.5;

    public MineshaftCooldownHud() {
        super("mineshaft_cooldown", 10, 95, 1.0f, 145, 12,
                () -> ModConfig.Mining.showMineshaftCooldownHud,
                () -> MineshaftHandler.cooldownSecondsLeft() != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        Double secondsLeft = isPreview ? Double.valueOf(PREVIEW_SECONDS) : MineshaftHandler.cooldownSecondsLeft();
        if (secondsLeft == null) return;

        // 残り5秒を切ったら、もうすぐ入れると分かるよう緑にする
        String color = secondsLeft <= 5.0 ? "§a" : "§e";
        text(graphics, font, LABEL + color + String.format(Locale.ROOT, "%.1fs", secondsLeft), 0, 0, 0xFFFFFFFF, true);
    }
}
