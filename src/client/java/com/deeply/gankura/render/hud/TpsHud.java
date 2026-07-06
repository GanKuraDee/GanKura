package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class TpsHud extends HudElement {
    public TpsHud() {
        super("tps", 10, 99, 1.0f, 80, 15, () -> ModConfig.INSTANCE.misc.showTpsHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        double tps = isPreview ? 20.0 : GameState.Server.tps;

        String color = tps >= 19.0 ? "§a" : tps >= 15.0 ? "§e" : "§c";
        graphics.text(font, String.format("TPS: %s%.1f", color, tps), 0, 0, 0xFFFFFFFF, true);
    }
}
