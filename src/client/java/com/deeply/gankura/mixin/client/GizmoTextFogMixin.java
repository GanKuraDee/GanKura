package com.deeply.gankura.mixin.client;

import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gizmo経由のワールド内テキスト(ボス座標ラベル等)はバニラだと
 * TextRenderer.TextLayerType.NORMAL で描画され、これはFOGスニペットを持つ
 * パイプライン(RENDERTYPE_TEXT)を使うため霧の色に文字色が引っ張られてしまう。
 * TextLayerType.SEE_THROUGH 用のパイプラインはFOGスニペットを持たないため、
 * ここで描画モードを差し替えて霧の影響を受けないようにする。
 */
@Mixin(targets = "net.minecraft.client.render.gizmo.GizmoDrawerImpl$Division")
public class GizmoTextFogMixin {

    @ModifyArg(
            method = "drawText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"
            )
    )
    private TextRenderer.TextLayerType gankura$disableFogForGizmoText(TextRenderer.TextLayerType mode) {
        return TextRenderer.TextLayerType.SEE_THROUGH;
    }
}
