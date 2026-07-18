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

// SkyHanniの「Loadouts Keybind」を参考に、"Loadouts" メニューが開いている間は数字キー(1〜9)で
// 現在のページのロードアウト切り替えスロットを直接クリックできるようにする。
// スロット位置はHypixel側のGUIレイアウト固定値(1列目末尾の3列×最大4行、1ページ目の先頭スロットは14)
// で、Minecraftのバージョン/マッピングに依存しない。
// 実際のクリックはslotClickedを直接呼び出すことで、左クリックした場合と完全に同じ経路(サーバーへの
// パケット送信、LoadoutsClickMixin/LoadoutsContentSyncMixinによるPet/Equipment Hud更新)を通す。
// 数字キーはバニラでは「ホバー中スロットをホットバーへスワップ」に使われるため、ここで消費して
// 意図しないスワップが発生しないようにする。
@Mixin(AbstractContainerScreen.class)
public abstract class LoadoutsKeybindMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\((?<page>\\d+)/\\d+\\)\\s*Loadouts");
    private static final int FIRST_ICON_SLOT = 14;
    private static final int[] ROWS_PER_PAGE = {4, 4, 1};
    private static final int SLOTS_PER_ROW = 3;
    private static final int[] NUMBER_KEYS = {
            GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6,
            GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9,
    };

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput containerInput);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.INSTANCE.misc.enableLoadoutsKeybind) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        Matcher matcher = LOADOUTS_TITLE_PATTERN.matcher(screen.getTitle().getString());
        if (!matcher.find()) return;

        int index = indexOfNumberKey(event.key());
        if (index < 0) return;

        int page = Integer.parseInt(matcher.group("page"));
        int rows = (page >= 1 && page <= ROWS_PER_PAGE.length) ? ROWS_PER_PAGE[page - 1] : 0;
        if (index >= rows * SLOTS_PER_ROW) return;

        int slotIndex = FIRST_ICON_SLOT + (index / SLOTS_PER_ROW) * 9 + (index % SLOTS_PER_ROW);
        AbstractContainerMenu menu = screen.getMenu();
        if (slotIndex >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return;

        slotClicked(slot, slotIndex, 0, ContainerInput.PICKUP);
        cir.setReturnValue(true);
    }

    private static int indexOfNumberKey(int keyCode) {
        for (int i = 0; i < NUMBER_KEYS.length; i++) {
            if (NUMBER_KEYS[i] == keyCode) return i;
        }
        return -1;
    }
}
