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
        super("arachne_status", 400, 78, 1.0f, 270, 36,
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

            if (GameState.Arachne.isReady) {
                status = "§aReady";
            } else if (GameState.Arachne.hasSpawned) {
                status = inSanctuary ? "§cSpawned" : "§6Spawned/Killed §f(Go to Arachne's Sanctuary!)";
            } else if (GameState.Arachne.isSummoning) {
                if (GameState.Arachne.awaitingCrystalParticles) {
                    // Big(Crystal)はパーティクル観測でQuick/Normalが確定するまで秒数を出せない
                    status = "§eSpawning §f(...)";
                } else {
                    long remainingMs = GameState.Arachne.spawnTargetTime - System.currentTimeMillis();
                    if (remainingMs > 0) {
                        status = String.format("§eSpawning §f(%.1fs)", remainingMs / 1000.0);
                    } else {
                        status = inSanctuary ? "§eSpawning §f(Soon)" : "§6Spawned/Killed §f(Go to Arachne's Sanctuary!)";
                    }
                }
            } else if (inSanctuary) {
                // チャットからの情報が何もない状態でSanctuaryにいる場合は、実際のエンティティ検知状況で判定する
                status = GameState.Arachne.isDetected ? "§cSpawned" : "§aReady";
            } else {
                status = "§7Unknown §f(Go to Arachne's Sanctuary!)";
            }
        }

        context.drawTextWithShadow(tr, "§5§lArachne Status", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, "Altar: " + status, 0, 12, 0xFFFFFFFF);

        // Small/Bigはカウントダウン開始メッセージの時点で確定しているため、Spawned確定を待たず表示する
        if (isPreview) {
            context.drawTextWithShadow(tr, "Size: §aSmall", 0, 24, 0xFFFFFFFF);
        } else if (GameState.Arachne.size != null) {
            String sizeColor = "Big".equals(GameState.Arachne.size) ? "§c" : "§a";
            context.drawTextWithShadow(tr, "Size: " + sizeColor + GameState.Arachne.size, 0, 24, 0xFFFFFFFF);
        }
    }
}
