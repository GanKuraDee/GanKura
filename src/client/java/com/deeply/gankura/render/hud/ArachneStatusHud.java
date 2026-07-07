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
        super("arachne_status", 200, 319, 1.0f, 270, 36,
                () -> ModConfig.INSTANCE.spidersDen.showArachneStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String status;

        if (isPreview) {
            status = "§eSpawning §c(12.0s)";
        } else {
            boolean inSanctuary = GameState.Arachne.inSanctuary;

            if (inSanctuary && GameState.Arachne.cobwebDetected) {
                // 基準座標に蜘蛛の巣ブロックが存在する = Spawned確定(Sanctuary外はスキャンしないため判定に使わない)
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
                    } else if (inSanctuary) {
                        status = "§eSpawning §e(Soon)";
                    } else {
                        // カウントダウン終了時点でSanctuary外にいた場合はSpawned/Killedとする
                        status = "§6Spawned/Killed §7(Go to Arachne's Sanctuary!)";
                    }
                }
            } else if (inSanctuary && GameState.Arachne.arachneMessageSeen) {
                // カウントダウン情報がない状態で「[BOSS] Arachne」を検知した場合の「間もなく」表示
                status = "§eSpawning §e(Soon)";
            } else if (inSanctuary && GameState.Arachne.downConfirmed) {
                // ARACHNE DOWN!確定済み
                status = "§aReady";
            } else if (inSanctuary && !GameState.Arachne.webAreaLoaded) {
                // Sanctuary内だが基準座標のチャンクが読み込まれておらず判定できない(稀なエッジケース)
                status = "§7Unknown §7(Go to Arachne's Sanctuary!)";
            } else if (inSanctuary) {
                // チャンクは読み込めており、蜘蛛の巣が存在しないと確認できた = Ready
                status = "§aReady";
            } else if (!GameState.Arachne.everConfirmed) {
                // Sanctuaryに一度もアクセスしておらず状態を確定できたことがない
                status = "§7Unknown §7(Go to Arachne's Sanctuary!)";
            } else if (GameState.Arachne.lastConfirmedWasReady) {
                // 直近Sanctuary内で確定した状態がReadyだった場合はエリア外でもReadyを維持する
                status = "§aReady";
            } else {
                // 直近確定した状態がSpawning/Spawnedだった場合はエリア外ではSpawned/Killedとする
                status = "§6Spawned/Killed §7(Go to Arachne's Sanctuary!)";
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
