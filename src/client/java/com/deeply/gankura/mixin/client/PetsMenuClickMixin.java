package com.deeply.gankura.mixin.client;

import com.deeply.gankura.handler.PetHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
// onMouseClickはオーバーロードされているため、Mixinの対象メソッドをディスクリプタで明示する
@Mixin(HandledScreen.class)
public class PetsMenuClickMixin {
    private static final Pattern PETS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Pets");

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (button != 0) return;
        if (slot == null || !slot.hasStack()) return;

        Screen screen = (Screen) (Object) this;
        if (!PETS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        PetHandler.processPetsMenuClick(slot.getStack());
    }
}
