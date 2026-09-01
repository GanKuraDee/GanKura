package com.deeply.gankura.handler;

import com.deeply.gankura.data.ButtonClickType;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.InventoryButton;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.gui.InventoryButtonIcons;
import com.deeply.gankura.gui.InventoryButtonTexture;
import com.deeply.gankura.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 収納画面の周りにボタンを出し、押されたらコマンドを送る。
 *
 * NotEnoughUpdates の Inventory Buttons を移植したもの。
 * ボタンの置き場所はエディタ({@link com.deeply.gankura.gui.InventoryButtonEditorScreen})で決める
 */
public final class InventoryButtonHandler {

    // 描かれていない枠は薄く見せる
    private static final int OPAQUE = 0xFFFFFFFF;

    // ダンジョンの仕掛けメニュー。ボタンが仕掛けの上に重なると邪魔になる
    private static final Set<String> DUNGEON_MENUS = Set.of(
            "Spirit Leap",
            "Revive A Teammate",
            "Click in order!",
            "Click the button on time!",
            "Correct all the panes!",
            "Change all to same color!");

    private static final List<String> DUNGEON_MENU_PREFIXES = List.of(
            "What starts with",
            "Select all the");

    // 今カーソルが乗っているボタンと、乗り始めた時刻。説明を出すまでの間を測るために持つ
    private static InventoryButton hoveredButton = null;
    private static long hoveredSince = 0;

    private InventoryButtonHandler() {
    }

    // 画面に置いたボタン1つ分。位置は画面の大きさで変わるので、その都度計算する
    public record Placement(InventoryButton button, int x, int y) {

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + InventoryButtonTexture.BUTTON_SIZE
                    && mouseY >= y && mouseY <= y + InventoryButtonTexture.BUTTON_SIZE;
        }
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;

            // 描画は InventoryButtonRenderMixin が受け持つ。
            // 画面イベントはアイテムの説明を描き終えた後に呼ばれるので、
            // ここで描くとボタンが説明の上に出てしまい、こちらの説明も間に合わない
            ScreenMouseEvents.allowMouseClick(screen).register(
                    (ignored, event) -> allowMouse(container, event, ButtonClickType.MOUSE_DOWN));
            ScreenMouseEvents.allowMouseRelease(screen).register(
                    (ignored, event) -> allowMouse(container, event, ButtonClickType.MOUSE_UP));
        });
    }

    // -------------------------------------------------- 描画

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        InventoryButton hovered = null;

        for (Placement placement : placements(screen)) {
            InventoryButtonTexture.drawButton(graphics, placement.button().backgroundIndex,
                    placement.x(), placement.y(), OPAQUE);
            InventoryButtonIcons.render(graphics, placement.button().iconOrEmpty(),
                    placement.x() + 1, placement.y() + 1);

            if (placement.contains(mouseX, mouseY)) hovered = placement.button();
        }

        if (hovered == null) {
            hoveredButton = null;
            return;
        }

        // 乗せた直後に説明が出ると目障りなので、少し待ってから出す
        long now = System.currentTimeMillis();
        if (hoveredButton != hovered) {
            hoveredButton = hovered;
            hoveredSince = now;
        }
        if (now - hoveredSince <= config().tooltipDelay) return;

        // ボタンはスロットの上にも置けるので、下のスロットの説明を押しのけて自分のものを出す
        Component text = Component.literal(commandWithSlash(hovered)).withStyle(ChatFormatting.GRAY);
        graphics.setTooltipForNextFrame(client.font, List.of(text.getVisualOrderText()),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
    }

    // -------------------------------------------------- 操作

    private static boolean allowMouse(AbstractContainerScreen<?> screen, MouseButtonEvent event, ButtonClickType when) {
        Placement placement = findAt(screen, event.x(), event.y());
        if (placement == null) return true;

        // アイテムを掴んだままボタンを押しても、コマンドは送らずに掴んだままにする。
        // ボタンはスロットの上にも置けるので、下のスロットにも届かないよう止めておく
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !screen.getMenu().getCarried().isEmpty()) return false;

        // 押した/離したの片方だけを使う。もう片方は素通りさせて、
        // スロットのドラッグなど本来の操作を邪魔しないようにする
        if (config().clickType != when) return true;

        runCommand(placement.button());
        return false;
    }

    private static void runCommand(InventoryButton button) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String command = button.commandOrEmpty().trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isEmpty()) return;

        client.player.connection.sendCommand(command);
    }

    // -------------------------------------------------- 位置決め

    /**
     * 今の画面に出すボタンと、その置き場所。
     *
     * 出さない場面(SkyBlock以外・機能を切っている・ダンジョンの仕掛け)では空になる
     */
    public static List<Placement> placements(AbstractContainerScreen<?> screen) {
        List<Placement> placements = new ArrayList<>();
        forEachPlacement(screen, placements::add);
        return placements;
    }

    private static void forEachPlacement(AbstractContainerScreen<?> screen, Consumer<Placement> consumer) {
        ModConfig.InventoryButtonsCategory config = config();
        if (!config.enableInventoryButtons) return;
        if (!GameState.Server.isSkyblock()) return;
        if (config.hideInDungeonMenus && isDungeonMenu(screen)) return;

        ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
        int left = accessor.gankura$getLeftPos();
        int top = accessor.gankura$getTopPos();
        int width = accessor.gankura$getImageWidth();
        int height = accessor.gankura$getImageHeight();

        for (InventoryButton button : config.buttons) {
            if (button == null || !button.isActive()) continue;
            if (button.playerInvOnly && !(screen instanceof InventoryScreen)) continue;

            consumer.accept(new Placement(button,
                    left + button.x + (button.anchorRight ? width : 0),
                    top + button.y + (button.anchorBottom ? height : 0)));
        }
    }

    private static Placement findAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (Placement placement : placements(screen)) {
            if (placement.contains(mouseX, mouseY)) return placement;
        }
        return null;
    }

    // ダンジョンの仕掛けメニューか。名前がそのまま画面の題になっている
    private static boolean isDungeonMenu(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        if (DUNGEON_MENUS.contains(title)) return true;

        for (String prefix : DUNGEON_MENU_PREFIXES) {
            if (title.startsWith(prefix)) return true;
        }
        return false;
    }

    // 説明に出すコマンド。打ち込む形と揃えて先頭に / を付ける
    private static String commandWithSlash(InventoryButton button) {
        String command = button.commandOrEmpty().trim();
        return command.startsWith("/") ? command : "/" + command;
    }

    private static ModConfig.InventoryButtonsCategory config() {
        return ModConfig.INSTANCE.inventoryButtons;
    }
}
