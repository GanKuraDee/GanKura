package com.deeply.gankura.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Inventory Button の絵柄。
 *
 * 1枚の画像に、枠・パネル・矢印がまとめて入っている。
 * 切り出す位置は NotEnoughUpdates のものと同じにしてあるので、画像もそのまま使える
 */
public final class InventoryButtonTexture {

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("gankura", "textures/invbuttons/editor.png");
    public static final Identifier VANILLA_INVENTORY =
            Identifier.withDefaultNamespace("textures/gui/container/inventory.png");

    // ボタン1つの大きさ
    public static final int BUTTON_SIZE = 18;
    // 枠の絵柄の数
    public static final int BACKGROUND_TYPES = 5;

    // 編集パネルの大きさ
    public static final int PANEL_WIDTH = 150;
    public static final int PANEL_HEIGHT = 204;
    // プリセット一覧の大きさ
    public static final int PRESET_PANEL_WIDTH = 80;

    // 疑似的に描くプレイヤーの持ち物画面の大きさ
    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 166;

    private static final int TEXTURE_SIZE = 256;
    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    // 画像の中での縦位置
    private static final int SWATCH_V = 0;
    private static final int BUTTON_V = 18;
    private static final int PANEL_V = 41;
    // 矢印は 6x5 の絵を 10x5 に伸ばして使う
    private static final int ARROW_U = 0;
    private static final int ARROW_V = 36;
    private static final int ARROW_SOURCE_WIDTH = 6;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 5;
    // アイコン置き場の空枠は、枠の絵柄1番と同じ場所にある
    private static final int SLOT_U = 18;

    private InventoryButtonTexture() {
    }

    // 実際に置かれるボタンの枠
    public static void drawButton(GuiGraphicsExtractor graphics, int backgroundIndex, int x, int y, int color) {
        int index = Math.clamp(backgroundIndex, 0, BACKGROUND_TYPES - 1);
        graphics.blit(PIPELINE, TEXTURE, x, y, index * BUTTON_SIZE, BUTTON_V,
                BUTTON_SIZE, BUTTON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, color);
    }

    // 枠の絵柄を選ぶときの見本
    public static void drawSwatch(GuiGraphicsExtractor graphics, int backgroundIndex, int x, int y) {
        graphics.blit(PIPELINE, TEXTURE, x, y, backgroundIndex * BUTTON_SIZE, SWATCH_V,
                BUTTON_SIZE, BUTTON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // アイコンを並べるときの空枠
    public static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(PIPELINE, TEXTURE, x, y, SLOT_U, SWATCH_V,
                BUTTON_SIZE, BUTTON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(PIPELINE, TEXTURE, x, y, 0, PANEL_V,
                PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public static void drawPresetPanel(GuiGraphicsExtractor graphics, int x, int y, int height) {
        graphics.blit(PIPELINE, TEXTURE, x, y, PANEL_WIDTH, PANEL_V,
                PRESET_PANEL_WIDTH, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // 編集パネルが、どのボタンのものかを示す小さな三角
    public static void drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(PIPELINE, TEXTURE, x, y, ARROW_U, ARROW_V,
                ARROW_WIDTH, ARROW_HEIGHT, ARROW_SOURCE_WIDTH, ARROW_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // エディタの土台にするプレイヤーの持ち物画面
    public static void drawInventory(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(PIPELINE, VANILLA_INVENTORY, x, y, 0.0F, 0.0F,
                INVENTORY_WIDTH, INVENTORY_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
