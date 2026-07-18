package com.deeply.gankura.data;

import com.deeply.gankura.render.HudEditorScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        INSTANCE.gui.openHudEditor = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
            });
        };
        INSTANCE.misc.resetHeldItemScale = () -> INSTANCE.misc.heldItemScale = 1.0f;
        INSTANCE.misc.resetHeldItemOffsetX = () -> INSTANCE.misc.heldItemOffsetX = 0.0f;
        INSTANCE.misc.resetHeldItemOffsetY = () -> INSTANCE.misc.heldItemOffsetY = 0.0f;

        // 起動時やエラー発生時に、確実に現在の設定をJSON形式でファイルに書き込んでおく
        INSTANCE.saveNow();
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
                Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
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
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemHealthHud = true;

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
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows stage 4→5 duration.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showStage4Duration = true;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows DPS results in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows loot quality in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Day 30+ Alert Chat", desc = "Alerts on day 30+.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean enableDay30Alert = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expands highlight settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Golem.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Golem.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemTracer = true;

        // ========== Dragon section ==========
        @Expose
        @ConfigOption(name = "Dragon", desc = "Expands Dragon settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean dragonSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 11)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonTrackerHud = true;

        @Expose
        @ConfigOption(name = "Spawn Alert Title", desc = "Expands spawn alerts.")
        @ConfigEditorAccordion(id = 12)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean spawnTitleFolder = false;

        @Expose
        @ConfigOption(name = "Protector", desc = "Shows Protector Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Protector = true;

        @Expose
        @ConfigOption(name = "Old", desc = "Shows Old Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Old = true;

        @Expose
        @ConfigOption(name = "Unstable", desc = "Shows Unstable Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Unstable = true;

        @Expose
        @ConfigOption(name = "Young", desc = "Shows Young Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Young = true;

        @Expose
        @ConfigOption(name = "Strong", desc = "Shows Strong Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Strong = true;

        @Expose
        @ConfigOption(name = "Wise", desc = "Shows Wise Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Wise = true;

        @Expose
        @ConfigOption(name = "Superior", desc = "Shows Superior Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Superior = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expands chat messages.")
        @ConfigEditorAccordion(id = 13)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonChatFolder = false;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows DPS results in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows loot quality in chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableDragonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expands highlight settings.")
        @ConfigEditorAccordion(id = 14)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 14)
        public boolean enableDragonHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 14)
        public boolean enableDragonTracer = true;
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
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherStatusHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherHealthHud = true;

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
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows stage 4→5 duration.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBroodmotherStage4Duration = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expands highlight settings.")
        @ConfigEditorAccordion(id = 4)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Broodmother.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Broodmother.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherTracer = true;

        @Expose
        @ConfigOption(name = "Arachne", desc = "Expands Arachne settings.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        public boolean arachneSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean arachneHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows spawn countdown.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean showArachneStatusHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean showArachneHealthHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Shows floating text at altar.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showArachneWorldText = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expands highlight settings.")
        @ConfigEditorAccordion(id = 7)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean arachneHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Arachne.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 7)
        public boolean enableArachneHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Arachne.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 7)
        public boolean enableArachneTracer = true;
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
        @ConfigOption(name = "World Location Display", desc = "Shows floating text at spawns.")
        @ConfigEditorBoolean
        public boolean showCrimsonIsleWorldText = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Shows rare drop alert.")
        @ConfigEditorBoolean
        public boolean enableCrimsonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Shows loot tracker HUD.")
        @ConfigEditorBoolean
        public boolean showCrimsonLootTrackerHud = true;

        // ---- Barbarian Duke X (id: 0) ----
        @Expose
        @ConfigOption(name = "Barbarian Duke X", desc = "Expands Barbarian Duke X settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean barbarianSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBarbarianHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Barbarian Duke X.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableBarbarianHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Barbarian Duke X.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableBarbarianTracer = true;

        // ---- Bladesoul (id: 10) ----
        @Expose
        @ConfigOption(name = "Bladesoul", desc = "Expands Bladesoul settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean bladesoulSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean showBladesoulHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Bladesoul.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableBladesoulHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Bladesoul.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableBladesoulTracer = true;

        // ---- Mage Outlaw (id: 20) ----
        @Expose
        @ConfigOption(name = "Mage Outlaw", desc = "Expands Mage Outlaw settings.")
        @ConfigEditorAccordion(id = 20)
        @ConfigEditorBoolean
        public boolean mageOutlawSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean showMageOutlawHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Mage Outlaw.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean enableMageOutlawHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Mage Outlaw.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean enableMageOutlawTracer = true;

        // ---- Ashfang (id: 30, 33) ----
        @Expose
        @ConfigOption(name = "Ashfang", desc = "Expands Ashfang settings.")
        @ConfigEditorAccordion(id = 30)
        @ConfigEditorBoolean
        public boolean ashfangSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean showAshfangHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Ashfang.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean enableAshfangHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Ashfang.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean enableAshfangTracer = true;

        @Expose
        @ConfigOption(name = "Follower Highlight", desc = "Expands follower highlight settings.")
        @ConfigEditorAccordion(id = 33)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean ashfangFollowerHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Follower Glowing", desc = "Highlights Followers.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerHighlight = true;

        @Expose
        @ConfigOption(name = "Follower Tracer", desc = "Draws tracer to Followers.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerTracer = true;

        @Expose
        @ConfigOption(name = "Acolyte Glowing", desc = "Highlights Acolytes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteHighlight = true;

        @Expose
        @ConfigOption(name = "Acolyte Tracer", desc = "Draws tracer to Acolytes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteTracer = true;

        @Expose
        @ConfigOption(name = "Underling Glowing", desc = "Highlights Underlings.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingHighlight = true;

        @Expose
        @ConfigOption(name = "Underling Tracer", desc = "Draws tracer to Underlings.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingTracer = true;

        // ---- Magma Boss (id: 40) ----
        @Expose
        @ConfigOption(name = "Magma Boss", desc = "Expands Magma Boss settings.")
        @ConfigEditorAccordion(id = 40)
        @ConfigEditorBoolean
        public boolean magmaBossSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Shows HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean showMagmaBossHealthHud = true;

        @Expose
        @ConfigOption(name = "Stage Status Title", desc = "Shows stage status title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossSpawnTitle = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Magma Boss.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws tracer to Magma Boss.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossTracer = true;
    }

    public static class MiscCategory {
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expands HUD settings.")
        @ConfigEditorAccordion(id = 50)
        @ConfigEditorBoolean
        public boolean hudFolder = false;

        @Expose
        @ConfigOption(name = "Pet HUD", desc = "Shows active pet.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showPetHud = false;

        @Expose
        @ConfigOption(name = "TPS HUD", desc = "Shows server TPS.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showTpsHud = false;

        @Expose
        @ConfigOption(name = "Day HUD", desc = "Shows lobby day.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showDayHud = false;

        @Expose
        @ConfigOption(name = "Armor HUD", desc = "Shows equipped armor.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showEquipmentHud = false;

        @Expose
        @ConfigOption(name = "Armor HUD Orientation", desc = "Expands armor HUD layout.")
        @ConfigEditorAccordion(id = 51)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean armorOrientationFolder = false;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Sets horizontal or vertical.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 51)
        public HudOrientation equipmentHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Equipment HUD", desc = "Shows Necklace/Cloak/Belt/Gloves.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showGearHud = false;

        @Expose
        @ConfigOption(name = "Equipment HUD Orientation", desc = "Expands equipment HUD layout.")
        @ConfigEditorAccordion(id = 52)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean gearOrientationFolder = false;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Sets horizontal or vertical.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 52)
        public HudOrientation gearHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Armor Stack HUD", desc = "Shows armor stack counts.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showArmorStackHud = false;

        @Expose
        @ConfigOption(name = "Arrow Poison Indicator", desc = "Shows arrow poison uses left.")
        @ConfigEditorBoolean
        public boolean showPoisonIndicator = false;

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

        @Expose
        @ConfigOption(name = "Set Keybind", desc = "Switches Loadouts/Armor/Equipment Sets via number keys.")
        @ConfigEditorBoolean
        public boolean enableSetKeybind = true;

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
    }
}
