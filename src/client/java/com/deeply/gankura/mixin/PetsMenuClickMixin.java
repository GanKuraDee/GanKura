package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PetHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

// "Pets" メニューはActive Petの表示位置が固定でないため、アイテムを左クリックした
// 瞬間のスロットからツールチップを読み取る必要がある(読み取り自体はPetHandlerに委譲)
// タイトルは "(1/4) Pets" のようにページ番号付きで表示される。単純に"Pets"を含むかで
// 判定すると、ペットを配置しても切り替えにはならない別メニュー("Offer Pets"等)にも
// 誤って反応してしまうため、ページ番号表記込みで厳密にマッチさせる
@Mixin(AbstractContainerScreen.class)
public class PetsMenuClickMixin {
    private static final Pattern PETS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Pets");

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (buttonNum != 0) return;
        if (slot == null || !slot.hasItem()) return;

        Screen screen = (Screen) (Object) this;
        if (!PETS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        PetHandler.processPetsMenuClick(slot.getItem());
    }
}
