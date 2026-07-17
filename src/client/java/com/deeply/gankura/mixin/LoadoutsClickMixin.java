package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

// "Loadouts" メニューのロードアウト切り替えスロットをクリックした瞬間、そのスロット自身のツールチップから
// Pet Hud用の情報を読み取る(こちらはロードアウトに保存された情報のためクリック時点で確定している)。
// Equipment Hudについては、クリック直後はまだサーバー側の切り替えが反映されていないため、
// ここでは全EquipmentスロットをBarrier(Unknown)化するだけにとどめ、実際の読み取りは
// LoadoutsContentSyncMixin(サーバーからコンテナ内容が同期された瞬間)に委譲する。
// タイトルは "(1/3) Loadouts" のようにページ番号付きで表示されるため、他の類似メニューへの
// 誤反応を避けるためページ番号表記込みで厳密にマッチさせる
@Mixin(AbstractContainerScreen.class)
public class LoadoutsClickMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Loadouts");

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        if (buttonNum != 0) return;
        if (slot == null || !slot.hasItem()) return;

        Screen screen = (Screen) (Object) this;
        if (!LOADOUTS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        PetHandler.processLoadoutsSlotClick(slot.getItem());
        EquipmentScanner.resetToUnknown();
    }
}
