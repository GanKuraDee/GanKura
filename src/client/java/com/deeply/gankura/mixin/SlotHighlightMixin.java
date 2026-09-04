package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.AuctionHandler;
import com.deeply.gankura.handler.BazaarOrderHandler;
import com.deeply.gankura.util.HighlightColor;
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
 * Bazaar に出している注文と、Auction House に出している品の枠を塗る。
 *
 * 何色に塗るかは {@link BazaarOrderHandler} と {@link AuctionHandler} が決める
 */
@Mixin(AbstractContainerScreen.class)
public class SlotHighlightMixin {

    @Unique
    private static final int SLOT_SIZE = 16;

    // アイテムより先に塗る。後から重ねると品が透けて見づらくなるので、下に敷く
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void gankura$markOwnOffers(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                       CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        String title = screen.getTitle().getString();

        Integer color = null;
        if (BazaarOrderHandler.inOrderMenu(title)) {
            color = BazaarOrderHandler.colorFor(slot);
        } else if (AuctionHandler.inManageMenu(title)) {
            color = AuctionHandler.colorFor(slot);
        }
        if (color == null) return;

        graphics.fill(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, HighlightColor.tint(color));
    }
}
