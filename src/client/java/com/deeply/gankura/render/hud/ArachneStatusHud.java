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
        super("arachne_status", 200, 319, 1.0f, 270, 36,
                () -> ModConfig.INSTANCE.spidersDen.showArachneStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        String status;

        if (isPreview) {
            status = "§eSpawning §c(12.0s)";
        } else {
            boolean inSanctuary = GameState.Arachne.inSanctuary;

            if (GameState.Arachne.cobwebDetected) {
                // 基準座標に蜘蛛の巣ブロックが存在する = Spawned確定
                status = "§cSpawned";
            } else if (GameState.Arachne.isSummoning) {
                if (GameState.Arachne.awaitingCrystalParticles) {
                    // Big(Crystal)はパーティクル観測でQuick/Normalが確定するまで秒数を出せない
                    status = "§eSpawning §f(...)";
                } else {
                    long timeSincePacket = Math.min(System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis, 1000);
                    double remainingTicks = Math.max(0, GameState.Arachne.spawnTargetTime - (GameState.Server.lastTimePacket + (timeSincePacket / 50.0)));
                    if (remainingTicks > 0) {
                        status = String.format("§eSpawning §c(%.1fs)", remainingTicks / 20.0);
                    } else {
                        status = inSanctuary ? "§eSpawning §e(Soon)" : "§6Spawned/Killed §7(Go to Arachne's Sanctuary!)";
                    }
                }
            } else if (GameState.Arachne.arachneMessageSeen) {
                // カウントダウン情報がない状態で「[BOSS] Arachne」を検知した場合の「間もなく」表示
                status = "§eSpawning §e(Soon)";
            } else if (inSanctuary) {
                // Sanctuary入場時のデフォルトはReady。蜘蛛の巣を検知した時点でSpawnedに切り替わる
                status = "§aReady";
            } else {
                status = "§7Unknown §7(Go to Arachne's Sanctuary!)";
            }
        }

        graphics.text(font, "§5§lArachne Status", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, "Altar: " + status, 0, 12, 0xFFFFFFFF, true);

        // Small/Bigはカウントダウン開始メッセージの時点で確定しているため、Spawned確定を待たず表示する
        if (isPreview) {
            graphics.text(font, "Size: §aSmall", 0, 24, 0xFFFFFFFF, true);
        } else if (GameState.Arachne.size != null) {
            String sizeColor = "Big".equals(GameState.Arachne.size) ? "§c" : "§a";
            graphics.text(font, "Size: " + sizeColor + GameState.Arachne.size, 0, 24, 0xFFFFFFFF, true);
        }
    }
}
