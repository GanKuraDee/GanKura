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
        super("arachne_status", 400, 78, 1.0f, 270, 24,
                () -> ModConfig.INSTANCE.spidersDen.showArachneStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        String status;

        if (isPreview) {
            status = "§eSpawning §f(12.0s)";
        } else {
            boolean inSanctuary = GameState.Arachne.inSanctuary;

            if (GameState.Arachne.isReady) {
                status = "§aReady";
            } else if (GameState.Arachne.hasSpawned) {
                status = inSanctuary ? "§cSpawned" : "§6Spawned/Killed §f(Go to Arachne's Sanctuary!)";
            } else if (GameState.Arachne.isSummoning) {
                long remainingMs = GameState.Arachne.spawnTargetTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    status = String.format("§eSpawning §f(%.1fs)", remainingMs / 1000.0);
                } else {
                    status = inSanctuary ? "§eSpawning §f(Soon)" : "§6Spawned/Killed §f(Go to Arachne's Sanctuary!)";
                }
            } else if (inSanctuary) {
                // チャットからの情報が何もない状態でSanctuaryにいる場合は、実際のエンティティ検知状況で判定する
                status = GameState.Arachne.isDetected ? "§cSpawned" : "§aReady";
            } else {
                status = "§7Unknown §f(Go to Arachne's Sanctuary!)";
            }
        }

        graphics.text(font, "§5§lArachne Status", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, "Altar: " + status, 0, 12, 0xFFFFFFFF, true);
    }
}
