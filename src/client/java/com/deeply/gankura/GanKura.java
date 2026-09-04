package com.deeply.gankura;

import com.deeply.gankura.handler.ArachneHandler;
import com.deeply.gankura.handler.AttributeCostPanel;
import com.deeply.gankura.handler.ArmorStackHandler;
import com.deeply.gankura.handler.CrimsonDropHandler;
import com.deeply.gankura.handler.FishingBobberTracker;
import com.deeply.gankura.handler.FloorDropHandler;
import com.deeply.gankura.handler.GoldenFishHandler;
import com.deeply.gankura.handler.HotspotAlertHandler;
import com.deeply.gankura.handler.LavaTextureHandler;
import com.deeply.gankura.handler.HotspotAreaHandler;
import com.deeply.gankura.handler.HotspotRadarHandler;
import com.deeply.gankura.handler.InventoryButtonHandler;
import com.deeply.gankura.handler.AttributeTooltipHandler;
import com.deeply.gankura.handler.BestiaryTooltipHandler;
import com.deeply.gankura.handler.EnchantTooltipHandler;
import com.deeply.gankura.handler.ItemPriceTooltipHandler;
import com.deeply.gankura.handler.ScrollableTooltipHandler;
import com.deeply.gankura.handler.MenuOpenKeybindHandler;
import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.handler.PetHandler;
import com.deeply.gankura.handler.QuiverAlertHandler;
import com.deeply.gankura.handler.ServerRestartHandler;
import com.deeply.gankura.handler.WarpCooldownHandler;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.util.DevHooks;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.scanner.*;
import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.gui.InventoryButtonEditorScreen;
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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
    // Inventory Button の編集画面も同じ理由で 1tick 待つ
    private static boolean openButtonEditorNextTick = false;

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
        GoldfinShardScanner.register();
        BaitScanner.register();
        QuiverAlertHandler.register();
        EntityHighlightManager.register();
        CrimsonDropHandler.register();
        WarpCooldownHandler.register();
        HotspotAlertHandler.register();
        HotspotAreaHandler.register();
        GoldenFishHandler.register();
        HotspotRadarHandler.register();
        EquipmentScanner.register();
        ArachneHandler.register();
        MenuOpenKeybindHandler.register();
        FloorDropHandler.register();
        BeeNestScanner.register();
        FishingBobberTracker.register();
        InventoryButtonHandler.register();
        EnchantTooltipHandler.register();
        BestiaryTooltipHandler.register();
        AttributeTooltipHandler.register();
        ItemPriceTooltipHandler.register();
        AttributeCostPanel.register();
        ScrollableTooltipHandler.register();
        LavaTextureHandler.register();
        // 開発中の一時的な機能。配布ビルドには入っていないので、あるときだけ読み込む
        DevHooks.load();

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
            if (openWaypointNextTick) {
                openWaypointNextTick = false;
                client.gui.setScreen(new WaypointScreen(null));
            }
            if (openButtonEditorNextTick) {
                openButtonEditorNextTick = false;
                client.gui.setScreen(new InventoryButtonEditorScreen(null));
            }
        });

        // ★追加: ゲーム終了時に、確実に最新の設定をファイルに保存する
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModConfig.INSTANCE.saveNow();
            WaypointManager.getInstance().save();
            // 最後にスキャンしたSkyblock Equipmentも保存する (レジストリ情報が必要なためワールド参加中のみ)
            if (client.level != null) {
                EquipmentState.save(client.level.registryAccess());
            }
            LOGGER.info("GanKura config saved successfully on exit.");
        });

        // コマンドの登録
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> gankura = ClientCommands.literal("gankura")
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
                    // /gankura buttons -> Inventory Button の編集画面の予約チケットをON
                    .then(ClientCommands.literal("buttons")
                            .executes(context -> {
                                openButtonEditorNextTick = true;
                                return 1;
                            })
                    )
                    .then(waypointCommand())
                    .then(shareHotspotCommand());

            // 開発中の一時的な機能。配布ビルドでは何も足されない
            DevHooks.buildCommand(gankura);
            dispatcher.register(gankura);

            // ウェイポイントは出し入れの回数が多いので、短い別名も用意しておく
            dispatcher.register(ClientCommands.literal("gkw")
                    .executes(context -> {
                        openWaypointNextTick = true;
                        return 1;
                    })
                    .redirect(dispatcher.getRoot().getChild("gankura").getChild("waypoint")));
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
            // 開発中の一時的な機能。配布ビルドではカテゴリが増えない
            DevHooks.extendConfig(processor);

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

    // /gankura waypoint ... : 画面を開く / その場に追加 / 一覧 / 表示のON・OFF
    // チャットに出した共有ボタン専用。手で打つことは想定していない
    private static LiteralArgumentBuilder<FabricClientCommandSource> shareHotspotCommand() {
        return ClientCommands.literal("sharehotspot")
                .then(ClientCommands.argument("id", IntegerArgumentType.integer())
                        .executes(context -> {
                            HotspotAreaHandler.share(IntegerArgumentType.getInteger(context, "id"));
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> waypointCommand() {
        return ClientCommands.literal("waypoint")
                .executes(context -> {
                    openWaypointNextTick = true;
                    return 1;
                })
                .then(ClientCommands.literal("add")
                        .executes(context -> addWaypoint(context, null))
                        .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> addWaypoint(context, StringArgumentType.getString(context, "name")))))
                .then(ClientCommands.literal("list").executes(GanKura::listWaypoints))
                .then(ClientCommands.literal("toggle").executes(GanKura::toggleWaypoints));
    }

    // 他のModのチャットに紛れないよう、Modの他の通知と同じ接頭辞を付ける。
    // 接頭辞に直接つなぐと、その色(暗いネザライト色)を本文まで受け継いで読みづらくなるので、
    // 色を持たない親の下に接頭辞と本文を並べる
    private static Component waypointPrefixed(Component message) {
        return Component.empty()
                .append(NotificationUtils.getGanKuraPrefix())
                .append(message);
    }

    // チャットの中で拾いやすいよう、ウェイポイント名・エリア名・座標は地の文と別の色にする
    private static final ChatFormatting WAYPOINT_NAME_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting WAYPOINT_AREA_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting WAYPOINT_POS_COLOR = ChatFormatting.AQUA;

    private static Component waypointName(String name) {
        return Component.literal(name).withStyle(WAYPOINT_NAME_COLOR);
    }

    private static Component waypointArea(String area) {
        return Component.literal(area).withStyle(WAYPOINT_AREA_COLOR);
    }

    private static Component waypointPos(int x, int y, int z) {
        return Component.literal(coordinates(x, y, z)).withStyle(WAYPOINT_POS_COLOR);
    }

    private static String coordinates(int x, int y, int z) {
        return "(%d, %d, %d)".formatted(x, y, z);
    }

    private static int addWaypoint(CommandContext<FabricClientCommandSource> context, String name) {
        String area = WaypointManager.currentArea();
        List<Waypoint> waypoints = WaypointManager.getInstance().waypointsForEditing(area);
        BlockPos pos = context.getSource().getPlayer().blockPosition();
        Waypoint existing = WaypointManager.getInstance().findAt(area, pos);

        // 1ブロックに2つは置けないので、既にあるならその名前を返して終わる
        if (existing != null) {
            context.getSource().sendError(waypointPrefixed(Component.literal("That block already has the waypoint ")
                    .append(waypointName(existing.getName()))
                    .append(Component.literal("."))));
            return 0;
        }

        String newName = name == null || name.isBlank() ? "Waypoint " + (waypoints.size() + 1) : name;
        WaypointManager.getInstance().add(area, Waypoint.of(newName, pos, Waypoint.DEFAULT_GROUP));

        context.getSource().sendFeedback(waypointPrefixed(Component.literal("Added waypoint ")
                .append(waypointName(newName))
                .append(Component.literal(" in "))
                .append(waypointArea(area))
                .append(Component.literal(" at "))
                .append(waypointPos(pos.getX(), pos.getY(), pos.getZ()))
                .append(Component.literal("."))));
        return 1;
    }

    private static int listWaypoints(CommandContext<FabricClientCommandSource> context) {
        String area = WaypointManager.currentArea();
        List<Waypoint> waypoints = WaypointManager.getInstance().waypoints(area);

        if (waypoints.isEmpty()) {
            context.getSource().sendFeedback(waypointPrefixed(Component.literal("No waypoints in ")
                    .append(waypointArea(area))
                    .append(Component.literal("."))));
            return 0;
        }

        context.getSource().sendFeedback(waypointPrefixed(Component.literal("Waypoints in ")
                .append(waypointArea(area))
                .append(Component.literal(" (" + waypoints.size() + "):"))));

        for (Waypoint waypoint : waypoints) {
            String group = waypoint.getGroup().isEmpty() ? "" : " [" + waypoint.getGroup() + "]";

            // 非表示にしているものは暗い色にして、一覧の中で見分けが付くようにする
            context.getSource().sendFeedback(Component.literal("- ")
                    .append(Component.literal(waypoint.getName())
                            .withStyle(waypoint.isEnabled() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" "))
                    .append(waypointPos(waypoint.getX(), waypoint.getY(), waypoint.getZ()))
                    .append(Component.literal(group).withStyle(ChatFormatting.GRAY)));
        }

        return waypoints.size();
    }

    private static int toggleWaypoints(CommandContext<FabricClientCommandSource> context) {
        WaypointData data = WaypointManager.getInstance().data();
        data.enabled = !data.enabled;
        WaypointManager.getInstance().save();

        context.getSource().sendFeedback(waypointPrefixed(Component.literal(
                data.enabled ? "Waypoint rendering enabled." : "Waypoint rendering disabled.")));
        return 1;
    }
}
