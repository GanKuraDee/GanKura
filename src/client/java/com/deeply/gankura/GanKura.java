package com.deeply.gankura;

import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.render.HudEditorScreen; // ★追加
import com.deeply.gankura.scanner.ItemDropScanner;
import com.deeply.gankura.scanner.LocationScanner;
import com.deeply.gankura.scanner.StageScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager; // ★追加
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback; // ★追加
import net.minecraft.client.MinecraftClient; // ★追加
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GanKura implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("GanKura");

    @Override
    public void onInitializeClient() {
        NetworkHandler.init();
        StageScanner.register();
        LocationScanner.register();
        ItemDropScanner.register();

        // ★追加: /gankura hud コマンドの登録
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("gankura")
                    .then(ClientCommandManager.literal("hud")
                            .executes(context -> {
                                // 画面を開く (メインスレッドで実行)
                                MinecraftClient.getInstance().send(() ->
                                        MinecraftClient.getInstance().setScreen(new HudEditorScreen())
                                );
                                return 1;
                            })
                    )
            );
        });

        LOGGER.info("GanKura initialized (Refactored).");
    }
}