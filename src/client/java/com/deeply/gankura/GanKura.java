package com.deeply.gankura;

import com.deeply.gankura.handler.ArachneHandler;
import com.deeply.gankura.handler.ArmorStackHandler;
import com.deeply.gankura.handler.CrimsonDropHandler;
import com.deeply.gankura.handler.FishingBobberTracker;
import com.deeply.gankura.handler.FloorDropHandler;
import com.deeply.gankura.handler.MenuOpenKeybindHandler;
import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.handler.QuiverAlertHandler;
import com.deeply.gankura.handler.ServerRestartHandler;
import com.deeply.gankura.handler.WarpCooldownHandler;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.scanner.*;
import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.gui.WaypointScreen;
import com.deeply.gankura.waypoint.Waypoint;
import com.deeply.gankura.waypoint.WaypointData;
import com.deeply.gankura.waypoint.WaypointManager;
import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.ModConfig;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GanKura implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("GanKura");

    // ★追加: 画面を開くための「予約チケット（フラグ）」
    private static boolean openConfigNextTick = false;
    private static boolean openHudNextTick = false;
    // ウェイポイント画面もコマンドから開く。チャットが閉じた次のtickまで待つのは他と同じ理由
    private static boolean openWaypointNextTick = false;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        WaypointManager.getInstance().load();
        NetworkHandler.init();
        // エリア判定(タブリスト/スコアボード由来)は他のスキャナーの前提条件なので最初に登録する
        LocationScanner.register();
        TabListScanner.register();
        GolemLocationScanner.register();
        RareDropScanner.register();
        // AreaScanner(inSanctuary判定)は、同tick内でEntityHealthScannerが古い値を参照して
        // webAreaLoaded/cobwebDetectedが1tick分staleになりHUDがUnknownに一瞬なる不具合を防ぐため、
        // 必ずEntityHealthScannerより先に登録する
        AreaScanner.register();
        EntityHealthScanner.register();
        PetHandler.register();
        ServerRestartHandler.register();
        ArmorStackHandler.register();
        ArrowPoisonScanner.register();
        QuiverScanner.register();
        QuiverAlertHandler.register();
        CrimsonDropHandler.register();
        EntityHighlightManager.register();
        WarpCooldownHandler.register();
        EquipmentScanner.register();
        ArachneHandler.register();
        MenuOpenKeybindHandler.register();
        FloorDropHandler.register();
        BeeNestScanner.register();
        FishingBobberTracker.register();

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
            if (openWaypointNextTick) {
                openWaypointNextTick = false;
                client.setScreen(new WaypointScreen(null));
            }
        });

        // ★追加: ゲーム終了時に、確実に最新の設定をファイルに保存（セーブ）する！
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.INSTANCE.saveNow();
            WaypointManager.getInstance().save();
            // 最後にスキャンしたSkyblock Equipmentも保存する (レジストリ情報が必要なためワールド参加中のみ)
            if (client.world != null) {
                EquipmentState.save(client.world.getRegistryManager());
            }
            LOGGER.info("GanKura config saved successfully on exit.");
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
                    .then(waypointCommand())
            );

            // ウェイポイントは出し入れの回数が多いので、短い別名も用意しておく
            dispatcher.register(ClientCommandManager.literal("gkw")
                    .executes(context -> {
                        openWaypointNextTick = true;
                        return 1;
                    })
                    .redirect(dispatcher.getRoot().getChild("gankura").getChild("waypoint")));
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

    // /gankura waypoint ... : 画面を開く / その場に追加 / 一覧 / 表示のON・OFF
    private static LiteralArgumentBuilder<FabricClientCommandSource> waypointCommand() {
        return ClientCommandManager.literal("waypoint")
                .executes(context -> {
                    openWaypointNextTick = true;
                    return 1;
                })
                .then(ClientCommandManager.literal("add")
                        .executes(context -> addWaypoint(context, null))
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> addWaypoint(context, StringArgumentType.getString(context, "name")))))
                .then(ClientCommandManager.literal("list").executes(GanKura::listWaypoints))
                .then(ClientCommandManager.literal("toggle").executes(GanKura::toggleWaypoints));
    }

    // 他のModのチャットに紛れないよう、Modの他の通知と同じ接頭辞を付ける。
    // 接頭辞に直接つなぐと、その色(暗いネザライト色)を本文まで受け継いで読みづらくなるので、
    // 色を持たない親の下に接頭辞と本文を並べる
    private static Text waypointPrefixed(Text message) {
        return Text.empty()
                .append(NotificationUtils.getGanKuraPrefix())
                .append(message);
    }

    // チャットの中で拾いやすいよう、ウェイポイント名・エリア名・座標は地の文と別の色にする
    private static final Formatting WAYPOINT_NAME_COLOR = Formatting.AQUA;
    private static final Formatting WAYPOINT_AREA_COLOR = Formatting.AQUA;
    private static final Formatting WAYPOINT_POS_COLOR = Formatting.AQUA;

    private static Text waypointName(String name) {
        return Text.literal(name).formatted(WAYPOINT_NAME_COLOR);
    }

    private static Text waypointArea(String area) {
        return Text.literal(area).formatted(WAYPOINT_AREA_COLOR);
    }

    private static Text waypointPos(int x, int y, int z) {
        return Text.literal(coordinates(x, y, z)).formatted(WAYPOINT_POS_COLOR);
    }

    private static String coordinates(int x, int y, int z) {
        return "(%d, %d, %d)".formatted(x, y, z);
    }

    private static int addWaypoint(CommandContext<FabricClientCommandSource> context, String name) {
        String area = WaypointManager.currentArea();
        List<Waypoint> waypoints = WaypointManager.getInstance().waypointsForEditing(area);
        BlockPos pos = context.getSource().getPlayer().getBlockPos();
        Waypoint existing = WaypointManager.getInstance().findAt(area, pos);

        // 1ブロックに2つは置けないので、既にあるならその名前を返して終わる
        if (existing != null) {
            context.getSource().sendError(waypointPrefixed(Text.literal("That block already has the waypoint ")
                    .append(waypointName(existing.getName()))
                    .append(Text.literal("."))));
            return 0;
        }

        String newName = name == null || name.isBlank() ? "Waypoint " + (waypoints.size() + 1) : name;
        WaypointManager.getInstance().add(area, Waypoint.of(newName, pos, Waypoint.DEFAULT_GROUP));

        context.getSource().sendFeedback(waypointPrefixed(Text.literal("Added waypoint ")
                .append(waypointName(newName))
                .append(Text.literal(" in "))
                .append(waypointArea(area))
                .append(Text.literal(" at "))
                .append(waypointPos(pos.getX(), pos.getY(), pos.getZ()))
                .append(Text.literal("."))));
        return 1;
    }

    private static int listWaypoints(CommandContext<FabricClientCommandSource> context) {
        String area = WaypointManager.currentArea();
        List<Waypoint> waypoints = WaypointManager.getInstance().waypoints(area);

        if (waypoints.isEmpty()) {
            context.getSource().sendFeedback(waypointPrefixed(Text.literal("No waypoints in ")
                    .append(waypointArea(area))
                    .append(Text.literal("."))));
            return 0;
        }

        context.getSource().sendFeedback(waypointPrefixed(Text.literal("Waypoints in ")
                .append(waypointArea(area))
                .append(Text.literal(" (" + waypoints.size() + "):"))));

        for (Waypoint waypoint : waypoints) {
            String group = waypoint.getGroup().isEmpty() ? "" : " [" + waypoint.getGroup() + "]";

            // 非表示にしているものは暗い色にして、一覧の中で見分けが付くようにする
            context.getSource().sendFeedback(Text.literal("- ")
                    .append(Text.literal(waypoint.getName())
                            .formatted(waypoint.isEnabled() ? Formatting.WHITE : Formatting.DARK_GRAY))
                    .append(Text.literal(" "))
                    .append(waypointPos(waypoint.getX(), waypoint.getY(), waypoint.getZ()))
                    .append(Text.literal(group).formatted(Formatting.GRAY)));
        }

        return waypoints.size();
    }

    private static int toggleWaypoints(CommandContext<FabricClientCommandSource> context) {
        WaypointData data = WaypointManager.getInstance().data();
        data.enabled = !data.enabled;
        WaypointManager.getInstance().save();

        context.getSource().sendFeedback(waypointPrefixed(Text.literal(
                data.enabled ? "Waypoint rendering enabled." : "Waypoint rendering disabled.")));
        return 1;
    }
}
