package com.deeply.gankura;

import com.deeply.gankura.handler.ArmorStackHandler;
import com.deeply.gankura.handler.CrimsonDropHandler;
import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.handler.ServerRestartHandler;
import com.deeply.gankura.handler.WarpCooldownHandler;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.scanner.*;
import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.ModConfig;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GanKura implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("GanKura");

    // ★追加: 画面を開くための「予約チケット（フラグ）」
    private static boolean openConfigNextTick = false;
    private static boolean openHudNextTick = false;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
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
        CrimsonDropHandler.register();
        WarpCooldownHandler.register();
        EquipmentScanner.register();

        // ★追加: 毎ティック（1/20秒）ごとに予約チケットをチェックする
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigNextTick) {
                openConfigNextTick = false;
                openConfigScreen(); // チャットが閉じた「後」に安全に開く！
            }
            if (openHudNextTick) {
                openHudNextTick = false;
                // MinecraftClient -> Minecraft -> 26.2: Gui.setScreen()
                client.gui.setScreen(new HudEditorScreen());
            }
        });

        // ★追加: ゲーム終了時に、確実に最新の設定をファイルに保存する
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.INSTANCE.saveNow();
            // 最後にスキャンしたSkyblock Equipmentも保存する (レジストリ情報が必要なためワールド参加中のみ)
            if (client.level != null) {
                EquipmentState.save(client.level.registryAccess());
            }
            LOGGER.info("GanKura config saved successfully on exit.");
        });

        // コマンドの登録
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommands.literal("gankura")
                    // /gankura (引数なし) -> 設定画面の予約チケットをON
                    .executes(context -> {
                        openConfigNextTick = true;
                        return 1;
                    })
                    // /gankura hud -> HUD移動画面の予約チケットをON
                    .then(ClientCommands.literal("hud")
                            .executes(context -> {
                                openHudNextTick = true;
                                return 1;
                            })
                    )
            );
        });

        LOGGER.info("GanKura initialized (Mojang Mapping).");
    }

    // ★設定画面展開メソッド
    private static void openConfigScreen() {
        try {
            // 1. デフォルトのUIパーツを登録した状態でプロセッサを生成
            MoulConfigProcessor<ModConfig> processor = MoulConfigProcessor.withDefaults(ModConfig.INSTANCE);

            // 2. ドライバーの生成
            ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);

            // 3. 解析の実行
            driver.processConfig(ModConfig.INSTANCE);

            // 4. 解析が完了したプロセッサをエディタに渡す
            MoulConfigEditor<ModConfig> editor = new MoulConfigEditor<>(processor);
            GuiElementComponent editorComponent = new GuiElementComponent(editor);
            GuiContext guiContext = new GuiContext(editorComponent);

            // MinecraftClient -> Minecraft -> 26.2: Gui.screen() / Gui.setScreen()
            MoulConfigScreenComponent configScreen = new MoulConfigScreenComponent(
                    Component.literal("GanKura Configuration"),
                    guiContext,
                    Minecraft.getInstance().gui.screen()
            );

            Minecraft.getInstance().gui.setScreen(configScreen);
        } catch (Exception e) {
            LOGGER.error("Failed to open MoulConfig screen!", e);
        }
    }
}