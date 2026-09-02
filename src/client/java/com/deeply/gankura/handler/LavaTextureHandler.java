package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.LavaTextureArea;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 溶岩を水の見た目で描くかを決める。
 *
 * 実際の差し替えは LavaTextureMixin が行う。
 * 見た目はチャンクを組み立てた時点で焼き込まれるので、
 * 設定やエリアが変わったらチャンクを組み直させる必要がある。
 * その判断をここで一手に持ち、描画側は今の状態を見るだけにしている
 */
public class LavaTextureHandler {

    // チャンクの組み立ては別スレッドで走るので、そちらから見える形にする
    private static volatile boolean active;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LavaTextureHandler::tick);
    }

    /** 今、溶岩を水として描くか */
    public static boolean replacing() {
        return active;
    }

    /** 溶岩に入ったときのフォグを消すか */
    public static boolean hidingFog() {
        return active && ModConfig.INSTANCE.fishing.hideLavaFog;
    }

    private static void tick(Minecraft client) {
        boolean now = enabled();
        if (now == active) return;

        active = now;
        rebuildChunks(client);
    }

    private static boolean enabled() {
        ModConfig.FishingCategory config = ModConfig.INSTANCE.fishing;
        if (!config.replaceLavaTexture || !GameState.Server.isSkyblock()) return false;

        return config.lavaTextureArea == LavaTextureArea.EVERYWHERE
                || GameState.Server.isCrimsonIsle();
    }

    // 出来上がっているチャンクには前の見た目が入っているので、まとめて組み直させる
    private static void rebuildChunks(Minecraft client) {
        if (client.level == null) return;

        client.levelRenderer.allChanged();
    }
}
