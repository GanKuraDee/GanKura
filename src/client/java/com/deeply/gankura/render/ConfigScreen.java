package com.deeply.gankura.render;

import com.deeply.gankura.data.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private enum MenuType {
        MAIN,
        THE_END,
        MISC
    }

    private static MenuType currentMenu = MenuType.MAIN;

    private final String subtitleText;

    public ConfigScreen() {
        super(Text.literal("GanKura Configuration"));
        String version = FabricLoader.getInstance()
                .getModContainer("gankura")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
        this.subtitleText = "Release: " + version;
    }

    @Override
    protected void init() {
        this.clearChildren();

        int centerX = this.width / 2;
        int y = 60;

        switch (currentMenu) {
            case MAIN -> initMainMenu(centerX, y);
            case THE_END -> initTheEndMenu(centerX, y);
            case MISC -> initMiscMenu(centerX, y);
        }
    }

    private void initMainMenu(int centerX, int y) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD Locations"), button -> {
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
        }).dimensions(centerX - 100, 45, 200, 20).build());

        int buttonStartY = 90;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("The End"), button -> {
            currentMenu = MenuType.THE_END;
            this.clearAndInit();
        }).dimensions(centerX - 100, buttonStartY, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Misc"), button -> {
            currentMenu = MenuType.MISC;
            this.clearAndInit();
        }).dimensions(centerX - 100, buttonStartY + 24, 200, 20).build());
    }

    private void initTheEndMenu(int centerX, int y) {
        int left = centerX - 155;
        int right = centerX + 5;
        int bWidth = 150;

        // --- Golem Settings ---
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Status HUD", ModConfig.showGolemStatusHud, b -> ModConfig.showGolemStatusHud = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Loot Tracker HUD", ModConfig.showLootTrackerHud, b -> ModConfig.showLootTrackerHud = b));
        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "HP HUD", ModConfig.showGolemHealthHud, b -> ModConfig.showGolemHealthHud = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "World Location Display", ModConfig.showGolemWorldText, b -> ModConfig.showGolemWorldText = b));
        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Stage 4 & 5 Alert Title", ModConfig.enableStageAlerts, b -> ModConfig.enableStageAlerts = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Rare Drop Notification", ModConfig.enableDropAlerts, b -> ModConfig.enableDropAlerts = b));
        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Stage 4 Duration Chat", ModConfig.showStage4Duration, b -> ModConfig.showStage4Duration = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "DPS Chat", ModConfig.showDpsChat, b -> ModConfig.showDpsChat = b));
        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Loot Quality Chat", ModConfig.showLootQualityChat, b -> ModConfig.showLootQualityChat = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Day 30 Alert", ModConfig.enableDay30Alert, b -> ModConfig.enableDay30Alert = b));

        y += 40;

        // --- Dragon Settings ---
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Status HUD", ModConfig.showDragonStatusHud, b -> ModConfig.showDragonStatusHud = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Loot Tracker HUD", ModConfig.showDragonTrackerHud, b -> ModConfig.showDragonTrackerHud = b));

        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Spawn Alert Title", ModConfig.enableDragonAlerts, b -> ModConfig.enableDragonAlerts = b));
        // ★追加: Spawn Alert の右側に Rare Drop Alert を配置
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Rare Drop Notification", ModConfig.enableDragonDropAlerts, b -> ModConfig.enableDragonDropAlerts = b));

        y += 22;
        // ★変更: 残りの項目を下にずらして配置
        this.addDrawableChild(createToggleButton(left, y, bWidth, "DPS Chat", ModConfig.showDragonDpsChat, b -> ModConfig.showDragonDpsChat = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Loot Quality Chat", ModConfig.showDragonLootQualityChat, b -> ModConfig.showDragonLootQualityChat = b));
    }

    private void initMiscMenu(int centerX, int y) {
        int left = centerX - 155;
        int right = centerX + 5;
        int bWidth = 150;

        this.addDrawableChild(createToggleButton(left, y, bWidth, "Active Pet HUD", ModConfig.showPetHud, b -> ModConfig.showPetHud = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Armor Stack HUD", ModConfig.showArmorStackHud, b -> ModConfig.showArmorStackHud = b));
        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Server Reboot Alert", ModConfig.enableRebootAlert, b -> ModConfig.enableRebootAlert = b));
        this.addDrawableChild(createToggleButton(right, y, bWidth, "Day HUD", ModConfig.showDayHud, b -> ModConfig.showDayHud = b));

        y += 22;
        this.addDrawableChild(createToggleButton(left, y, bWidth, "Arrow Poison Indicator", ModConfig.showPoisonIndicator, b -> ModConfig.showPoisonIndicator = b));
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == 256) {
            if (currentMenu != MenuType.MAIN) {
                currentMenu = MenuType.MAIN;
                this.clearAndInit();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private ButtonWidget createToggleButton(int x, int y, int width, String label, boolean currentValue, BooleanConsumer onToggle) {
        String stateText = currentValue ? "§aON" : "§cOFF";
        return ButtonWidget.builder(Text.literal(label + ": " + stateText), button -> {
            boolean newState = !button.getMessage().getString().contains("ON");
            onToggle.accept(newState);
            ModConfig.save();
            button.setMessage(Text.literal(label + ": " + (newState ? "§aON" : "§cOFF")));
        }).dimensions(x, y, width, 20).build();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        int centerX = this.width / 2;

        context.getMatrices().pushMatrix();
        float scale = 2.0f;
        context.getMatrices().scale(scale, scale);
        context.drawCenteredTextWithShadow(textRenderer, "GanKura", (int)(centerX / scale), 5, 0xFFFFFFFF);
        context.getMatrices().popMatrix();

        context.drawCenteredTextWithShadow(textRenderer, this.subtitleText, centerX, 28, 0xFFAAAAAA);

        if (currentMenu == MenuType.MAIN) {
            context.drawCenteredTextWithShadow(textRenderer, "§lCategory Selection", centerX, 75, 0xFF55FFFF);
        } else if (currentMenu == MenuType.THE_END) {
            context.drawCenteredTextWithShadow(textRenderer, "§6§lEnd Stone Protector", centerX, 45, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, "§d§lDragon", centerX, 174, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        currentMenu = MenuType.MAIN;
        super.close();
    }

    @FunctionalInterface interface BooleanConsumer { void accept(boolean b); }
}