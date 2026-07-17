package com.deeply.gankura.mixin.client;

import com.deeply.gankura.scanner.EquipmentScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

// "Loadouts" メニューはロードアウト切り替え後、サーバーからコンテナ内容の同期パケット
// (updateSlotStacks/setStackInSlot)が届くまでEquipmentプレビューの内容が更新されない。
// クリックした瞬間に読み取ると同期前の古い内容を読んでしまうため、実際にサーバーから
// 内容が届いたこのタイミングでEquipment Hudを再スキャンする。
// タイトルは "(1/3) Loadouts" のようにページ番号付きで表示されるため、他の類似メニューへの
// 誤反応を避けるためページ番号表記込みで厳密にマッチさせる
@Mixin(ScreenHandler.class)
public class LoadoutsContentSyncMixin {
    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Loadouts");

    @Inject(method = "updateSlotStacks", at = @At("TAIL"))
    private void onUpdateSlotStacks(int revision, List<ItemStack> stacks, ItemStack cursorStack, CallbackInfo ci) {
        onContentsChanged();
    }

    @Inject(method = "setStackInSlot", at = @At("TAIL"))
    private void onSetStackInSlot(int slot, int revision, ItemStack stack, CallbackInfo ci) {
        onContentsChanged();
    }

    private void onContentsChanged() {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (!(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> screen)) return;
        if (screen.getScreenHandler() != handler) return;
        if (!LOADOUTS_TITLE_PATTERN.matcher(screen.getTitle().getString()).find()) return;

        EquipmentScanner.onLoadoutsContentsSynced(handler);
    }
}
