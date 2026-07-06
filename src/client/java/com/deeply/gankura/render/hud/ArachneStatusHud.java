package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ArachneStatusHud extends HudElement {
    public ArachneStatusHud() {
        super("arachne_status", 400, 78, 1.0f, 230, 24,
                () -> ModConfig.INSTANCE.spidersDen.showArachneStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        String status;

        if (isPreview) {
            status = "§e(12.0s)";
        } else {
            boolean inSanctuary = GameState.Arachne.inSanctuary;

            if (GameState.Arachne.hasSpawned) {
                status = "§c(Spawned)";
            } else if (GameState.Arachne.isSummoning) {
                long remainingMs = GameState.Arachne.spawnTargetTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    status = String.format("§e(%.1fs)", remainingMs / 1000.0);
                } else {
                    status = inSanctuary ? "§e(Soon)" : "§6(Spawned/Killed - Go to Sanctuary)";
                }
            } else {
                status = inSanctuary ? "§7(Unknown)" : "§7(Unknown - Go to Sanctuary)";
            }
        }

        graphics.text(font, "§5§lArachne Status", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, "Status: " + status, 0, 12, 0xFFFFFFFF, true);
    }
}
