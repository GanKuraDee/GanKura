package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DayHud extends HudElement {
    public DayHud() {
        super("day", 10, 46, 1.0f, 60, 15, () -> ModConfig.INSTANCE.misc.showDayHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        // ★変更: ClientLevel からではなく、Mixin で傍受したサーバーの正確な dayTime を使用する
        long day = GameState.Server.dayTime / 24000L;

        int color = 0xFFFFFFFF;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);

        if (ModConfig.INSTANCE.theEnd.enableDay30Alert && isTargetMap && day >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage)) {
            color = 0xFFFF5555;
        }

        text(graphics, font, "Day: " + String.format("%,d", day), 0, 0, color, true);
    }
}