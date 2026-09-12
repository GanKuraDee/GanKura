package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 右クリックの間だけ、アーマースタンドを照準から外す。
 *
 * Hypixel はホログラムやダメージ表示を透明なアーマースタンドで作っていて、
 * それが目の前にあると照準がそちらを掴む。
 * するとバニラは右クリックを「アーマースタンドへの操作」として扱うので、
 * 釣り竿を投げる・アビリティを出すはずの右クリックが吸われ、
 * 向こうにあるチェストやブロックにも手が届かない。
 *
 * そこで右クリックを処理する間だけ、照準をブロック側へ引き直す。
 * 元に戻すので、殴る相手や十字カーソルの見え方は変わらない
 */
@Mixin(Minecraft.class)
public class ArmorStandInteractMixin {

    // 差し替える前の照準。処理が終わったら戻す
    @Unique
    private HitResult gankura$heldHitResult;

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void gankura$dropArmorStand(CallbackInfo ci) {
        gankura$heldHitResult = null;

        if (!ModConfig.INSTANCE.misc.ignoreArmorStandClicks) return;
        if (!GameState.Server.isSkyblock()) return;

        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;
        if (player == null) return;
        if (!(client.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof ArmorStand)) return;

        gankura$heldHitResult = client.hitResult;
        // 液体は素通しにする。バニラがブロックを掴むときと同じ引き方
        client.hitResult = player.pick(player.blockInteractionRange(), 1.0f, false);
    }

    // startUseItem は途中でも抜けるので、どの出口でも必ず戻す
    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void gankura$restoreHitResult(CallbackInfo ci) {
        if (gankura$heldHitResult == null) return;

        ((Minecraft) (Object) this).hitResult = gankura$heldHitResult;
        gankura$heldHitResult = null;
    }
}
