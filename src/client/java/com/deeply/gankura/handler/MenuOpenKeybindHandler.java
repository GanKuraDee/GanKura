package com.deeply.gankura.handler;

import com.deeply.gankura.data.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.Arrays;

// Wardrobe/Equipment/Loadoutsの各メニューを、設定画面で割り当てたキーから直接開けるようにする。
// メニューを開く手段はHypixel側のコマンド(/wardrobe /equipment /loadouts)しかないため、
// ここではキー押下をエッジ検出してコマンドを送信するだけにしている。
// MenuSetKeybindMixin(メニューを開いている間のスロット切り替え)とは対になる機能。
public class MenuOpenKeybindHandler {
    private static final String[] COMMANDS = {"loadouts", "wardrobe", "equipment"};
    private static final boolean[] WAS_DOWN = new boolean[COMMANDS.length];

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MenuOpenKeybindHandler::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        // GUI表示中(チャット入力・コンテナ画面・設定画面など)はキー入力をそちらに譲り、誤爆を防ぐ。
        // 無効時も含め、押しっぱなしの状態を持ち越さないようにリセットしておく
        if (!ModConfig.INSTANCE.misc.enableOpenMenuKeybind || client.currentScreen != null) {
            Arrays.fill(WAS_DOWN, false);
            return;
        }

        ModConfig.MiscCategory misc = ModConfig.INSTANCE.misc;
        int[] keybinds = {misc.openLoadoutsKeybind, misc.openWardrobeKeybind, misc.openEquipmentKeybind};

        for (int i = 0; i < COMMANDS.length; i++) {
            boolean down = isKeyDown(client, keybinds[i]);
            // 押した瞬間のみ送信する(押しっぱなしでコマンドを連投しない)
            if (down && !WAS_DOWN[i]) {
                client.player.networkHandler.sendChatCommand(COMMANDS[i]);
            }
            WAS_DOWN[i] = down;
        }
    }

    // 未設定のキーバインドは-1(GLFW_KEY_UNKNOWN)で保存されるため除外する
    private static boolean isKeyDown(MinecraftClient client, int keyCode) {
        if (keyCode < 0) return false;
        return InputUtil.isKeyPressed(client.getWindow(), keyCode);
    }
}
