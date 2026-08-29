package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class DragonStatusHud extends HudElement {
    public DragonStatusHud() {
        super("dragon", 230, 104, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.theEnd.showDragonStatusHud,
                () -> GameState.Server.isTheEnd());
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;
        String eggState; String eyePlaced = null; String dragonType = null;

        if (isPreview) {
            eggState = "§cEgg: Hatched (Spawned)";
            eyePlaced = "§cEyes placed: 8/8 §b(2)";
            dragonType = "Type: §eSuperior";
        } else {
            String state = GameState.Dragon.eggState;
            int eyes = GameState.Dragon.eyes;

            if ("Hatching".equals(state)) {
                long timeSincePacket = Math.min(System.currentTimeMillis() - GameState.Server.lastPacketArrivalMillis, 1000);
                double remainingTicks = Math.max(0, GameState.Dragon.spawnTargetTime - (GameState.Server.lastTimePacket + (timeSincePacket / 50.0)));
                eggState = remainingTicks > 0 ? String.format("Egg: §eHatching §c(%.1fs)", remainingTicks / 20.0) : "Egg: §eHatching §e(Soon)";
            } else {
                String colorCode = switch (state) {
                    case "Ready" -> "§a";
                    case "Hatched" -> "§c";
                    case "Respawning" -> "§7";
                    case "Scanning..." -> "§8";
                    default -> "§f";
                };
                eggState = "Hatched".equals(state) ? "§cEgg: Hatched (Spawned)" : "Egg: " + colorCode + state;
            }

            if ("Respawning".equals(state)) {
                eyePlaced = "§mEyes placed: §e§m0§7§m/§a§m8";
            } else if (!"Scanning...".equals(state)) {
                eyePlaced = eyes == 8 ? "§cEyes placed: 8/8" : "Eyes placed: §e" + eyes + "§7/§a8";
                if (GameState.Dragon.playerEyes > 0) eyePlaced += " §b(" + GameState.Dragon.playerEyes + ")";
            }

            if (GameState.Dragon.type != null) {
                String typeColorCode = switch (GameState.Dragon.type) {
                    case "Protector" -> "§8";
                    case "Old" -> "§7";
                    case "Unstable" -> "§5";
                    case "Young" -> "§f";
                    case "Strong" -> "§c";
                    case "Wise" -> "§b";
                    case "Superior" -> "§e";
                    default -> "§d";
                };
                dragonType = "Type: " + typeColorCode + GameState.Dragon.type;
            }
        }

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        text(graphics, font, "§d§lDragon Status", 0, 0, 0xFFFF55FF, true);
        text(graphics, font, eggState, 0, 12, 0xFFFFFFFF, true);

        int nextY = 24;
        if (eyePlaced != null) {
            text(graphics, font, eyePlaced, 0, nextY, 0xFFFFFFFF, true);
            nextY += 12;
        }
        if (dragonType != null) {
            text(graphics, font, dragonType, 0, nextY, 0xFFFFFFFF, true);
        }
    }
}