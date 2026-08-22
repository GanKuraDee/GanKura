package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

// Dragonの残りHPを表示するHUD。
// 他のボスと異なり値の取得元がサイドバー(EntityHealthScanner#scanDragonScoreboard)で、
// 最大HPが得られないため「現在HPのみ」を表示し、割合による色分けは行わない。
public class DragonHealthHud extends HudElement {
    public DragonHealthHud() {
        super("dragon_health", 230, 226, 1.0f, 120, 24,
                () -> ModConfig.INSTANCE.theEnd.showDragonHealthHud,
                () -> (GameState.Server.isTheEnd())
                        && GameState.Dragon.health != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        String title = "§d§lDragon HP";
        String hpText;
        if (isPreview) {
            title = "§c§lDragon HP";
            hpText = "§a4,824,217";
        } else {
            String type = GameState.Dragon.type;
            if (type != null) title = typeColorCode(type) + "§l" + type + " Dragon HP";
            hpText = "§a" + GameState.Dragon.health;
        }

        text(graphics, font, title, 0, 0, 0xFFFFFFFF, true);
        text(graphics, font, hpText, 0, 12, 0xFFFFFFFF, true);
    }

    // DragonStatusHud と同じ配色ルール
    static String typeColorCode(String type) {
        return switch (type) {
            case "Protector" -> "§8";
            case "Old"       -> "§7";
            case "Unstable"  -> "§5";
            case "Young"     -> "§f";
            case "Strong"    -> "§c";
            case "Wise"      -> "§b";
            case "Superior"  -> "§e";
            default          -> "§d";
        };
    }
}
