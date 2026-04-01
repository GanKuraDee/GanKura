package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class BroodmotherStatusHud extends HudElement {
    public BroodmotherStatusHud() {
        super("broodmother", 10, 50, 1.0f, 150, 50,
                () -> ModConfig.INSTANCE.broodmother.showBroodmotherStatusHud,
                () -> ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map));
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String displayStats; String timerText = null;

        if (isPreview) {
            // ★修正: プレビュー表示も実際の動作に合わせて「4」を黄色(§e)にしました
            displayStats = "Stage: §e4"; timerText = "Since S4: §e0m 45s §7(Max 1m)";
        } else {
            String stage = GameState.Broodmother.stage;

            if ("Alive!".equals(stage)) {
                displayStats = "§cStage: 5 (Spawned)";
            }
            else if ("Scanning...".equals(stage)) {
                displayStats = "Stage: §8Scanning...";
            }
            else {
                // ★修正: Stage 4 (Imminent) の「4」を黄色(§e)にするように色分けを追加
                String num = switch (stage) {
                    case "Slain" -> "§f0";
                    case "Dormant" -> "§f1";
                    case "Soon" -> "§f2";
                    case "Awakening" -> "§f3";
                    case "Imminent" -> "§e4"; // 黄色
                    default -> "§f?";
                };
                displayStats = "Stage: " + num;

                if ("Imminent".equals(stage) && GameState.Broodmother.stage4StartTime > 0) {
                    long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;
                    String colorCode = seconds >= 45 ? "§c" : (seconds >= 30 ? "§e" : "§f");
                    timerText = String.format("Since S4: %s%dm %ds §7(Max 1m)", colorCode, seconds / 60, seconds % 60);
                }
            }
        }

        context.drawTextWithShadow(tr, "§4§lBroodmother Status", 0, 0, 0xFFFFFFFF);
        context.drawTextWithShadow(tr, displayStats, 0, 12, 0xFFFFFFFF);
        // ★修正: タイマーテキストが存在し、かつ設定スイッチがONの時だけ描画する
        if (timerText != null && ModConfig.INSTANCE.broodmother.showBroodmotherStatusHud_SinceS4) {
            context.drawTextWithShadow(tr, timerText, 0, 24, 0xFFFFFFFF);
        }
    }
}