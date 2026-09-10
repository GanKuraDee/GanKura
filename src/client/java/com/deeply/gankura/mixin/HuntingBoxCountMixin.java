package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.TierText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hunting Box に並ぶシャードへ、今の所持数を添える。
 *
 * 所持数はロアの "Owned: 72 Shards" にしか書かれておらず、
 * 1つずつカーソルを合わせないと分からない。
 * アイテムの個数と同じ右下の隅に出して、一覧のまま見比べられるようにする
 */
@Mixin(AbstractContainerScreen.class)
public class HuntingBoxCountMixin {

    // 題は "(1/7) Hunting Box" のようにページ数が頭に付く
    @Unique
    private static final String MENU_TITLE = "Hunting Box";

    // 数字を置く位置。アイテムの個数と同じ、右下の隅
    @Unique
    private static final int COUNT_RIGHT = 17;
    @Unique
    private static final int COUNT_BOTTOM = 9;
    @Unique
    private static final int COUNT_COLOR = 0xFFFFFF55;

    // 4桁を超えると枠からはみ出して読めないので、そこから先は丸める
    @Unique
    private static final int SHORTEN_FROM = 10000;
    @Unique
    private static final int THOUSAND = 1000;

    // 数字は読めないと意味が無いので、アイテムを描いた後に重ねる
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void gankura$markShardsOwned(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                         CallbackInfo ci) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableHuntingBoxTweaks || !config.showShardsOwned) return;
        if (!GameState.Server.isSkyblock()) return;

        Screen screen = (Screen) (Object) this;
        if (!screen.getTitle().getString().contains(MENU_TITLE)) return;

        Integer owned = TierText.shardsOwned(slot.getItem());
        if (owned == null) return;

        Font font = Minecraft.getInstance().font;
        String text = gankura$shorten(owned);

        graphics.text(font, text, slot.x + COUNT_RIGHT - font.width(text), slot.y + COUNT_BOTTOM,
                COUNT_COLOR);
    }

    @Unique
    private static String gankura$shorten(int owned) {
        if (owned < SHORTEN_FROM) return String.valueOf(owned);
        return owned / THOUSAND + "k";
    }
}
