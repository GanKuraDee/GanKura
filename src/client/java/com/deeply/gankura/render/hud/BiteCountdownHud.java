package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.handler.FishingBobberTracker;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * 何かがかかるまでの残り時間。
 *
 * Hypixel は浮きのそばにアーマースタンドで秒数を出しているので、その文字をそのまま画面に持ってくる。
 * 目線を浮きから外していても残り時間が分かる。
 */
public class BiteCountdownHud extends HudElement {

    public BiteCountdownHud() {
        super("bite_countdown", 220, 120, 1.0f, 40, 15,
                () -> ModConfig.INSTANCE.fishing.showBiteCountdownHud,
                () -> BiteCountdownHud.text() != null);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        String text = text();
        if (isPreview && text == null) text = "§e§l2.0";

        drawTextWithShadow(context, font, text, 0, 0, 0xFFFFFFFF);
    }

    /** 今出すべき文字。出すものが無ければ null */
    private static String text() {
        // 魚が来た合図は、残り時間より優先して出す
        if (FishingBobberTracker.fishArrived()) return "§c§l!!!";

        String countdown = FishingBobberTracker.hypixelCountdown();
        return countdown == null ? null : "§e§l" + countdown;
    }
}
