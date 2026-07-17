package com.deeply.gankura.mixin;

import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// "Loadouts" メニューはロードアウト切り替え後、サーバーからコンテナ内容の同期パケット
// (initializeContents/setItem)が届くまでEquipmentプレビューの内容が更新されない。
// クリックした瞬間に読み取ると同期前の古い内容を読んでしまうため、実際にサーバーから
// 内容が届いたこのタイミングでEquipment Hudを再スキャンする。
@Mixin(AbstractContainerMenu.class)
public class LoadoutsContentSyncMixin {
    private static final String LOADOUTS_TITLE = "Loadouts";

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void onInitializeContents(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        onContentsChanged();
    }

    @Inject(method = "setItem", at = @At("TAIL"))
    private void onSetItem(int slot, int stateId, ItemStack itemStack, CallbackInfo ci) {
        onContentsChanged();
    }

    private void onContentsChanged() {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return;
        if (screen.getMenu() != menu) return;
        if (!screen.getTitle().getString().contains(LOADOUTS_TITLE)) return;

        EquipmentScanner.onLoadoutsContentsSynced(menu);
    }
}
