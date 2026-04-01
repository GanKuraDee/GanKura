package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class DayHud extends HudElement {
    public DayHud() {
        super("day", 10, 90, 1.0f, 60, 15, () -> ModConfig.INSTANCE.misc.showDayHud, () -> true);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        long day = client.world != null ? client.world.getTimeOfDay() / 24000L : 0;
        int color = 0xFFFFFFFF;
        boolean isTargetMap = ModConstants.MAP_THE_END.equals(GameState.Server.map) || ModConstants.MODE_COMBAT_3.equals(GameState.Server.mode);

        if (ModConfig.INSTANCE.golem.enableDay30Alert && isTargetMap && day >= 30 && ModConstants.STAGE_AWAKENING.equals(GameState.Golem.stage)) {
            color = 0xFFFF5555;
        }
        context.drawTextWithShadow(tr, "Day: " + String.format("%,d", day), 0, 0, color);
    }
}