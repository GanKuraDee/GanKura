package com.deeply.gankura;

import com.deeply.gankura.handler.ArmorStackHandler;
import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.handler.ServerRestartHandler;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.scanner.*;
import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.render.ModConfig;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GanKura implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("GanKura");

    // ★追加: 画面を開くための「予約チケット（フラグ）」
    private static boolean openConfigNextTick = false;
    private static boolean openHudNextTick = false;

    @Override
    public void onInitializeClient() {
        NetworkHandler.init();
        TabListScanner.register();
        GolemLocationScanner.register();
        RareDropScanner.register();
        EntityHealthScanner.register();
        PetHandler.register();
        ServerRestartHandler.register();
        ArmorStackHandler.register();
        ArrowPoisonScanner.register();
        EntityHighlightManager.register();

        // ★追加: 毎ティック（1/20秒）ごとに予約チケットをチェックする
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigNextTick) {
                openConfigNextTick = false;
                openConfigScreen(); // チャットが閉じた「後」に安全に開く！
            }
            if (openHudNextTick) {
                openHudNextTick = false;
                client.setScreen(new HudEditorScreen());
            }
        });

        // コマンドの登録
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("gankura")
                    // /gankura (引数なし) -> 設定画面の予約チケットをON
                    .executes(context -> {
                        openConfigNextTick = true;
                        return 1;
                    })
                    // /gankura hud -> HUD移動画面の予約チケットをON
                    .then(ClientCommandManager.literal("hud")
                            .executes(context -> {
                                openHudNextTick = true;
                                return 1;
                            })
                    )
            );
        });

        LOGGER.info("GanKura initialized (Refactored).");
    }

    // ★抽出した設定画面展開メソッド
    private static void openConfigScreen() {
        try {
            // 1. デフォルトのUIパーツを登録した状態でプロセッサを生成
            MoulConfigProcessor<ModConfig> processor = MoulConfigProcessor.withDefaults(ModConfig.INSTANCE);

            // 2. 【修正】ドライバーの生成時に、引数としてプロセッサ(reader)を渡す！
            ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);

            // 3. 【修正】解析の実行には、コンフィグ本体（1つ目の引数）だけを渡す！
            driver.processConfig(ModConfig.INSTANCE);

            // 4. 解析が完了したプロセッサをエディタに渡す
            MoulConfigEditor<ModConfig> editor = new MoulConfigEditor<>(processor);
            GuiElementComponent editorComponent = new GuiElementComponent(editor);
            GuiContext guiContext = new GuiContext(editorComponent);

            MoulConfigScreenComponent configScreen = new MoulConfigScreenComponent(
                    Text.literal("GanKura Configuration"),
                    guiContext,
                    MinecraftClient.getInstance().currentScreen
            );

            MinecraftClient.getInstance().setScreen(configScreen);
        } catch (Exception e) {
            LOGGER.error("Failed to open MoulConfig screen!", e);
        }
    }
}