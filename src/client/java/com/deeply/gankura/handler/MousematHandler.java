package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Squeaky Mousemat に向きを合わせたあと、視点を固定する。
 *
 * 向きを合わせるのは Hypixel の側で、合わせ終わると
 * "Snapped to squeaky mousemat!" と知らせが来る。
 * こちらはそれを合図に、マウスで視点が動かないようにするだけ。
 * 畑を耕している間に少しずつ向きがずれていくのを止めるための機能。
 * 解くのは割り当てたキーか、畑を移ったときだけ。
 * 敷物を右クリックし直しても解けないのは、向きを合わせ直しただけのつもりで
 * 固定まで外れてしまうと分かりにくいため
 */
public final class MousematHandler {

    // 敷物に向きを合わせ終わったときの知らせ
    private static final String SNAP_MESSAGE = "Snapped to squeaky mousemat!";

    // 畑を移動したときの知らせ。移った先では向きが違うので、そのままだと動けなくなる
    private static final String TELEPORT_PREFIX = "Teleported you to ";
    private static final String WARP_MESSAGE = "Warping...";

    private static final String LOCKED_TEXT = "§eView locked. §7Press §e";
    private static final String LOCKED_TEXT_TAIL = " §7to let it go.";
    // 解除キーを割り当てていないときの言い方。押す物が無いので、設定に案内する
    private static final String LOCKED_TEXT_NO_KEY =
            "§eView locked. §7Set a release key in the settings to let it go.";
    private static final String RELEASED_TEXT = "§eView released.";

    private static boolean locked = false;
    // 押しっぱなしで何度も解こうとしないよう、押した瞬間だけを拾う
    private static boolean keyWasDown = false;

    private MousematHandler() {
    }

    /** 割り当てたキーで解けるようにする。敷物から離れてしまったときの逃げ道 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MousematHandler::tick);
    }

    private static void tick(Minecraft client) {
        // 画面を開いている間はそちらに入力を譲る。文字を打っただけで解けてしまわないように
        if (client.player == null || client.screen != null) {
            keyWasDown = false;
            return;
        }

        boolean down = isKeyDown(client, ModConfig.INSTANCE.farming.garden.releaseViewKeybind);
        if (down && !keyWasDown && isLocked()) {
            locked = false;
            say(client, RELEASED_TEXT);
        }
        keyWasDown = down;
    }

    // 割り当てていないキーは -1(GLFW_KEY_UNKNOWN)で保存される
    private static boolean isKeyDown(Minecraft client, int keyCode) {
        if (keyCode < 0) return false;
        return InputConstants.isKeyDown(client.getWindow(), keyCode);
    }

    /** 今、マウスでの視点移動を止めているか */
    public static boolean isLocked() {
        return locked && isEnabled();
    }

    public static void reset() {
        locked = false;
    }

    public static void handleMessage(String unformattedMessage, Minecraft client) {
        if (!isEnabled()) {
            locked = false;
            return;
        }

        String message = unformattedMessage.trim();

        if (message.equals(SNAP_MESSAGE)) {
            // 合わせ直すたびに知らせを出しても仕方がないので、掛かっていなければだけ
            if (!locked) {
                locked = true;
                say(client, lockedText());
            }
            return;
        }

        if (!locked || !ModConfig.INSTANCE.farming.garden.unlockViewOnTeleport) return;

        // 飛んだ先で視点が固まったままだと、身動きが取れないと勘違いしやすい
        if (message.startsWith(TELEPORT_PREFIX) || message.equals(WARP_MESSAGE)) {
            locked = false;
            say(client, RELEASED_TEXT);
        }
    }

    private static boolean isEnabled() {
        return ModConfig.INSTANCE.farming.garden.lockViewOnMousemat && GameState.Server.isGarden();
    }

    /** 固定したときの知らせ。解除キーを割り当てていれば、その名前を添える */
    private static String lockedText() {
        int keyCode = ModConfig.INSTANCE.farming.garden.releaseViewKeybind;
        if (keyCode < 0) return LOCKED_TEXT_NO_KEY;

        String key = InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
        return LOCKED_TEXT + key + LOCKED_TEXT_TAIL;
    }

    private static void say(Minecraft client, String text) {
        client.execute(() -> NotificationUtils.sendSystemChat(client, Component.literal(text)));
    }
}
