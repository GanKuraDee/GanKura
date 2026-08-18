package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// 矢筒で選んでいる矢と残り本数
public class QuiverHud extends HudElement {

    public QuiverHud() {
        super("quiver", 460, 184, 1.0f, 130, 15,
                () -> ModConfig.INSTANCE.misc.showQuiverHud, () -> GameState.Player.quiverArrow != null);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        String text = isPreview && GameState.Player.quiverArrow == null
                ? "§b1,234x §fFlint Arrows"
                : label();

        text(graphics, font, text, 0, 0, 0xFFFFFFFF, true);
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
