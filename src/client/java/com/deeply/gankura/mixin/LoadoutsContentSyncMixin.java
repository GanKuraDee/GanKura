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
import java.util.regex.Pattern;

// "Loadouts" メニューはロードアウト切り替え後、サーバーからコンテナ内容の同期パケット
// (initializeContents/setItem)が届くまでEquipmentプレビューの内容が更新されない。
// クリックした瞬間に読み取ると同期前の古い内容を読んでしまうため、実際にサーバーから
// 内容が届いたこのタイミングでEquipment Hudを再スキャンする。
// タイトルは "(1/3) Loadouts" のようにページ番号付きで表示されるため、他の類似メニューへの
// 誤反応を避けるためページ番号表記込みで厳密にマッチさせる
@Mixin(AbstractContainerMenu.class)
public class LoadoutsContentSyncMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Loadouts");

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
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen.getMenu() != menu) return;
        if (!LOADOUTS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        EquipmentScanner.onLoadoutsContentsSynced(menu);
    }
}
