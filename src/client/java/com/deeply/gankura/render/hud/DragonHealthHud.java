package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

// Dragonの残りHPを表示するHUD。
// 他のボスと異なり値の取得元がサイドバー(EntityHealthScanner#scanDragonScoreboard)で、
// 最大HPが得られないため「現在HPのみ」を表示し、割合による色分けは行わない。
public class DragonHealthHud extends HudElement {
    public DragonHealthHud() {
        super("dragon_health", 165, 202, 1.0f, 120, 24,
                () -> ModConfig.INSTANCE.theEnd.showDragonHealthHud,
                () -> (ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode))
                        && GameState.Dragon.health != null);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

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

        drawTextWithShadow(context, tr, title, 0, 0, 0xFFFFFFFF);
        drawTextWithShadow(context, tr, hpText, 0, 12, 0xFFFFFFFF);
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
