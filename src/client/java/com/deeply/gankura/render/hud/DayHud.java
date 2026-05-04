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
        super("day", 10, 90, 1.0f, 60, 15, () -> ModConfig.INSTANCE.misc.showDayHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        long day = 0;
        /*
         * ClientLevel.class の L178 および L517 (内部クラス ClientLevelData) を参照
         * getLevelData() は ClientLevelData 型を返し、
         * その中の getGameTime() がワールドの累積ティック数を保持しています。
         */
        if (client.level != null) {
            // getDayTime() ではなく getGameTime() を使用します
            day = client.level.getLevelData().getGameTime() / 24000L;
        }

        int color = 0xFFFFFFFF;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);

        if (ModConfig.INSTANCE.golem.enableDay30Alert && isTargetMap && day >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage)) {
            color = 0xFFFF5555;
        }

        graphics.text(font, "Day: " + String.format("%,d", day), 0, 0, color, true);
    }
}