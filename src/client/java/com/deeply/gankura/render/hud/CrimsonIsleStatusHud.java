package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class CrimsonIsleStatusHud extends HudElement {
    public CrimsonIsleStatusHud() {
        super("crimson_isle_status", 200, 110, 1.0f, 180, 76,
                () -> ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleStatusHud,
                () -> ModConstants.MAP_CRIMSON_ISLE.equals(GameState.Server.map)
                        || ModConstants.MODE_CRIMSON_ISLE.equals(GameState.Server.mode));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, "§c§lNether Boss Status", 0, 0, 0xFFFFFFFF);
        int y = 14;
        for (var boss : EntityHighlightManager.CRIMSON_BOSSES) {
            String status;
            if (isPreview) {
                status = "§aSpawned";
            } else if ("Magma Boss".equals(boss.nameTag())) {
                // Magma Boss はスコアボードのみで判定
                String sp = GameState.MagmaBoss.spawnStatus;
                if (sp != null) {
                    status = "§aSpawned §7(" + sp + ")";
                } else {
                    long respawnEnd = GameState.MagmaBoss.respawnEndTime;
                    long remaining = respawnEnd - System.currentTimeMillis();
                    if (remaining > 0) {
                        long secs = remaining / 1000;
                        status = String.format("§eRespawning §f%dm %02ds", secs / 60, secs % 60);
                    } else if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) {
                        status = "§aReady";
                    } else {
                        status = "§7Unknown §8(spawns within 2m)";
                    }
                }
            } else {
                long respawnEnd = getRespawnEndTime(boss.nameTag());
                long remaining = respawnEnd - System.currentTimeMillis();
                if (remaining > 0) {
                    long secs = remaining / 1000;
                    status = String.format("§eRespawning §f%dm %02ds", secs / 60, secs % 60);
                } else if (boss.getIsDetected().get()) {
                    status = "§aSpawned";
                } else if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) {
                    status = "§aReady";
                } else {
                    status = "§7Unknown §8(spawns within 2m)";
                }
            }
            String nameColor = colorCode(boss.glowColorRGB());
            context.drawTextWithShadow(tr, nameColor + boss.nameTag() + "§f: " + status, 0, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    private static long getRespawnEndTime(String nameTag) {
        return switch (nameTag) {
            case "Barbarian Duke X" -> GameState.BarbarianDukeX.respawnEndTime;
            case "Bladesoul"        -> GameState.Bladesoul.respawnEndTime;
            case "Mage Outlaw"      -> GameState.MageOutlaw.respawnEndTime;
            case "Ashfang"          -> GameState.Ashfang.respawnEndTime;
            case "Magma Boss"       -> GameState.MagmaBoss.respawnEndTime;
            default                 -> 0L;
        };
    }

    private static String colorCode(int rgb) {
        return switch (rgb) {
            case 0xFF5555 -> "§c"; // Barbarian Duke X
            case 0x555555 -> "§8"; // Bladesoul
            case 0xAA00AA -> "§5"; // Mage Outlaw
            case 0xAAAAAA -> "§7"; // Ashfang
            case 0xFFAA00 -> "§6"; // Magma Boss
            default -> "§f";
        };
    }
}
