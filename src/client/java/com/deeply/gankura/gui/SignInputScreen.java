package com.deeply.gankura.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

/**
 * Hypixel が看板で受け取る入力を、普通の入力欄で受け取る。
 *
 * 看板は Enter で確定できず、長い名前も入りきらない。
 * ここでは Enter で確定でき、幅も自由に取れる。
 * 送る内容は看板と同じなので、サーバー側は違いに気付かない
 */
public class SignInputScreen extends Screen {

    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    // 看板1行に入る上限。これ以上はサーバーが切り捨てる
    private static final int MAX_LENGTH = 384;

    private static final int PROMPT_COLOR = 0xFFA0A0A0;

    // 書き換える看板
    private final BlockPos pos;
    private final boolean frontText;
    // 看板に書かれていた案内。何を入れる欄なのかを見せる
    private final Component prompt;

    private EditBox input;
    // 閉じるときに一度だけ送る
    private boolean sent;

    public SignInputScreen(BlockPos pos, boolean frontText, Component prompt) {
        super(Component.literal("Search"));
        this.pos = pos;
        this.frontText = frontText;
        this.prompt = prompt;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        input = new EditBox(font, centerX - FIELD_WIDTH / 2, centerY - FIELD_HEIGHT / 2,
                FIELD_WIDTH, FIELD_HEIGHT, prompt);
        input.setMaxLength(MAX_LENGTH);
        addRenderableWidget(input);
        setInitialFocus(input);

        addRenderableWidget(Button.builder(Component.literal("Search"), button -> confirm())
                .bounds(centerX - BUTTON_WIDTH / 2, centerY + FIELD_HEIGHT, BUTTON_WIDTH, FIELD_HEIGHT)
                .build());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Enter で確定。看板では出来なかった操作
        if (event.isConfirmation()) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    private void confirm() {
        send(input.getValue());
        onClose();
    }

    // 看板と同じ形で送る。1行目だけが読まれる
    private void send(String text) {
        if (sent || minecraft == null || minecraft.getConnection() == null) return;

        sent = true;
        minecraft.getConnection().send(
                new ServerboundSignUpdatePacket(pos, frontText, text, "", "", ""));
    }

    @Override
    public void removed() {
        // 確定せずに閉じたときも、看板を閉じたときと同じ扱いにしておく
        send("");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, prompt, width / 2, height / 2 - FIELD_HEIGHT - 12, PROMPT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
