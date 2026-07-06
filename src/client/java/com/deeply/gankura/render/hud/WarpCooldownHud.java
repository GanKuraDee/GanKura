package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WarpCooldownHud extends HudElement {
    public WarpCooldownHud() {
        super("warp_cooldown", 10, 79, 1.0f, 100, 15,
                () -> ModConfig.INSTANCE.misc.enableWarpQueue,
                () -> GameState.Warp.cooldownEndAt > System.currentTimeMillis());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        double remaining = isPreview ? 5.0 : Math.max(0, GameState.Warp.cooldownEndAt - System.currentTimeMillis()) / 1000.0;
        String suffix = (!isPreview && GameState.Warp.queuedCommand != null) ? " §e(Queued)" : "";
        graphics.text(font, String.format("§bWarp: %.1fs%s", remaining, suffix), 0, 0, 0xFFFFFFFF, true);
    }
}
