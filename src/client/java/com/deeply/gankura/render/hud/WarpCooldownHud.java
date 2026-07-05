package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class WarpCooldownHud extends HudElement {
    public WarpCooldownHud() {
        super("warp_cooldown", 400, 79, 1.0f, 100, 15,
                () -> ModConfig.INSTANCE.misc.enableWarpQueue,
                () -> GameState.Warp.cooldownEndAt > System.currentTimeMillis());
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        double remaining = isPreview ? 5.0 : Math.max(0, GameState.Warp.cooldownEndAt - System.currentTimeMillis()) / 1000.0;
        String suffix = (!isPreview && GameState.Warp.queuedCommand != null) ? " §e(Queued)" : "";
        context.drawTextWithShadow(tr, String.format("§bWarp: %.1fs%s", remaining, suffix), 0, 0, 0xFFFFFFFF);
    }
}
