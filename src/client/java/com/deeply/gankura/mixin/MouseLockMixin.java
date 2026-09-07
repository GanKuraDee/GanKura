package com.deeply.gankura.mixin;

import com.deeply.gankura.handler.MousematHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 視点を固定している間、マウスの動きを視点に渡さない。
 *
 * 溜まっている移動量は turnPlayer を呼んだ側が呼び終わった直後に捨てるので、
 * ここで打ち切っても解いた瞬間に溜まった分だけ視点が飛ぶことはない。
 * 固定するかどうかは {@link MousematHandler} が決める
 */
@Mixin(MouseHandler.class)
public class MouseLockMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void gankura$holdView(double partialTick, CallbackInfo ci) {
        if (MousematHandler.isLocked()) ci.cancel();
    }
}
