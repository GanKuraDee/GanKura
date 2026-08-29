package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// 釣り竿に付けている餌と、その残数
public class BaitHud extends HudElement {

    public BaitHud() {
        super("bait", 460, 199, 1.0f, 130, 15,
                () -> ModConfig.INSTANCE.fishing.showBaitHud, () -> GameState.Player.fishingBait != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        String text = isPreview && GameState.Player.fishingBait == null
                ? "§b48x §9Whale Bait"
                : label();

        text(graphics, font, text, 0, 0, 0xFFFFFFFF, true);
    }

    private static String label() {
        // 餌の名前は読み取った色コードごと持っているので、ここでは色を付け直さない
        return String.format("§b%,dx %s", GameState.Player.fishingBaitCount, GameState.Player.fishingBait);
    }
}
