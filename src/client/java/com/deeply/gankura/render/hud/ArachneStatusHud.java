package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class ArachneStatusHud extends HudElement {
    public ArachneStatusHud() {
        super("arachne_status", 400, 78, 1.0f, 270, 24,
                () -> ModConfig.INSTANCE.spidersDen.showArachneStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String status;

        if (isPreview) {
            status = "§eSpawning §f(12.0s)";
        } else {
            boolean inSanctuary = GameState.Arachne.inSanctuary;

            if (GameState.Arachne.hasSpawned) {
                status = "§cSpawned";
            } else if (GameState.Arachne.isSummoning) {
                long remainingMs = GameState.Arachne.spawnTargetTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    status = String.format("§eSpawning §f(%.1fs)", remainingMs / 1000.0);
                } else {
                    status = inSanctuary ? "§eSpawning §f(Soon)" : "§6Spawned/Killed §f(Go to Arachne's Sanctuary!)";
                }
            } else {
                status = inSanctuary ? "§7Unknown" : "§7Unknown §f(Go to Arachne's Sanctuary!)";
            }
        }

        context.drawTextWithShadow(tr, "§5§lArachne Status", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, "Status: " + status, 0, 12, 0xFFFFFFFF);
    }
}
