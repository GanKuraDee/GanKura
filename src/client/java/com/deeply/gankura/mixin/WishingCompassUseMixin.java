package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.WishingCompassHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wishing Compass を使った瞬間と、そのときの立ち位置を拾う。
 *
 * 粒の飛ぶ向きだけでは目標までの距離が出ないので、
 * どこから使ったかが解読の起点になる
 */
@Mixin(MultiPlayerGameMode.class)
public class WishingCompassUseMixin {

    // 手に持ったままの右クリック
    @Inject(method = "useItem", at = @At("HEAD"))
    private void gankura$onUseItem(Player player, InteractionHand hand,
                                   CallbackInfoReturnable<InteractionResult> cir) {
        WishingCompassHandler.onUseItem(player.getItemInHand(hand), Minecraft.getInstance());
    }

    // ブロックを見ながらの右クリック。坑道の中ではこちらを通ることの方が多い
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void gankura$onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        WishingCompassHandler.onUseItem(player.getItemInHand(hand), Minecraft.getInstance());
    }
}
