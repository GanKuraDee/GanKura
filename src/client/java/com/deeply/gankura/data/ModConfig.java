package com.deeply.gankura.data;

import com.deeply.gankura.render.HudEditorScreen;
import com.deeply.gankura.gui.WaypointScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModConfig extends Config {

    public enum HudOrientation {
        HORIZONTAL, VERTICAL;

        @Override
        public String toString() {
            return this == HORIZONTAL ? "Horizontal" : "Vertical";
        }
    }

    // ★修正1: final を外して、ファイルから読み込んだデータで上書きできるようにします
    public static ModConfig INSTANCE = new ModConfig();

    // ★修正2: セーブ＆ロード用のGsonを準備 (@Expose が付いた変数だけを処理する設定)
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    // ★修正3: 保存先を config/gankura/gankura_config.properties に変更
    private static File getConfigFile() {
        // "config/gankura" というフォルダへのパスを作成
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "gankura");

        // もし "gankura" フォルダが存在しなければ、新しく作成する
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // そのフォルダの中の "gankura_config.properties" を指定
        return new File(dir, "gankura_config.properties");
    }

    // ★修正4: 起動時にファイルを読み込むメソッドを強化
    public static void load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (Exception e) {
                System.err.println("[GanKura] Old config format detected or file corrupted. Overwriting with JSON...");
                // 古いproperties形式のテキストが残っていてエラーになった場合は、無視して新しいJSON形式で上書きさせます
            }
        }

        // ★超重要: Gsonでデータを読み込むと、transient（保存除外）にしていた「ボタンの処理」が消滅してしまうため、ここで再セットする！
        if (INSTANCE.gui == null)         INSTANCE.gui         = new GuiCategory();
        if (INSTANCE.theEnd == null)      INSTANCE.theEnd      = new TheEndCategory();
        if (INSTANCE.spidersDen == null)  INSTANCE.spidersDen  = new SpidersDenCategory();
        if (INSTANCE.crimsonIsle == null) INSTANCE.crimsonIsle = new CrimsonIsleCategory();
        if (INSTANCE.foraging == null)    INSTANCE.foraging    = new ForagingCategory();
        if (INSTANCE.mobVisuals == null) INSTANCE.mobVisuals = new MobVisualsCategory();
        INSTANCE.gui.openWaypointScreen = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new WaypointScreen(Minecraft.getInstance().screen));
            });
        };
        INSTANCE.gui.openHudEditor = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new HudEditorScreen());
            });
        };
        INSTANCE.mobVisuals.resetNameplateScale =
                () -> INSTANCE.mobVisuals.nameplateScale = MobVisualsCategory.DEFAULT_NAMEPLATE_SCALE;
        INSTANCE.misc.resetHeldItemScale = () -> INSTANCE.misc.heldItemScale = 1.0f;
        INSTANCE.misc.resetHeldItemOffsetX = () -> INSTANCE.misc.heldItemOffsetX = 0.0f;
        INSTANCE.misc.resetHeldItemOffsetY = () -> INSTANCE.misc.heldItemOffsetY = 0.0f;

        // ドラッグリストは要素を全削除できるため、未設定(null)と「空にした」を区別する必要がある。
        // null のときだけ既定値に戻し、Gsonが解決できなかった不明な要素(null)は取り除く
        INSTANCE.theEnd.trackedGolemDrops = normalizeEnumList(INSTANCE.theEnd.trackedGolemDrops, GolemRareDrop.defaults());
        INSTANCE.theEnd.trackedDragonDrops = normalizeEnumList(INSTANCE.theEnd.trackedDragonDrops, DragonRareDrop.defaults());
        INSTANCE.crimsonIsle.trackedCrimsonDrops = normalizeEnumList(INSTANCE.crimsonIsle.trackedCrimsonDrops, CrimsonRareDrop.defaults());
        INSTANCE.theEnd.dragonSpawnAlerts = normalizeEnumList(INSTANCE.theEnd.dragonSpawnAlerts, DragonAlertType.defaults());

        // 起動時やエラー発生時に、確実に現在の設定をJSON形式でファイルに書き込んでおく
        INSTANCE.mobVisuals.targetsTheEnd = normalizeEnumList(INSTANCE.mobVisuals.targetsTheEnd, List.of());
        INSTANCE.mobVisuals.targetsSpidersDen = normalizeEnumList(INSTANCE.mobVisuals.targetsSpidersDen, List.of());
        INSTANCE.mobVisuals.targetsCrimsonIsle = normalizeEnumList(INSTANCE.mobVisuals.targetsCrimsonIsle, List.of());
        INSTANCE.mobVisuals.targetsCrystalHollows = normalizeEnumList(INSTANCE.mobVisuals.targetsCrystalHollows, List.of());
        INSTANCE.mobVisuals.targetsSafariCavern = normalizeEnumList(INSTANCE.mobVisuals.targetsSafariCavern, List.of());
        INSTANCE.mobVisuals.targetsSafariForest = normalizeEnumList(INSTANCE.mobVisuals.targetsSafariForest, List.of());
        INSTANCE.mobVisuals.targetsSafariHaunted = normalizeEnumList(INSTANCE.mobVisuals.targetsSafariHaunted, List.of());
        INSTANCE.mobVisuals.targetsSafariIcy = normalizeEnumList(INSTANCE.mobVisuals.targetsSafariIcy, List.of());
        INSTANCE.mobVisuals.targetsMoongladeMarsh = normalizeEnumList(INSTANCE.mobVisuals.targetsMoongladeMarsh, List.of());
        INSTANCE.mobVisuals.targetsTorrhusCanyon = normalizeEnumList(INSTANCE.mobVisuals.targetsTorrhusCanyon, List.of());
        INSTANCE.mobVisuals.targetsLotusAtoll = normalizeEnumList(INSTANCE.mobVisuals.targetsLotusAtoll, List.of());

        INSTANCE.saveNow();
    }

    private static <T> List<T> normalizeEnumList(List<T> loaded, List<T> defaults) {
        if (loaded == null) return new ArrayList<>(defaults);
        List<T> cleaned = new ArrayList<>(loaded);
        cleaned.removeIf(java.util.Objects::isNull);
        return cleaned;
    }

    @Override
    public StructuredText getTitle() {
        String version = getModVersion();
        return StructuredText.of("GanKura (Release: " + version + ") by GanKuraDee");
    }

    // ★ バージョンを取得するための専用メソッドを追加
    private String getModVersion() {
        // "gankura" の部分は、あなたの fabric.mod.json に書かれている "id" に合わせてください
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("gankura");
        if (container.isPresent()) {
            // モジュールのメタデータからバージョンを文字列として取得
            return container.get().getMetadata().getVersion().getFriendlyString();
        }
        return "Unknown"; // 取得に失敗した場合の保険
    }

    // ==========================================
    // ★追加: 消えてしまっていた「セーブ処理」の本体を復活！
    // ==========================================
    @Override
    public void saveNow() {
        try (FileWriter writer = new FileWriter(getConfigFile())) {
            // Gsonを使って、現在の設定をJSON形式でファイルに書き込む
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("Failed to save GanKura config!");
            e.printStackTrace();
        }
    }

    // ==========================================
    // カテゴリの定義
    // ==========================================
    // ★ ここにGUIカテゴリを追加（一番上に書くことで、画面でも一番上に表示されます）
    @Expose
    @Category(name = "GUI", desc = "HUD and GUI settings.")
    public GuiCategory gui = new GuiCategory();

    @Expose
    @Category(name = "The End", desc = "End Stone Protector and Dragon.")
    public TheEndCategory theEnd = new TheEndCategory();

    @Expose
    @Category(name = "Spider's Den", desc = "Broodmother and Arachne.")
    public SpidersDenCategory spidersDen = new SpidersDenCategory();

    @Expose
    @Category(name = "Crimson Isle", desc = "Crimson Isle bosses.")
    public CrimsonIsleCategory crimsonIsle = new CrimsonIsleCategory();

    @Expose
    @Category(name = "Crystal Hollows", desc = "Crystal Hollows features.")
    public CrystalHollowsCategory crystalHollows = new CrystalHollowsCategory();

    @Expose
    @Category(name = "Foraging", desc = "Foraging features.")
    public ForagingCategory foraging = new ForagingCategory();

    @Expose
    @Category(name = "Fishing", desc = "Fishing features.")
    public FishingCategory fishing = new FishingCategory();

    @Expose
    @Category(name = "Mob Visuals", desc = "Which mobs get a highlight, tracer or nameplate.")
    public MobVisualsCategory mobVisuals = new MobVisualsCategory();

    @Expose
    @Category(name = "Misc", desc = "Miscellaneous features.")
    public MiscCategory misc = new MiscCategory();


    // ==========================================
    // 各カテゴリの中身（設定項目）
    // ==========================================
    // ★ 新しくGUIカテゴリの中身を追加
    public static class GuiCategory {
        // ボタンには @Expose は付けず、代わりに transient を付けます！
        @ConfigOption(name = "Edit GUI Locations", desc = "Opens HUD editor.")
        @ConfigEditorButton(buttonText = "Open")
        public transient Runnable openHudEditor = () -> {
            // ボタンが押されたら、マイクラの画面をHudEditorScreenに切り替える
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new HudEditorScreen());
            });
        };

        // 自分で置いたウェイポイントの一覧。MoulConfig では表を出せないので専用の画面を開く
        @ConfigOption(name = "Custom Waypoints", desc = "Opens the waypoint list of the area you are in.")
        @ConfigEditorButton(buttonText = "Open")
        public transient Runnable openWaypointScreen = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new WaypointScreen(Minecraft.getInstance().screen));
            });
        };
    }

    // ==========================================
    // The End: End Stone Protector + Dragon
    // ==========================================
    public static class TheEndCategory {

        // ========== End Stone Protector section ==========
        @Expose
        @ConfigOption(name = "End Stone Protector", desc = "Expands End Stone Protector settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean golemSection = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showGolemStatusHud = true;

        // ---- Rare Drop Settings (id: 7) ----
        @Expose
        @ConfigOption(name = "Rare Drop Settings", desc = "Expands rare drop settings.")
        @ConfigEditorAccordion(id = 7)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemRareDropFolder = false;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 7)
        public boolean showLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 7)
        public boolean enableDropAlerts = true;

        @Expose
        @ConfigOption(name = "Tracked Drops", desc = "Change which drops are scanned and shown on the loot tracker HUD.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 7)
        public List<GolemRareDrop> trackedGolemDrops = new ArrayList<>(GolemRareDrop.defaults());

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Expands text and beacon settings.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean worldLocationFolder = false;

        @Expose
        @ConfigOption(name = "Show Text", desc = "Shows 3D floating text.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Text = true;

        @Expose
        @ConfigOption(name = "Show Beacon Beam", desc = "Shows beacon beam.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Beacon = true;

        @Expose
        @ConfigOption(name = "Show Tracer", desc = "Draws a line pointing at the 3D floating text.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Tracer = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expands stage 4 alerts.")
        @ConfigEditorAccordion(id = 3)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean stage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows stage 4 title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays stage 4 sound.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage4Sound = true;

        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expands stage 5 alerts.")
        @ConfigEditorAccordion(id = 4)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean stage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows stage 5 title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays stage 5 sound.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableStage5Sound = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expands chat messages.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemChatFolder = false;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Message", desc = "Shows stage 4→5 duration.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showStage4Duration = true;

        @Expose
        @ConfigOption(name = "DPS Message", desc = "Shows DPS results in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Message", desc = "Shows loot quality in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Day 30+ Alert Message", desc = "Alerts on day 30+.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean enableDay30Alert = true;

        // ---- Nameplate (id: 8) ----
        // ========== Dragon section ==========
        @Expose
        @ConfigOption(name = "Dragon", desc = "Expands Dragon settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean dragonSection = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean showDragonStatusHud = true;

        // ---- Rare Drop Settings (id: 15) ----
        @Expose
        @ConfigOption(name = "Rare Drop Settings", desc = "Expands rare drop settings.")
        @ConfigEditorAccordion(id = 15)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonRareDropFolder = false;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 15)
        public boolean showDragonTrackerHud = true;

        @Expose
        @ConfigOption(name = "Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 15)
        public boolean enableDragonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Tracked Drops", desc = "Change which drops are scanned and shown on the loot tracker HUD.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 15)
        public List<DragonRareDrop> trackedDragonDrops = new ArrayList<>(DragonRareDrop.defaults());

        @Expose
        @ConfigOption(name = "Spawn Alert Title", desc = "Expands spawn alerts.")
        @ConfigEditorAccordion(id = 12)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean spawnTitleFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Shows a spawn alert title for dragons.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonSpawnAlert = true;

        @Expose
        @ConfigOption(name = "Alert Dragons", desc = "Change which dragons show a spawn alert title.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 12)
        public List<DragonAlertType> dragonSpawnAlerts = new ArrayList<>(DragonAlertType.defaults());

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expands chat messages.")
        @ConfigEditorAccordion(id = 13)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonChatFolder = false;

        @Expose
        @ConfigOption(name = "DPS Message", desc = "Shows DPS results in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Message", desc = "Shows loot quality in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonLootQualityChat = true;

        // ---- Nameplate (id: 16) ----
    }

    // ==========================================
    // Spider's Den: Broodmother
    // ==========================================
    public static class SpidersDenCategory {

        @Expose
        @ConfigOption(name = "Broodmother", desc = "Expands Broodmother settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean broodmotherSection = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBroodmotherStatusHud = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expands stage 4 alerts.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherStage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows stage 4 title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays stage 4 sound.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Sound = true;

        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expands stage 5 alerts.")
        @ConfigEditorAccordion(id = 3)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherStage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows stage 5 title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays stage 5 sound.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Sound = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expands chat messages.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherChatFolder = false;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Message", desc = "Shows stage 4→5 duration.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherStage4Duration = true;

        // ---- Nameplate (id: 8) ----
        @Expose
        @ConfigOption(name = "Arachne", desc = "Expands Arachne settings.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        public boolean arachneSection = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows spawn countdown.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showArachneStatusHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Expands world location settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean arachneWorldLocationFolder = false;

        @Expose
        @ConfigOption(name = "Show Text", desc = "Shows floating text at altar.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean showArachneWorldText = true;

        // ---- Nameplate (id: 9) ----
    }

    // ==========================================
    // Crimson Isle: Status HUD + 5 bosses
    // ==========================================
    public static class CrimsonIsleCategory {

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows boss status.")
        @ConfigEditorBoolean
        public boolean showCrimsonIsleStatusHud = true;

        @Expose
        @ConfigOption(name = "Rare Drop Settings", desc = "Expands rare drop settings.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        public boolean crimsonRareDropFolder = false;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showCrimsonLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean enableCrimsonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Tracked Drops", desc = "Change which drops are scanned and shown on the loot tracker HUD.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 5)
        public List<CrimsonRareDrop> trackedCrimsonDrops = new ArrayList<>(CrimsonRareDrop.defaults());

        // ---- World Location Display (id: 6) ----
        @Expose
        @ConfigOption(name = "World Location Display", desc = "Expands world location settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        public boolean crimsonWorldLocationFolder = false;

        @Expose
        @ConfigOption(name = "Show Text", desc = "Shows floating text at spawns.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean showCrimsonIsleWorldText = true;

        // ---- Magma Boss (id: 40) ----
        @Expose
        @ConfigOption(name = "Magma Boss", desc = "Expands Magma Boss settings.")
        @ConfigEditorAccordion(id = 40)
        @ConfigEditorBoolean
        public boolean magmaBossSection = false;

        @Expose
        @ConfigOption(name = "Stage Status Title", desc = "Shows stage status title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossSpawnTitle = true;

    }

    // ==========================================
    // Foraging
    // ==========================================
    public static class ForagingCategory {
        @Expose
        @ConfigOption(name = "Title Settings", desc = "Expands title notification settings.")
        @ConfigEditorAccordion(id = 70)
        @ConfigEditorBoolean
        public boolean titleFolder = false;

        @Expose
        @ConfigOption(name = "Tree Felled Title", desc = "Shows a title when the whole tree is felled at once.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 70)
        public boolean enableTreeFelledTitle = true;

        @Expose
        @ConfigOption(name = "Mob From Tree Title", desc = "Shows a title when a mob falls from the felled tree.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 70)
        public boolean enableTreeMobTitle = true;

        @Expose
        @ConfigOption(name = "Cocoon Catch Title", desc = "Shows a title when you cocoon a mob.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 70)
        public boolean enableCocoonCatchTitle = true;

        @Expose
        @ConfigOption(name = "Torrhus Canyon", desc = "Expands Torrhus Canyon settings.")
        @ConfigEditorAccordion(id = 74)
        @ConfigEditorBoolean
        public boolean torrhusCanyonFolder = false;

        @Expose
        @ConfigOption(name = "Tiki Spawn Waypoints", desc = "Marks where Sneaky / Shrieky / Cheeky Tikis spawn.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 74)
        public boolean enableTikiWaypoints = false;

        @Expose
        @ConfigOption(name = "Critter Safari", desc = "Expands Critter Safari settings.")
        @ConfigEditorAccordion(id = 71)
        @ConfigEditorBoolean
        public boolean critterSafariFolder = false;

        @Expose
        @ConfigOption(name = "Captured Critters HUD", desc = "Lists every Critter Safari critter and marks the ones already captured.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean showCapturedCrittersHud = true;

        @Expose
        @ConfigOption(name = "Floor Drops", desc = "Marks the foraging drops lying on the ground.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean enableFloorDrops = true;

        @Expose
        @ConfigOption(name = "Bee Nest Waypoints", desc = "Marks the bee nests in the Forest Biome.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean enableBeeNestWaypoints = true;

        @Expose
        @ConfigOption(name = "Fish Highlight", desc = "Highlights the fish you can feed to Scrappy.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean enableSafariFishHighlight = false;

        @Expose
        @ConfigOption(name = "Rockmite Mound Highlight", desc = "Highlights the Rockmite Mounds hiding around the Cavern Biome.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean enableRockmiteMoundHighlight = true;

        @Expose
        @ConfigOption(name = "Wumpa", desc = "Expands Wumpa settings.")
        @ConfigEditorAccordion(id = 72)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean wumpaFolder = false;

        @Expose
        @ConfigOption(name = "Spawn Title", desc = "Shows a title when Wumpa spawns in the Icy Biome.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 72)
        public boolean enableWumpaSpawnTitle = true;

        @Expose
        @ConfigOption(name = "Capsule Usage Message", desc = "Posts how many Critter Capsules the capture took.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 72)
        public boolean enableWumpaCapsuleMessage = true;

        @Expose
        @ConfigOption(name = "Re-enter Waypoint", desc = "Marks the hole used to re-enter the Wumpa arena.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 72)
        public boolean enableWumpaWaypoint = true;

        @Expose
        @ConfigOption(name = "Doomspiral", desc = "Expands Doomspiral settings.")
        @ConfigEditorAccordion(id = 73)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean doomspiralFolder = false;

        @Expose
        @ConfigOption(name = "Capsule Usage Message", desc = "Posts how many Critter Capsules the capture took.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 73)
        public boolean enableDoomspiralCapsuleMessage = true;

        @Expose
        @ConfigOption(name = "Macaw", desc = "Expands Macaw settings.")
        @ConfigEditorAccordion(id = 75)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 71)
        public boolean macawFolder = false;

        @Expose
        @ConfigOption(name = "Spawn Title", desc = "Shows a title when two Macaws are attracted to the Birdfeeder.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 75)
        public boolean enableMacawSpawnTitle = true;

    }

    public static class CrystalHollowsCategory {

        @Expose
        @ConfigOption(name = "Boss Corleone Spawn Title", desc = "Shows a title when Boss Corleone shows up nearby.")
        @ConfigEditorBoolean
        public boolean enableCorleoneSpawnTitle = true;
    }

    // ==========================================
    // Mob Visuals: 3機能それぞれの対象モブ
    // ==========================================
    public static class FishingCategory {
        @Expose
        @ConfigOption(name = "Cast Timer", desc = "Expands cast timer settings.")
        @ConfigEditorAccordion(id = 57)
        @ConfigEditorBoolean
        public boolean castTimerFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Shows how long the bobber has been floating, above the bobber.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 57)
        public boolean showCastTimer = false;

        @Expose
        @ConfigOption(name = "Start on Liquid Touch", desc = "Starts counting when the bobber lands,\n"
                + "instead of when it is cast.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 57)
        public boolean castTimerOnLiquidTouch = true;

        @Expose
        @ConfigOption(name = "Bite Countdown HUD", desc = "Shows how long until something bites while fishing.")
        @ConfigEditorBoolean
        public boolean showBiteCountdownHud = false;

        @Expose
        @ConfigOption(name = "Shorten Catch Message", desc = "Replaces the long sea creature catch message\n"
                + "with a short one.")
        @ConfigEditorBoolean
        public boolean shortenSeaCreatureMessage = false;
    }

    public static class MobVisualsCategory {
        // ネームプレートの基準サイズ。1.0 でGUIスケール4相当の見え方になる
        public static final float DEFAULT_NAMEPLATE_SCALE = 1.0f;

        @Expose
        @ConfigOption(name = "Highlight", desc = "Outlines the target mobs with a glow.")
        @ConfigEditorBoolean
        public boolean enableHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a line to the target mobs.")
        @ConfigEditorBoolean
        public boolean enableTracer = true;

        @Expose
        @ConfigOption(name = "Tracer Target", desc = "Nearest: only the closest mob of each kind.\nAll: every mob found.")
        @ConfigEditorDropdown
        public TracerMode tracerMode = TracerMode.NEAREST;

        @Expose
        @ConfigOption(name = "Nameplate", desc = "Shows a nameplate on the target mobs.")
        @ConfigEditorBoolean
        public boolean enableNameplate = true;

        @Expose
        @ConfigOption(name = "Nameplate Size", desc = "Changes nameplate text size.")
        @ConfigEditorSlider(minValue = 0.25f, maxValue = 3.0f, minStep = 0.05f)
        public float nameplateScale = DEFAULT_NAMEPLATE_SCALE;

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Reset Nameplate Size", desc = "Reset to default.")
        @ConfigEditorButton(buttonText = "Reset")
        public transient Runnable resetNameplateScale = () -> nameplateScale = DEFAULT_NAMEPLATE_SCALE;

        @Expose
        @ConfigOption(name = "Nameplate Health", desc = "Shows the mob's health under its name.")
        @ConfigEditorBoolean
        public boolean showNameplateHealth = true;

        @Expose
        @ConfigOption(name = "Nameplate Capsule Count", desc = "Shows how many Critter Capsules have been thrown, under the mob's name.\nOnly Wumpa and Doomspiral have this.")
        @ConfigEditorBoolean
        public boolean showNameplateCapsule = true;

        @Expose
        @ConfigOption(name = "The End", desc = "Expands The End targets.")
        @ConfigEditorAccordion(id = 83)
        @ConfigEditorBoolean
        public boolean theEndFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 83)
        public List<MobVisual.TheEnd> targetsTheEnd = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every The End mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 83)
        public transient Runnable enableAllTheEnd = () -> setAll(targetsTheEnd, MobVisual.TheEnd.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 83)
        public transient Runnable disableAllTheEnd = () -> targetsTheEnd.clear();

        @Expose
        @ConfigOption(name = "Spider's Den", desc = "Expands Spider's Den targets.")
        @ConfigEditorAccordion(id = 84)
        @ConfigEditorBoolean
        public boolean spidersDenFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 84)
        public List<MobVisual.SpidersDen> targetsSpidersDen = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Spider's Den mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 84)
        public transient Runnable enableAllSpidersDen = () -> setAll(targetsSpidersDen, MobVisual.SpidersDen.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 84)
        public transient Runnable disableAllSpidersDen = () -> targetsSpidersDen.clear();

        @Expose
        @ConfigOption(name = "Crimson Isle", desc = "Expands Crimson Isle targets.")
        @ConfigEditorAccordion(id = 85)
        @ConfigEditorBoolean
        public boolean crimsonIsleFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 85)
        public List<MobVisual.CrimsonIsle> targetsCrimsonIsle = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Crimson Isle mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 85)
        public transient Runnable enableAllCrimsonIsle = () -> setAll(targetsCrimsonIsle, MobVisual.CrimsonIsle.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 85)
        public transient Runnable disableAllCrimsonIsle = () -> targetsCrimsonIsle.clear();

        @Expose
        @ConfigOption(name = "Crystal Hollows", desc = "Expands Crystal Hollows targets.")
        @ConfigEditorAccordion(id = 93)
        @ConfigEditorBoolean
        public boolean crystalHollowsFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 93)
        public List<MobVisual.CrystalHollows> targetsCrystalHollows = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Crystal Hollows mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 93)
        public transient Runnable enableAllCrystalHollows = () -> setAll(targetsCrystalHollows, MobVisual.CrystalHollows.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 93)
        public transient Runnable disableAllCrystalHollows = () -> targetsCrystalHollows.clear();

        @Expose
        @ConfigOption(name = "Moonglade Marsh", desc = "Expands Moonglade Marsh targets.")
        @ConfigEditorAccordion(id = 87)
        @ConfigEditorBoolean
        public boolean moongladeMarshFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 87)
        public List<MobVisual.MoongladeMarsh> targetsMoongladeMarsh = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Moonglade Marsh mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 87)
        public transient Runnable enableAllMoongladeMarsh = () -> setAll(targetsMoongladeMarsh, MobVisual.MoongladeMarsh.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 87)
        public transient Runnable disableAllMoongladeMarsh = () -> targetsMoongladeMarsh.clear();

        @Expose
        @ConfigOption(name = "Torrhus Canyon", desc = "Expands Torrhus Canyon targets.")
        @ConfigEditorAccordion(id = 88)
        @ConfigEditorBoolean
        public boolean torrhusCanyonFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 88)
        public List<MobVisual.TorrhusCanyon> targetsTorrhusCanyon = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Torrhus Canyon mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 88)
        public transient Runnable enableAllTorrhusCanyon = () -> setAll(targetsTorrhusCanyon, MobVisual.TorrhusCanyon.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 88)
        public transient Runnable disableAllTorrhusCanyon = () -> targetsTorrhusCanyon.clear();

        @Expose
        @ConfigOption(name = "Lotus Atoll", desc = "Expands Lotus Atoll targets.")
        @ConfigEditorAccordion(id = 95)
        @ConfigEditorBoolean
        public boolean lotusAtollFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 95)
        public List<MobVisual.LotusAtoll> targetsLotusAtoll = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Lotus Atoll mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 95)
        public transient Runnable enableAllLotusAtoll = () -> setAll(targetsLotusAtoll, MobVisual.LotusAtoll.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 95)
        public transient Runnable disableAllLotusAtoll = () -> targetsLotusAtoll.clear();

        @Expose
        @ConfigOption(name = "Critter Safari", desc = "Expands Critter Safari areas.")
        @ConfigEditorAccordion(id = 92)
        @ConfigEditorBoolean
        public boolean critterSafariFolder = false;

        @Expose
        @ConfigOption(name = "Hide Captured Critters", desc = "Drops critters already captured from Highlight, Tracer and Nameplate.\nThe Captured Critters HUD keeps showing them.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 92)
        public boolean hideCapturedCritters = false;

        @Expose
        @ConfigOption(name = "Cavern", desc = "Expands Cavern targets.")
        @ConfigAccordionId(id = 92)
        @ConfigEditorAccordion(id = 86)
        @ConfigEditorBoolean
        public boolean safariCavernFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 86)
        public List<MobVisual.SafariCavern> targetsSafariCavern = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Cavern mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 86)
        public transient Runnable enableAllSafariCavern = () -> setAll(targetsSafariCavern, MobVisual.SafariCavern.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 86)
        public transient Runnable disableAllSafariCavern = () -> targetsSafariCavern.clear();

        @Expose
        @ConfigOption(name = "Forest", desc = "Expands Forest targets.")
        @ConfigAccordionId(id = 92)
        @ConfigEditorAccordion(id = 89)
        @ConfigEditorBoolean
        public boolean safariForestFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 89)
        public List<MobVisual.SafariForest> targetsSafariForest = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Forest mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 89)
        public transient Runnable enableAllSafariForest = () -> setAll(targetsSafariForest, MobVisual.SafariForest.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 89)
        public transient Runnable disableAllSafariForest = () -> targetsSafariForest.clear();

        @Expose
        @ConfigOption(name = "Haunted", desc = "Expands Haunted targets.")
        @ConfigAccordionId(id = 92)
        @ConfigEditorAccordion(id = 90)
        @ConfigEditorBoolean
        public boolean safariHauntedFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 90)
        public List<MobVisual.SafariHaunted> targetsSafariHaunted = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Haunted mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 90)
        public transient Runnable enableAllSafariHaunted = () -> setAll(targetsSafariHaunted, MobVisual.SafariHaunted.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 90)
        public transient Runnable disableAllSafariHaunted = () -> targetsSafariHaunted.clear();

        @Expose
        @ConfigOption(name = "Icy", desc = "Expands Icy targets.")
        @ConfigAccordionId(id = 92)
        @ConfigEditorAccordion(id = 91)
        @ConfigEditorBoolean
        public boolean safariIcyFolder = false;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 91)
        public List<MobVisual.SafariIcy> targetsSafariIcy = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Icy mob into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 91)
        public transient Runnable enableAllSafariIcy = () -> setAll(targetsSafariIcy, MobVisual.SafariIcy.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 91)
        public transient Runnable disableAllSafariIcy = () -> targetsSafariIcy.clear();

        @Expose
        @ConfigOption(name = "Sea Creatures", desc = "Expands Sea Creature targets.\n"
                + "§eLegendary and Mythic tiers, plus the Special ones.")
        @ConfigEditorAccordion(id = 94)
        @ConfigEditorBoolean
        public boolean seaCreatureFolder = false;

        @Expose
        @ConfigOption(name = "Spawn Title", desc = "Shows a title with the name when a listed Sea Creature is found.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 94)
        public boolean enableSeaCreatureTitle = true;

        @Expose
        @ConfigOption(name = "Targets", desc = "Mobs to show. Applies to highlight, tracer and nameplate.")
        @ConfigEditorDraggableList
        @ConfigAccordionId(id = 94)
        public List<MobVisual.SeaCreature> targetsSeaCreature = new ArrayList<>();

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigOption(name = "Enable All", desc = "Puts every Sea Creature into the list above.")
        @ConfigEditorButton(buttonText = "All")
        @ConfigAccordionId(id = 94)
        public transient Runnable enableAllSeaCreature = () -> setAll(targetsSeaCreature, MobVisual.SeaCreature.values());

        @ConfigOption(name = "Disable All", desc = "Empties the list above.")
        @ConfigEditorButton(buttonText = "None")
        @ConfigAccordionId(id = 94)
        public transient Runnable disableAllSeaCreature = () -> targetsSeaCreature.clear();

        /** Tracer をどのモブに出すか。表示名はそのまま設定画面の選択肢になる */
        public enum TracerMode {
            NEAREST("Nearest"),
            ALL("All");

            private final String label;

            TracerMode(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        // ドラッグリストは設定画面が同じインスタンスを見ているため、
        // 新しいリストに差し替えず、中身だけ入れ替える
        private static <T> void setAll(List<T> list, T[] values) {
            list.clear();
            list.addAll(java.util.Arrays.asList(values));
        }

    }

    public static class MiscCategory {
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 50)
        @ConfigEditorBoolean
        public boolean hudFolder = false;

        @Expose
        @ConfigOption(name = "Armor HUD", desc = "Expands armor HUD settings.")
        @ConfigEditorAccordion(id = 51)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean armorHudFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Shows equipped armor.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 51)
        public boolean showEquipmentHud = true;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Sets horizontal or vertical.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 51)
        public HudOrientation equipmentHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Equipment HUD", desc = "Expands equipment HUD settings.")
        @ConfigEditorAccordion(id = 52)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean equipmentHudFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Shows Necklace/Cloak/Belt/Gloves.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 52)
        public boolean showGearHud = true;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Sets horizontal or vertical.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 52)
        public HudOrientation gearHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Yaw and Pitch HUD", desc = "Expands yaw and pitch HUD settings.")
        @ConfigEditorAccordion(id = 54)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean yawPitchHudFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Shows where you are looking.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 54)
        public boolean showYawPitchHud = false;

        @Expose
        @ConfigOption(name = "Yaw Precision", desc = "Sets decimals shown for yaw.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
        @ConfigAccordionId(id = 54)
        public int yawPrecision = 4;

        @Expose
        @ConfigOption(name = "Pitch Precision", desc = "Sets decimals shown for pitch.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
        @ConfigAccordionId(id = 54)
        public int pitchPrecision = 4;

        @Expose
        @ConfigOption(name = "Pet HUD", desc = "Shows active pet.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showPetHud = true;

        @Expose
        @ConfigOption(name = "TPS HUD", desc = "Shows server TPS.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showTpsHud = true;

        @Expose
        @ConfigOption(name = "Day HUD", desc = "Shows lobby day.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showDayHud = true;

        @Expose
        @ConfigOption(name = "Armor Stack HUD", desc = "Shows armor stack counts.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showArmorStackHud = true;

        @Expose
        @ConfigOption(name = "Ferocity HUD", desc = "Shows ferocity. Hidden while it cannot be read.\n"
                + "§eNeeds the Ferocity Stats Widget.\n"
                + "§e(/widget -> Stats Widget -> Enable Ferocity)")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showFerocityHud = false;

        @Expose
        @ConfigOption(name = "Quiver HUD", desc = "Shows selected arrow and how many are left.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showQuiverHud = true;

        @Expose
        @ConfigOption(name = "Keybind Settings", desc = "Expands menu keybind settings.")
        @ConfigEditorAccordion(id = 59)
        @ConfigEditorBoolean
        public boolean keybindFolder = false;

        @Expose
        @ConfigOption(name = "Open Menu Keybind", desc = "Expands menu opening keybind settings.")
        @ConfigEditorAccordion(id = 63)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 59)
        public boolean openMenuKeybindFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Opens menus via configured keys.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 63)
        public boolean enableOpenMenuKeybind = false;

        @Expose
        @ConfigOption(name = "Loadouts", desc = "Sets key for /loadouts.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        @ConfigAccordionId(id = 63)
        public int openLoadoutsKeybind = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Wardrobe", desc = "Sets key for /wardrobe.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        @ConfigAccordionId(id = 63)
        public int openWardrobeKeybind = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Equipment", desc = "Sets key for /equipment.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        @ConfigAccordionId(id = 63)
        public int openEquipmentKeybind = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Loadouts Keybind", desc = "Expands per-slot keybind settings.")
        @ConfigEditorAccordion(id = 60)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 59)
        public boolean loadoutsKeybindFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Switches Loadouts via configured keys.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 60)
        public boolean enableLoadoutsKeybind = false;

        @Expose
        @ConfigOption(name = "Slot 1", desc = "Sets key for slot 1.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot1 = GLFW.GLFW_KEY_1;

        @Expose
        @ConfigOption(name = "Slot 2", desc = "Sets key for slot 2.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot2 = GLFW.GLFW_KEY_2;

        @Expose
        @ConfigOption(name = "Slot 3", desc = "Sets key for slot 3.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot3 = GLFW.GLFW_KEY_3;

        @Expose
        @ConfigOption(name = "Slot 4", desc = "Sets key for slot 4.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot4 = GLFW.GLFW_KEY_4;

        @Expose
        @ConfigOption(name = "Slot 5", desc = "Sets key for slot 5.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot5 = GLFW.GLFW_KEY_5;

        @Expose
        @ConfigOption(name = "Slot 6", desc = "Sets key for slot 6.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot6 = GLFW.GLFW_KEY_6;

        @Expose
        @ConfigOption(name = "Slot 7", desc = "Sets key for slot 7.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot7 = GLFW.GLFW_KEY_7;

        @Expose
        @ConfigOption(name = "Slot 8", desc = "Sets key for slot 8.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot8 = GLFW.GLFW_KEY_8;

        @Expose
        @ConfigOption(name = "Slot 9", desc = "Sets key for slot 9.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot9 = GLFW.GLFW_KEY_9;

        @Expose
        @ConfigOption(name = "Slot 10", desc = "Sets key for slot 10.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_0)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot10 = GLFW.GLFW_KEY_0;

        @Expose
        @ConfigOption(name = "Slot 11", desc = "Sets key for slot 11.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_MINUS)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot11 = GLFW.GLFW_KEY_MINUS;

        @Expose
        @ConfigOption(name = "Slot 12", desc = "Sets key for slot 12.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_EQUAL)
        @ConfigAccordionId(id = 60)
        public int loadoutsKeybindSlot12 = GLFW.GLFW_KEY_EQUAL;

        @Expose
        @ConfigOption(name = "Armor Set Keybind", desc = "Expands per-slot keybind settings.")
        @ConfigEditorAccordion(id = 61)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 59)
        public boolean armorSetKeybindFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Switches Armor Sets via configured keys.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 61)
        public boolean enableArmorSetKeybind = false;

        @Expose
        @ConfigOption(name = "Slot 1", desc = "Sets key for slot 1.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot1 = GLFW.GLFW_KEY_1;

        @Expose
        @ConfigOption(name = "Slot 2", desc = "Sets key for slot 2.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot2 = GLFW.GLFW_KEY_2;

        @Expose
        @ConfigOption(name = "Slot 3", desc = "Sets key for slot 3.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot3 = GLFW.GLFW_KEY_3;

        @Expose
        @ConfigOption(name = "Slot 4", desc = "Sets key for slot 4.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot4 = GLFW.GLFW_KEY_4;

        @Expose
        @ConfigOption(name = "Slot 5", desc = "Sets key for slot 5.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot5 = GLFW.GLFW_KEY_5;

        @Expose
        @ConfigOption(name = "Slot 6", desc = "Sets key for slot 6.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot6 = GLFW.GLFW_KEY_6;

        @Expose
        @ConfigOption(name = "Slot 7", desc = "Sets key for slot 7.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot7 = GLFW.GLFW_KEY_7;

        @Expose
        @ConfigOption(name = "Slot 8", desc = "Sets key for slot 8.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot8 = GLFW.GLFW_KEY_8;

        @Expose
        @ConfigOption(name = "Slot 9", desc = "Sets key for slot 9.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9)
        @ConfigAccordionId(id = 61)
        public int armorSetKeybindSlot9 = GLFW.GLFW_KEY_9;

        @Expose
        @ConfigOption(name = "Equipment Set Keybind", desc = "Expands per-slot keybind settings.")
        @ConfigEditorAccordion(id = 62)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 59)
        public boolean equipmentSetKeybindFolder = false;

        @Expose
        @ConfigOption(name = "Enable", desc = "Switches Equipment Sets via configured keys.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 62)
        public boolean enableEquipmentSetKeybind = false;

        @Expose
        @ConfigOption(name = "Slot 1", desc = "Sets key for slot 1.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot1 = GLFW.GLFW_KEY_1;

        @Expose
        @ConfigOption(name = "Slot 2", desc = "Sets key for slot 2.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot2 = GLFW.GLFW_KEY_2;

        @Expose
        @ConfigOption(name = "Slot 3", desc = "Sets key for slot 3.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot3 = GLFW.GLFW_KEY_3;

        @Expose
        @ConfigOption(name = "Slot 4", desc = "Sets key for slot 4.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot4 = GLFW.GLFW_KEY_4;

        @Expose
        @ConfigOption(name = "Slot 5", desc = "Sets key for slot 5.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot5 = GLFW.GLFW_KEY_5;

        @Expose
        @ConfigOption(name = "Slot 6", desc = "Sets key for slot 6.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot6 = GLFW.GLFW_KEY_6;

        @Expose
        @ConfigOption(name = "Slot 7", desc = "Sets key for slot 7.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot7 = GLFW.GLFW_KEY_7;

        @Expose
        @ConfigOption(name = "Slot 8", desc = "Sets key for slot 8.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot8 = GLFW.GLFW_KEY_8;

        @Expose
        @ConfigOption(name = "Slot 9", desc = "Sets key for slot 9.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9)
        @ConfigAccordionId(id = 62)
        public int equipmentSetKeybindSlot9 = GLFW.GLFW_KEY_9;

        @Expose
        @ConfigOption(name = "Held Item Size", desc = "Expands size and position settings.")
        @ConfigEditorAccordion(id = 53)
        @ConfigEditorBoolean
        public boolean heldItemFolder = false;

        @Expose
        @ConfigOption(name = "Size", desc = "Changes held item size.")
        @ConfigEditorSlider(minValue = 0.01f, maxValue = 1.0f, minStep = 0.01f)
        @ConfigAccordionId(id = 53)
        public float heldItemScale = 1.0f;

        // ボタンには @Expose は付けず、代わりに transient を付けます！
        @ConfigOption(name = "Reset Size", desc = "Reset to default.")
        @ConfigEditorButton(buttonText = "Reset")
        @ConfigAccordionId(id = 53)
        public transient Runnable resetHeldItemScale = () -> heldItemScale = 1.0f;

        @Expose
        @ConfigOption(name = "X", desc = "Shifts item horizontally.")
        @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f)
        @ConfigAccordionId(id = 53)
        public float heldItemOffsetX = 0.0f;

        @ConfigOption(name = "Reset X", desc = "Reset to default.")
        @ConfigEditorButton(buttonText = "Reset")
        @ConfigAccordionId(id = 53)
        public transient Runnable resetHeldItemOffsetX = () -> heldItemOffsetX = 0.0f;

        @Expose
        @ConfigOption(name = "Y", desc = "Shifts item vertically.")
        @ConfigEditorSlider(minValue = -1.0f, maxValue = 1.0f, minStep = 0.01f)
        @ConfigAccordionId(id = 53)
        public float heldItemOffsetY = 0.0f;

        @ConfigOption(name = "Reset Y", desc = "Reset to default.")
        @ConfigEditorButton(buttonText = "Reset")
        @ConfigAccordionId(id = 53)
        public transient Runnable resetHeldItemOffsetY = () -> heldItemOffsetY = 0.0f;

        @Expose
        @ConfigOption(name = "Hide Damage Splash", desc = "Hides the damage numbers popping off mobs.")
        @ConfigEditorBoolean
        public boolean hideDamageSplash = false;

        @Expose
        @ConfigOption(name = "Low Quiver Alert", desc = "Warns with a title and sound at 50 and 10 arrows left.")
        @ConfigEditorBoolean
        public boolean enableQuiverAlert = true;

        @Expose
        @ConfigOption(name = "Arrow Poison Indicator", desc = "Shows arrow poison uses left.")
        @ConfigEditorBoolean
        public boolean showPoisonIndicator = true;

        @Expose
        @ConfigOption(name = "Server Reboot Alert", desc = "Warns of lobby restart.")
        @ConfigEditorBoolean
        public boolean enableRebootAlert = true;

        @Expose
        @ConfigOption(name = "Warp Cooldown Queue", desc = "Shows cooldown, queues /warp.")
        @ConfigEditorBoolean
        public boolean enableWarpQueue = true;

        @Expose
        @ConfigOption(name = "Keep Cursor Position", desc = "Prevents cursor reset on quick swap.")
        @ConfigEditorBoolean
        public boolean enableCursorRestoreOnRapidReopen = true;
    }
}
