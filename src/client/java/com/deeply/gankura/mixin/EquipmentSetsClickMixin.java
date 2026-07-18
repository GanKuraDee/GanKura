package com.deeply.gankura.mixin;

import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

// "Equipment Sets" メニューの5行目(切り替えボタン)がクリックされた瞬間、対応する列の
// プレビュー(1〜4行目)を即座にEquipment Hudへ反映する。列のプレビューはサーバー同期を
// 待たずともクリック時点で既にメニュー内に表示されているため、読み取るだけで正確な内容が得られる
// (Loadoutsのようにサーバーからのコンテナ同期を待つ必要がない)。
// MenuSetKeybindMixin経由のキーバインドクリックでもslotClickedを通るため、同様に反応する。
@Mixin(AbstractContainerScreen.class)
public class EquipmentSetsClickMixin {
    private static final Pattern EQUIPMENT_SETS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Equipment Sets");
    private static final int SET_BUTTON_ROW = 4; // 5行目(0-index)
    private static final int MENU_WIDTH = 9;

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (buttonNum != 0) return;
        if (slot == null || !slot.hasItem()) return;
        if (slotId < SET_BUTTON_ROW * MENU_WIDTH || slotId >= (SET_BUTTON_ROW + 1) * MENU_WIDTH) return;

        Screen screen = (Screen) (Object) this;
        if (!EQUIPMENT_SETS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        AbstractContainerMenu menu = ((AbstractContainerScreen<?>) screen).getMenu();
        int column = slotId % MENU_WIDTH;
        EquipmentScanner.onEquipmentSetSlotClicked(menu, column);
    }
}
