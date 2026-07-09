package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor; // 26.1.2仕様

public class PetHud extends HudElement {
    private static final long LEVEL_UP_DISPLAY_MS = 5000;

    public PetHud() {
        super("pet", 10, 10, 1.0f, 120, 24, () -> ModConfig.INSTANCE.misc.showPetHud, () -> true);
    }

    @Override
    public void renderElement(GuiGraphicsExtractor graphics, boolean isPreview) {
        // Minecraft.getInstance().font を使用
        Font font = Minecraft.getInstance().font;

        // プレビュー時はダミー、それ以外はGameStateから取得
        String petText = GameState.Player.activePetName != null ? GameState.Player.activePetName : "§7None";

        // レベルアップ直後は一定時間だけペット名の横に黄色く表示する
        if (!isPreview && System.currentTimeMillis() - GameState.Player.petLevelUpTime < LEVEL_UP_DISPLAY_MS) {
            petText += " §e(Level up!)";
        }

        // GuiGraphicsExtractor のメソッド: text(Font, String, x, y, color, shadow)
        graphics.text(font, "§e§lActive Pet", 0, 0, 0xFFFFFFFF, true);
        graphics.text(font, petText, 0, 12, 0xFFFFFFFF, true);
    }
}