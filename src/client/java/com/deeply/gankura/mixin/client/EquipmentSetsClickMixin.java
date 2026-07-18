package com.deeply.gankura.mixin.client;

import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

// "Equipment Sets" メニューの5行目(切り替えボタン)がクリックされた瞬間、対応する列の
// プレビュー(1〜4行目)を即座にEquipment Hudへ反映する。列のプレビューはサーバー同期を
// 待たずともクリック時点で既にメニュー内に表示されているため、読み取るだけで正確な内容が得られる
// (Loadoutsのようにサーバーからのコンテナ同期を待つ必要がない)。
// MenuSetKeybindMixin経由のキーバインドクリックでもonMouseClickを通るため、同様に反応する。
// onMouseClickはオーバーロードされているため、Mixinの対象メソッドをディスクリプタで明示する
@Mixin(HandledScreen.class)
public class EquipmentSetsClickMixin {
    private static final Pattern EQUIPMENT_SETS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Equipment Sets");
    private static final int SET_BUTTON_ROW = 4; // 5行目(0-index)
    private static final int MENU_WIDTH = 9;

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (button != 0) return;
        if (slot == null || !slot.hasStack()) return;
        if (slotId < SET_BUTTON_ROW * MENU_WIDTH || slotId >= (SET_BUTTON_ROW + 1) * MENU_WIDTH) return;

        Screen screen = (Screen) (Object) this;
        if (!EQUIPMENT_SETS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        ScreenHandler handler = ((HandledScreen<?>) screen).getScreenHandler();
        int column = slotId % MENU_WIDTH;
        EquipmentScanner.onEquipmentSetSlotClicked(handler, column);
    }
}
