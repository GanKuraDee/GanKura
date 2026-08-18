package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 視点の向き(Yaw / Pitch)。畑を往復するときの角度合わせに使う
public class YawPitchHud extends HudElement {

    public YawPitchHud() {
        super("yawPitch", 460, 160, 1.0f, 80, 20,
                () -> ModConfig.INSTANCE.misc.showYawPitchHud, () -> true);
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        float yaw = 0.0f;
        float pitch = 0.0f;
        if (client.player != null) {
            yaw = normalizedYaw(client.player);
            pitch = client.player.getPitch();
        }

        drawTextWithShadow(context, tr, "§bYaw: §f" + format(yaw, ModConfig.INSTANCE.misc.yawPrecision), 0, 0, 0xFFFFFFFF);
        drawTextWithShadow(context, tr, "§bPitch: §f" + format(pitch, ModConfig.INSTANCE.misc.pitchPrecision), 0, 10, 0xFFFFFFFF);
    }

    // 生の Yaw は回った回数ぶん際限なく増減するので、-180〜180 に畳んでおく
    private static float normalizedYaw(PlayerEntity player) {
        float yaw = player.getYaw() % 360.0f;
        if (yaw < 0.0f) yaw += 360.0f;
        if (yaw > 180.0f) yaw -= 360.0f;
        return yaw;
    }

    // 指定した桁で丸める。末尾の0は落として "45" や "45.5" のように短く出す
    private static String format(float value, int precision) {
        return BigDecimal.valueOf(value)
                .setScale(precision, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
