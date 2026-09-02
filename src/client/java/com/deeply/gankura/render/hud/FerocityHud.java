package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// Ferocity。タブリストから読めているときだけ出す。
// 読めないときは表示ごと消える(出し方の案内は設定画面の説明にある)
public class FerocityHud extends HudElement {

    // Hypixel のリソースパックが持つ Ferocity のアイコン。SkyHanni と同じ文字を使う。
    // パックを入れていないと豆腐(□)になるが、そこは本家の表示に合わせている
    private static final String ICON = "\uE00B";

    public FerocityHud() {
        super("ferocity", 460, 208, 1.0f, 50, 15,
                () -> ModConfig.INSTANCE.generalHud.showFerocityHud, () -> GameState.Player.ferocity >= 0);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        int ferocity = Math.max(GameState.Player.ferocity, 0);
        text(graphics, font, "§c" + ICON + String.format("%,d", ferocity), 0, 0, 0xFFFFFFFF, true);
    }
}
