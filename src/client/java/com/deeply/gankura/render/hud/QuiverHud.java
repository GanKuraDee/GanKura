package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

// 矢筒で選んでいる矢と残り本数
public class QuiverHud extends HudElement {

    public QuiverHud() {
        super("quiver", 460, 184, 1.0f, 130, 15,
                () -> ModConfig.INSTANCE.misc.showQuiverHud, () -> GameState.Player.quiverArrow != null);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        String text = isPreview && GameState.Player.quiverArrow == null
                ? "§b1,234x §fFlint Arrows"
                : label();

        drawTextWithShadow(context, tr, text, 0, 0, 0xFFFFFFFF);
    }

    private static String label() {
        int amount = GameState.Player.quiverArrowCount;
        // 矢の名前は読み取った色コードごと持っているので、ここでは色を付け直さない
        return String.format("§b%,dx %s", amount, pluralize(GameState.Player.quiverArrow, amount));
    }

    // 1本のときだけ単数形。"Flint Arrow" -> "Flint Arrows"
    private static String pluralize(String name, int amount) {
        if (name == null) return "";
        if (amount == 1 || name.endsWith("s")) return name;
        return name + "s";
    }
}
