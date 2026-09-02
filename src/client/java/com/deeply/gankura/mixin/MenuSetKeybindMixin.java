package com.deeply.gankura.mixin;

import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// SkyHanniの「Loadouts/Wardrobe Keybind」を参考に、"Loadouts" "Armor Sets" "Equipment Sets"
// の各メニューが開いている間は設定画面で個別に割り当てたキーで現在のページの項目を直接
// クリックできるようにする。キー自体は数字キーに限らず、メニューごとに9個まで独立して設定可能。
// スロット位置はHypixel側のGUIレイアウト固定値(Minecraftのバージョン/マッピングに依存しない):
//   - Loadouts: 先頭スロット14から3列×最大4行(1ページ目・2ページ目)、3ページ目のみ1行
//   - Armor Sets / Equipment Sets: 5行目(0-index 4)の9列がそれぞれの切り替えボタン
// 実際のクリックはslotClickedを直接呼び出すことで、左クリックした場合と完全に同じ経路(サーバーへの
// パケット送信、LoadoutsClickMixin/LoadoutsContentSyncMixinによるPet/Equipment Hud更新)を通す。
// ただしバニラのクリックは「アイテムを拾う」予測を手元で先に進めるため、そのままだと
// アイテムが一瞬カーソルに乗って見える。これらのメニューはHypixel側の押しボタンで、
// 手元の予測はサーバーの返事で必ず上書きされるので、送った直後に見た目を戻している。
// 割り当てたキーはバニラでは「ホバー中スロットをホットバーへスワップ」に使われる場合があるため、
// ここで消費して意図しないスワップが発生しないようにする。
@Mixin(AbstractContainerScreen.class)
public abstract class MenuSetKeybindMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\((?<page>\\d+)/\\d+\\)\\s*Loadouts");
    private static final Pattern ARMOR_SET_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Armor Sets");
    private static final Pattern EQUIPMENT_SET_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Equipment Sets");

    private static final int MENU_WIDTH = 9;

    private static final int LOADOUTS_FIRST_SLOT = 14;
    private static final int[] LOADOUTS_ROWS_PER_PAGE = {4, 4, 1};
    private static final int LOADOUTS_SLOTS_PER_ROW = 3;

    private static final int SET_BUTTON_ROW = 4; // 5行目(0-index)
    private static final int SET_COLUMNS = 9;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput containerInput);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int slotIndex = resolveTargetSlot(screen.getTitle().getString(), event.key());
        if (slotIndex < 0) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (slotIndex >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return;

        ItemStack carried = menu.getCarried().copy();
        ItemStack clicked = slot.getItem().copy();

        slotClicked(slot, slotIndex, 0, ContainerInput.PICKUP);

        // 送るものは送った上で、拾った扱いになった手元の見た目だけ元に戻す
        menu.setCarried(carried);
        slot.set(clicked);
        cir.setReturnValue(true);
    }

    private static int resolveTargetSlot(String title, int keyCode) {
        ModConfig.KeybindsCategory keybinds = ModConfig.INSTANCE.keybinds;

        Matcher loadoutsMatcher = LOADOUTS_TITLE_PATTERN.matcher(title);
        if (loadoutsMatcher.find()) {
            if (!keybinds.enableLoadoutsKeybind) return -1;
            int index = indexOfKey(keyCode,
                    keybinds.loadoutsKeybindSlot1, keybinds.loadoutsKeybindSlot2, keybinds.loadoutsKeybindSlot3,
                    keybinds.loadoutsKeybindSlot4, keybinds.loadoutsKeybindSlot5, keybinds.loadoutsKeybindSlot6,
                    keybinds.loadoutsKeybindSlot7, keybinds.loadoutsKeybindSlot8, keybinds.loadoutsKeybindSlot9,
                    keybinds.loadoutsKeybindSlot10, keybinds.loadoutsKeybindSlot11, keybinds.loadoutsKeybindSlot12);
            if (index < 0) return -1;

            int page = Integer.parseInt(loadoutsMatcher.group("page"));
            int rows = (page >= 1 && page <= LOADOUTS_ROWS_PER_PAGE.length) ? LOADOUTS_ROWS_PER_PAGE[page - 1] : 0;
            if (index >= rows * LOADOUTS_SLOTS_PER_ROW) return -1;
            return LOADOUTS_FIRST_SLOT + (index / LOADOUTS_SLOTS_PER_ROW) * MENU_WIDTH + (index % LOADOUTS_SLOTS_PER_ROW);
        }

        if (ARMOR_SET_TITLE_PATTERN.matcher(title).find()) {
            if (!keybinds.enableArmorSetKeybind) return -1;
            int index = indexOfKey(keyCode,
                    keybinds.armorSetKeybindSlot1, keybinds.armorSetKeybindSlot2, keybinds.armorSetKeybindSlot3,
                    keybinds.armorSetKeybindSlot4, keybinds.armorSetKeybindSlot5, keybinds.armorSetKeybindSlot6,
                    keybinds.armorSetKeybindSlot7, keybinds.armorSetKeybindSlot8, keybinds.armorSetKeybindSlot9);
            if (index < 0 || index >= SET_COLUMNS) return -1;
            return SET_BUTTON_ROW * MENU_WIDTH + index;
        }

        if (EQUIPMENT_SET_TITLE_PATTERN.matcher(title).find()) {
            if (!keybinds.enableEquipmentSetKeybind) return -1;
            int index = indexOfKey(keyCode,
                    keybinds.equipmentSetKeybindSlot1, keybinds.equipmentSetKeybindSlot2, keybinds.equipmentSetKeybindSlot3,
                    keybinds.equipmentSetKeybindSlot4, keybinds.equipmentSetKeybindSlot5, keybinds.equipmentSetKeybindSlot6,
                    keybinds.equipmentSetKeybindSlot7, keybinds.equipmentSetKeybindSlot8, keybinds.equipmentSetKeybindSlot9);
            if (index < 0 || index >= SET_COLUMNS) return -1;
            return SET_BUTTON_ROW * MENU_WIDTH + index;
        }

        return -1;
    }

    // 未設定のキーバインドは-1(KeyboardConstants.none)で保存されるため、
    // event.key()が負の値になることは実際にはないが念のため除外する
    private static int indexOfKey(int keyCode, int... keybinds) {
        if (keyCode < 0) return -1;
        for (int i = 0; i < keybinds.length; i++) {
            if (keybinds[i] == keyCode) return i;
        }
        return -1;
    }
}
