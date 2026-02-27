package com.deeply.gankura.render;

import com.deeply.gankura.data.ModConfig;
import net.fabricmc.loader.api.FabricLoader; // ★追加: バージョン取得用
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    // カテゴリのテキストを描画するためのY座標を保持する変数
    private int golemTextY;
    private int dragonTextY;

    // ★追加: サブタイトル(バージョン情報)を保持する変数
    private final String subtitleText;

    public ConfigScreen() {
        super(Text.literal("GanKura Configuration"));

        // ★追加: FabricLoaderを使って現在のModバージョンを取得する
        // ※ "gankura" の部分は、fabric.mod.json 内の "id" (Mod ID) と同じものを指定してください
        String version = FabricLoader.getInstance()
                .getModContainer("gankura")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");

        this.subtitleText = "Release: " + version;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int btnWidth = 150;
        int leftCol = centerX - 155;
        int rightCol = centerX + 5;

        int y = 45;

        // 1. HUD Location Editor
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD Locations"), button -> {
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
        }).dimensions(centerX - 100, y, 200, 20).build());

        y += 28; // ボタンの高さ(20) + 少し広めの隙間(8)

        // ==========================================
        // --- Golem カテゴリ ---
        // ==========================================
        this.golemTextY = y;
        y += 12; // テキスト用のスペースを空ける

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "Status HUD",
                ModConfig.showGolemStatusHud, b -> ModConfig.showGolemStatusHud = b));
        this.addDrawableChild(createToggleButton(rightCol, y, btnWidth, "Loot Tracker HUD",
                ModConfig.showLootTrackerHud, b -> ModConfig.showLootTrackerHud = b));
        y += 22; // ボタンの高さ(20) + 最小限の隙間(2) にして高さを節約

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "HP HUD",
                ModConfig.showGolemHealthHud, b -> ModConfig.showGolemHealthHud = b));
        this.addDrawableChild(createToggleButton(rightCol, y, btnWidth, "World Location Text",
                ModConfig.showGolemWorldText, b -> ModConfig.showGolemWorldText = b));
        y += 22;

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "Stage 4 & 5 Alert Title",
                ModConfig.enableStageAlerts, b -> ModConfig.enableStageAlerts = b));
        this.addDrawableChild(createToggleButton(rightCol, y, btnWidth, "Rare Drop Notification",
                ModConfig.enableDropAlerts, b -> ModConfig.enableDropAlerts = b));
        y += 22;

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "Stage 4 Duration Chat",
                ModConfig.showStage4Duration, b -> ModConfig.showStage4Duration = b));
        this.addDrawableChild(createToggleButton(rightCol, y, btnWidth, "DPS Chat",
                ModConfig.showDpsChat, b -> ModConfig.showDpsChat = b));
        y += 22;

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "Loot Quality Chat",
                ModConfig.showLootQualityChat, b -> ModConfig.showLootQualityChat = b));

        y += 28; // 次のカテゴリとの間は少し広めに空ける

        // ==========================================
        // --- Dragon カテゴリ ---
        // ==========================================
        this.dragonTextY = y;
        y += 12;

        this.addDrawableChild(createToggleButton(leftCol, y, btnWidth, "Type Alert Title",
                ModConfig.enableDragonAlerts, b -> ModConfig.enableDragonAlerts = b));
    }

    private ButtonWidget createToggleButton(int x, int y, int width, String label, boolean currentValue, BooleanConsumer onToggle) {
        String stateText = currentValue ? "§aON" : "§cOFF";
        Text buttonText = Text.literal(label + ": " + stateText);

        return ButtonWidget.builder(buttonText, button -> {
            boolean newState = !getToggleState(button.getMessage().getString());
            onToggle.accept(newState);
            ModConfig.save();

            String newStateText = newState ? "§aON" : "§cOFF";
            button.setMessage(Text.literal(label + ": " + newStateText));
        }).dimensions(x, y, width, 20).build();
    }

    private boolean getToggleState(String text) {
        return text.contains("ON");
    }

    @FunctionalInterface
    interface BooleanConsumer {
        void accept(boolean b);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        int centerX = this.width / 2;

        // タイトルとサブタイトルも上に詰める (Y: 10->5, 45->28)
        context.getMatrices().pushMatrix();
        float scale = 2.0f;
        context.getMatrices().scale(scale, scale);
        context.drawCenteredTextWithShadow(textRenderer, "GanKura", (int)(centerX / scale), 5, 0xFFFFFFFF);
        context.getMatrices().popMatrix();

        // ★変更: 固定の文章から、取得したバージョン情報(subtitleText)の描画に変更
        context.drawCenteredTextWithShadow(textRenderer, this.subtitleText, centerX, 28, 0xFFAAAAAA);

        // ==========================================
        // カテゴリタイトルの描画
        // ==========================================
        context.drawTextWithShadow(textRenderer, "§lEnd Stone Protector", centerX - 155, this.golemTextY, 0xFFFFAA00);
        context.drawTextWithShadow(textRenderer, "§lDragon", centerX - 155, this.dragonTextY, 0xFFFF5555);

        super.render(context, mouseX, mouseY, delta);
    }
}