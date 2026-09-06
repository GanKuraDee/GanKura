package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.AbilityMenuHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 持ち替えをサーバーに伝える頃合いを、能力で開くメニューに合わせてずらす。
 *
 * 見送るのは tick から出る分だけにしてある。
 * 採掘・攻撃・右クリックはどれも先に ensureHasSentCarriedItem を呼ぶので、
 * 何か動いた時点で持ち替えは伝わり、ずれたままにはならない
 */
@Mixin(MultiPlayerGameMode.class)
public class HeldSlotSyncMixin {

    @Shadow
    private void ensureHasSentCarriedItem() {
        throw new AssertionError();
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V"
            )
    )
    private void gankura$holdSlotChange(MultiPlayerGameMode instance) {
        if (AbilityMenuHandler.shouldHoldSlotChange()) return;

        ensureHasSentCarriedItem();
    }

    // 品を持ったままの右クリック。ここで伝え終わってから、開くまでの間を数え始める
    @Inject(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V",
                    shift = At.Shift.AFTER
            )
    )
    private void gankura$onUseItem(Player player, InteractionHand hand,
                                   CallbackInfoReturnable<InteractionResult> cir) {
        AbilityMenuHandler.onUseItem(player.getItemInHand(hand));
    }

    // ブロックを見ている間の右クリックはこちらを通る
    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V",
                    shift = At.Shift.AFTER
            )
    )
    private void gankura$onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        AbilityMenuHandler.onUseItem(player.getItemInHand(hand));
    }
}
