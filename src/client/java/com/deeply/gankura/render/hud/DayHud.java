package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class DayHud extends HudElement {
    public DayHud() {
        super("day", 10, 90, 1.0f, 60, 15, () -> ModConfig.INSTANCE.misc.showDayHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        // ワールド時刻から日数を計算 (1日 = 24,000 ticks)
        long day = client.level != null ? client.level.getDayTime() / 24000L : 0;
        int color = 0xFFFFFFFF;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);

        // 特定条件下でのアラート色（赤）の判定
        if (ModConfig.INSTANCE.golem.enableDay30Alert && isTargetMap && day >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage)) {
            color = 0xFFFF5555;
        }

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "Day: " + String.format("%,d", day), 0, 0, color, true);
    }
}