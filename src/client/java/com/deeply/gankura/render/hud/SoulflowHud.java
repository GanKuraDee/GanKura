package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Soulflow。タブリストの Profile ウィジェットの "Soulflow:" 行を、色もそのままに出す。
 *
 * ウィジェットで Soulflow を出していないと行が無いので、
 * 何も出さずに黙っていると HUD の故障に見える。そのときは出し方を案内する
 */
public class SoulflowHud extends HudElement {

    // タブリストでは見出しが白、値が Dark Aqua
    private static final String LABEL = "§fSoulflow: ";
    private static final String VALUE_COLOR = "§3";
    private static final int PREVIEW_VALUE = 1764;

    private static final int LINE_HEIGHT = 12;

    public SoulflowHud() {
        super("soulflow", 460, 224, 1.0f, 90, LINE_HEIGHT,
                () -> ModConfig.Combat.showSoulflowHud,
                // タブリストをまだ読めていない間は、有るとも無いとも言えないので出さない
                () -> GameState.Server.isSkyblock()
                        && (GameState.Player.soulflow >= 0 || GameState.Player.soulflowWidgetMissing));
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Font font = Minecraft.getInstance().font;

        int soulflow = isPreview ? PREVIEW_VALUE : GameState.Player.soulflow;
        if (soulflow < 0) {
            missingWidget(graphics, font, 0, "Soulflow", "/widget -> Profile Widget -> Show Soulflow");
            return;
        }

        text(graphics, font, LABEL + VALUE_COLOR + String.format("%,d", soulflow), 0, 0, 0xFFFFFFFF, true);
    }
}
