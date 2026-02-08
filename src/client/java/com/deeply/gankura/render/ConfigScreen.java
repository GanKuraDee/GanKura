package com.deeply.gankura.render;

import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    public ConfigScreen() {
        super(Text.literal("GanKura Configuration"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 85;
        int spacing = 24;

        // 1. HUD Location Editor
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD Locations"), button -> {
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
        }).dimensions(centerX - 100, y, 200, 20).build());
        y += spacing;

        // 2. Golem Status HUD
        this.addDrawableChild(createToggleButton(centerX, y, "Golem Status HUD",
                ModConfig.showGolemStatusHud, b -> ModConfig.showGolemStatusHud = b));
        y += spacing;

        // 3. Loot Tracker HUD
        this.addDrawableChild(createToggleButton(centerX, y, "Loot Tracker HUD",
                ModConfig.showLootTrackerHud, b -> ModConfig.showLootTrackerHud = b));
        y += spacing;

        // 4. Stage Alerts (Title/Sound)
        this.addDrawableChild(createToggleButton(centerX, y, "Stage Alerts (Title & Sound)",
                ModConfig.enableStageAlerts, b -> ModConfig.enableStageAlerts = b));
        y += spacing;

        // 5. Drop Alerts (Title/Sound)
        this.addDrawableChild(createToggleButton(centerX, y, "Rare Drop Alerts",
                ModConfig.enableDropAlerts, b -> ModConfig.enableDropAlerts = b));
        y += spacing;

        // 6. Stage 4 Duration Chat
        this.addDrawableChild(createToggleButton(centerX, y, "Stage 4 Duration Chat",
                ModConfig.showStage4Duration, b -> ModConfig.showStage4Duration = b));
        y += spacing;

        // 7. DPS Chat
        this.addDrawableChild(createToggleButton(centerX, y, "Golem DPS Chat",
                ModConfig.showDpsChat, b -> ModConfig.showDpsChat = b));
        y += spacing;

        // 8. Loot Quality Chat
        this.addDrawableChild(createToggleButton(centerX, y, "Loot Quality Chat",
                ModConfig.showLootQualityChat, b -> ModConfig.showLootQualityChat = b));
    }

    private ButtonWidget createToggleButton(int centerX, int y, String label, boolean currentValue, BooleanConsumer onToggle) {
        String stateText = currentValue ? "§aEnable" : "§cDisable";
        Text buttonText = Text.literal(label + ": " + stateText);

        return ButtonWidget.builder(buttonText, button -> {
            boolean newState = !getToggleState(button.getMessage().getString());
            onToggle.accept(newState);
            ModConfig.save();

            String newStateText = newState ? "§aEnable" : "§cDisable";
            button.setMessage(Text.literal(label + ": " + newStateText));
        }).dimensions(centerX - 100, y, 200, 20).build();
    }

    private boolean getToggleState(String text) {
        return text.contains("Enable");
    }

    @FunctionalInterface
    interface BooleanConsumer {
        void accept(boolean b);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // タイトルと説明文の描画
        int centerX = this.width / 2;

        // 1. "GanKura" (白・大・中央)
        context.getMatrices().pushMatrix();

        float scale = 2.0f;
        context.getMatrices().scale(scale, scale);

        // ★修正: 色コードを 0xFFFFFF -> 0xFFFFFFFF (アルファ値FFを追加)
        context.drawCenteredTextWithShadow(textRenderer, "GanKura", (int)(centerX / scale), 10, 0xFFFFFFFF);

        context.getMatrices().popMatrix();

        // 2. 説明文 (灰・小・中央)
        // ★修正: 色コードを 0xAAAAAA -> 0xFFAAAAAA (アルファ値FFを追加)
        context.drawCenteredTextWithShadow(textRenderer, "A Hypixel Skyblock Mod focused on End Stone Protector.", centerX, 50, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }
}