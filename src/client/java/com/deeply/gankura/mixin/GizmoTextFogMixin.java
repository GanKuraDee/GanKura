package com.deeply.gankura.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gizmo経由のワールド内テキスト(ボス座標ラベル等)はバニラだと
 * Font.DisplayMode.NORMAL 用のレンダーパイプライン(WORLD_TEXT_SNIPPET)で描画され、
 * これにはFOGバインドグループが含まれるため霧の色に文字色が引っ張られてしまう。
 * DisplayMode.SEE_THROUGH 用のパイプラインはFOGバインドグループを持たないため、
 * ここで描画モードを差し替えて霧の影響を受けないようにする。
 */
@Mixin(targets = "net.minecraft.client.renderer.feature.GizmoFeatureRenderer$1")
public class GizmoTextFogMixin {

    @ModifyArg(
            method = "acceptRenderable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextRenderable;renderType(Lnet/minecraft/client/gui/Font$DisplayMode;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private Font.DisplayMode gankura$disableFogForGizmoText(Font.DisplayMode mode) {
        return Font.DisplayMode.SEE_THROUGH;
    }
}
