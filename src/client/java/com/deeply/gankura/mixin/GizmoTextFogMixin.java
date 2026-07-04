package com.deeply.gankura.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gizmo経由のワールド内テキスト(ボス座標ラベル等)はバニラだと
 * Font.DisplayMode.NORMAL で描画され、これはFOGバインドグループを持つ
 * パイプラインを使うため霧の色に文字色が引っ張られてしまう。
 * DisplayMode.SEE_THROUGH はFOGバインドグループを持たないため、
 * ここで描画モードを差し替えて霧の影響を受けないようにする。
 */
@Mixin(targets = "net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives$Group")
public class GizmoTextFogMixin {

    @ModifyArg(
            method = "renderTexts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
            )
    )
    private Font.DisplayMode gankura$disableFogForGizmoText(Font.DisplayMode mode) {
        return Font.DisplayMode.SEE_THROUGH;
    }
}
