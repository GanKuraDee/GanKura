package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class WarpCooldownHud extends HudElement {
    public WarpCooldownHud() {
        super("warp_cooldown", 10, 79, 1.0f, 100, 15,
                () -> ModConfig.INSTANCE.misc.enableWarpQueue,
                () -> GameState.Warp.cooldownEndAt > System.currentTimeMillis());
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        double remaining = isPreview ? 5.0 : Math.max(0, GameState.Warp.cooldownEndAt - System.currentTimeMillis()) / 1000.0;
        String suffix = (!isPreview && GameState.Warp.queuedCommand != null) ? " §e" + formatQueued(GameState.Warp.queuedCommand) : "";
        context.drawTextWithShadow(tr, String.format("§bWarp: %.1fs%s", remaining, suffix), 0, 0, 0xFFFFFFFF);
    }

    // /warp は必ず引数(warp名)を伴うため、"warp nest" のようなキュー済みコマンドから
    // 引数部分を取り出し、"(Queued - nest)" の形で表示する
    private static String formatQueued(String queuedCommand) {
        String arg = queuedCommand.regionMatches(true, 0, "warp ", 0, 5)
                ? queuedCommand.substring(5).trim()
                : queuedCommand;
        return "(Queued - " + arg + ")";
    }
}
