package com.deeply.gankura;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.render.ConfigScreen; // ★追加
import com.deeply.gankura.scanner.GolemHealthScanner; // ★追加
import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.scanner.ItemDropScanner;
import com.deeply.gankura.scanner.LocationScanner;
import com.deeply.gankura.scanner.StageScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
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
        // ★追加
        GolemHealthScanner.register();
        // ★追加
        PetHandler.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("gankura")
                    // ★ /gankura (引数なし) -> 設定画面を開く
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() ->
                                MinecraftClient.getInstance().setScreen(new ConfigScreen())
                        );
                        return 1;
                    })
                    // ★ /gankura hud -> HUD移動画面を直接開く
                    .then(ClientCommandManager.literal("hud")
                            .executes(context -> {
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