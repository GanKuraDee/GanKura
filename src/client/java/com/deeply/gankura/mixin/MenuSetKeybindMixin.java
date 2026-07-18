package com.deeply.gankura.mixin;

import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// SkyHanniの「Loadouts/Wardrobe Keybind」を参考に、"Loadouts" "Armor Sets" "Equipment Sets"
// の各メニューが開いている間は数字キー(1〜9)で現在のページの項目を直接クリックできるようにする。
// スロット位置はHypixel側のGUIレイアウト固定値(Minecraftのバージョン/マッピングに依存しない):
//   - Loadouts: 先頭スロット14から3列×最大4行(1ページ目・2ページ目)、3ページ目のみ1行
//   - Armor Sets / Equipment Sets: 5行目(0-index 4)の9列がそれぞれの切り替えボタン
// 実際のクリックはslotClickedを直接呼び出すことで、左クリックした場合と完全に同じ経路(サーバーへの
// パケット送信、LoadoutsClickMixin/LoadoutsContentSyncMixinによるPet/Equipment Hud更新)を通す。
// 数字キーはバニラでは「ホバー中スロットをホットバーへスワップ」に使われるため、ここで消費して
// 意図しないスワップが発生しないようにする。
@Mixin(AbstractContainerScreen.class)
public abstract class MenuSetKeybindMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\((?<page>\\d+)/\\d+\\)\\s*Loadouts");
    private static final Pattern SET_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*(?:Armor|Equipment) Sets");

    private static final int MENU_WIDTH = 9;

    private static final int LOADOUTS_FIRST_SLOT = 14;
    private static final int[] LOADOUTS_ROWS_PER_PAGE = {4, 4, 1};
    private static final int LOADOUTS_SLOTS_PER_ROW = 3;

    private static final int SET_BUTTON_ROW = 4; // 5行目(0-index)
    private static final int SET_COLUMNS = 9;

    private static final int[] NUMBER_KEYS = {
            GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6,
            GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9,
    };

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput containerInput);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.INSTANCE.misc.enableSetKeybind) return;

        int index = indexOfNumberKey(event.key());
        if (index < 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int slotIndex = resolveTargetSlot(screen.getTitle().getString(), index);
        if (slotIndex < 0) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (slotIndex >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return;

        slotClicked(slot, slotIndex, 0, ContainerInput.PICKUP);
        cir.setReturnValue(true);
    }

    private static int resolveTargetSlot(String title, int index) {
        Matcher loadoutsMatcher = LOADOUTS_TITLE_PATTERN.matcher(title);
        if (loadoutsMatcher.find()) {
            int page = Integer.parseInt(loadoutsMatcher.group("page"));
            int rows = (page >= 1 && page <= LOADOUTS_ROWS_PER_PAGE.length) ? LOADOUTS_ROWS_PER_PAGE[page - 1] : 0;
            if (index >= rows * LOADOUTS_SLOTS_PER_ROW) return -1;
            return LOADOUTS_FIRST_SLOT + (index / LOADOUTS_SLOTS_PER_ROW) * MENU_WIDTH + (index % LOADOUTS_SLOTS_PER_ROW);
        }

        if (SET_TITLE_PATTERN.matcher(title).find()) {
            if (index >= SET_COLUMNS) return -1;
            return SET_BUTTON_ROW * MENU_WIDTH + index;
        }

        return -1;
    }

    private static int indexOfNumberKey(int keyCode) {
        for (int i = 0; i < NUMBER_KEYS.length; i++) {
            if (NUMBER_KEYS[i] == keyCode) return i;
        }
        return -1;
    }
}
