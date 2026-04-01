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
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Optional;

public class ModConfig extends Config {

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
        if (INSTANCE.gui == null) {
            INSTANCE.gui = new GuiCategory();
        }
        INSTANCE.gui.openHudEditor = () -> {
            MinecraftClient.getInstance().send(() -> {
                MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            });
        };

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
    @Category(name = "GUI", desc = "Edit HUD locations and GUI settings.")
    public GuiCategory gui = new GuiCategory();

    @Expose
    @Category(name = "End Stone Protector", desc = "Settings for End Stone Protector features.")
    public GolemCategory golem = new GolemCategory();

    @Expose
    @Category(name = "Dragon", desc = "Settings for Dragon features.")
    public DragonCategory dragon = new DragonCategory();

    @Expose
    @Category(name = "Broodmother", desc = "Settings for Broodmother features.")
    public BroodmotherCategory broodmother = new BroodmotherCategory();

    @Expose
    @Category(name = "Misc", desc = "Settings for Miscellaneous features.")
    public MiscCategory misc = new MiscCategory();


    // ==========================================
    // 各カテゴリの中身（設定項目）
    // ==========================================
    // ★ 新しくGUIカテゴリの中身を追加
    public static class GuiCategory {
        // ボタンには @Expose は付けず、代わりに transient を付けます！
        @ConfigOption(name = "Edit GUI Locations", desc = "Click to move HUD elements on your screen.")
        @ConfigEditorButton(buttonText = "Open")
        public transient Runnable openHudEditor = () -> {
            // ボタンが押されたら、マイクラの画面をHudEditorScreenに切り替える
            MinecraftClient.getInstance().send(() -> {
                MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            });
        };
    }

    public static class GolemCategory {
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 0) // ★ 新しいグループなので ID を 1 にします
        @ConfigEditorBoolean
        public boolean hudFolder = false; // ★ 機能を持たないダミー変数

        // ▼ これ以降は「HUD Settings」の中に収納される子要素 ▼

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Golem Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0) // ★ 親と同じ ID 1 で紐付け！
        public boolean showGolemStatusHud = true;

        // ★追加: GolemのSince S4タイマー用スイッチ
        @Expose
        @ConfigOption(name = "Show Since S4", desc = "Toggles the Since S4 timer in the Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0) // 同じHUDアコーディオン(id=0)に収納
        public boolean showGolemStatusHud_SinceS4 = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Toggles the Golem Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0) // ★ 親と同じ ID 1 で紐付け！
        public boolean showLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Golem Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0) // ★ 親と同じ ID 1 で紐付け！
        public boolean showGolemHealthHud = true;

        // ==========================================
        // ★ アコーディオン（折り畳み）の「親」
        // ==========================================
        @Expose
        @ConfigOption(name = "World Location Display", desc = "Expand to configure Text and Beacon settings.")
        @ConfigEditorAccordion(id = 1) // ★ 親のIDを「0」とする
        public boolean worldLocationFolder = false;

        // ==========================================
        // ★ アコーディオンの中に収納される「子」
        // ==========================================
        @Expose
        @ConfigOption(name = "Show Text", desc = "Toggles the 3D floating text (e.g. GOLEM (Spawned)).")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1) // ★ 親と同じID「0」を指定して紐付ける！
        public boolean showGolemWorldLocation_Text = true;

        @Expose
        @ConfigOption(name = "Show Beacon Beam", desc = "Toggles the beacon beam light.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1) // ★ 親と同じID「0」を指定して紐付ける！
        public boolean showGolemWorldLocation_Beacon = true;

        // ==========================================
        // ★ 新設：Stage 4 Alert の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expand to configure Stage 4 alerts.")
        @ConfigEditorAccordion(id = 2) // ★ 新しいID「2」
        @ConfigEditorBoolean
        public boolean stage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Golem's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays an Iron Golem hurt sound when the Golem's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Sound = true;

        // ==========================================
        // ★ 新設：Stage 5 Alert の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expand to configure Stage 5 alerts.")
        @ConfigEditorAccordion(id = 3) // ★ 新しいID「3」
        @ConfigEditorBoolean
        public boolean stage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Golem's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays an anvil land sound when the Golem's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Sound = true;

        // ==========================================
        // ★ 新設：Chat 関連の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expand to configure chat messages and alerts.")
        @ConfigEditorAccordion(id = 4) // ★ 新しいID「4」を割り当て
        @ConfigEditorBoolean
        public boolean chatFolder = false;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows the time it took for the Golem to reach stage 5 in the chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4) // ★ 子要素をID 4に紐付け
        public boolean showStage4Duration = true;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows the DPS you and the top 3 players have dealt to the Golem after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4) // ★ 子要素をID 4に紐付け
        public boolean showDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows your Golem's loot quality after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4) // ★ 子要素をID 4に紐付け
        public boolean showLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Day 30+ Alert Chat", desc = "Alerts if the Golem's stage is 4 and the lobby's day is 30 or higher.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4) // ★ 子要素をID 4に紐付け
        public boolean enableDay30Alert = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Toggles the notification for rare drops from the Golem in title and chat.")
        @ConfigEditorBoolean
        public boolean enableDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Highlights the Golem through walls.")
        @ConfigEditorBoolean
        public boolean enableGolemHighlight = true;
    }

    public static class DragonCategory {
        // ★ HUD アコーディオン
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure Dragon HUD elements.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean hudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Dragon Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showDragonStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Toggles the Dragon Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showDragonTrackerHud = true;

        // ==========================================
        // ★ 新設：Spawn Title Alerts の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Spawn Alert Title ", desc = "Expand to configure spawn alerts per dragon type.")
        @ConfigEditorAccordion(id = 1) // ★ 新しいID「2」
        @ConfigEditorBoolean
        public boolean spawnTitleFolder = false;

        @Expose
        @ConfigOption(name = "Protector", desc = "Toggles title alert for Protector Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Protector = true;

        @Expose
        @ConfigOption(name = "Old", desc = "Toggles title alert for Old Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Old = true;

        @Expose
        @ConfigOption(name = "Unstable", desc = "Toggles title alert for Unstable Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Unstable = true;

        @Expose
        @ConfigOption(name = "Young", desc = "Toggles title alert for Young Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Young = true;

        @Expose
        @ConfigOption(name = "Strong", desc = "Toggles title alert for Strong Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Strong = true;

        @Expose
        @ConfigOption(name = "Wise", desc = "Toggles title alert for Wise Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Wise = true;

        @Expose
        @ConfigOption(name = "Superior", desc = "Toggles title alert for Superior Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableDragonAlert_Superior = true;

        // ★ Chat アコーディオン
        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expand to configure Dragon chat messages.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        public boolean chatFolder = false;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows the DPS you and the top 3 players have dealt to the Dragon after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showDragonDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows your Dragon's Loot Quality after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showDragonLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Toggles the notification for rare drops from the Dragon in title and chat.")
        @ConfigEditorBoolean
        public boolean enableDragonDropAlerts = true;
    }

    public static class BroodmotherCategory {
        // ★ HUD アコーディオン
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure Broodmother HUD elements.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean hudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Broodmother Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBroodmotherStatusHud = true;

        // ★追加: BroodmotherのSince S4タイマー用スイッチ
        @Expose
        @ConfigOption(name = "Show Since S4", desc = "Toggles the Since S4 timer in the Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0) // 同じHUDアコーディオン(id=0)に収納
        public boolean showBroodmotherStatusHud_SinceS4 = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Broodmother Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBroodmotherHealthHud = true;

        // ==========================================
        // ★ 新設：Stage 4 Alert の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expand to configure Stage 4 (Soon) alerts.")
        @ConfigEditorAccordion(id = 1) // ★ 新しいID「1」
        @ConfigEditorBoolean
        public boolean stage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Broodmother's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays a Creeper hurt sound when the Broodmother's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean enableStage4Sound = true;

        // ==========================================
        // ★ 新設：Stage 5 Alert の折り畳み
        // ==========================================
        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expand to configure Stage 5 (Spawned) alerts.")
        @ConfigEditorAccordion(id = 2) // ★ 新しいID「2」
        @ConfigEditorBoolean
        public boolean stage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Broodmother's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays a Zombie breaks door sound when the Broodmother's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage5Sound = true;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows the time it took for the Broodmother to reach stage 5 in the chat.")
        @ConfigEditorBoolean
        public boolean showBroodmotherStage4Duration = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Highlights the Broodmother through walls.")
        @ConfigEditorBoolean
        public boolean enableBroodmotherHighlight = true;
    }

    public static class MiscCategory {
        @Expose
        @ConfigOption(name = "Active Pet HUD", desc = "Shows your currently active pet.")
        @ConfigEditorBoolean
        public boolean showPetHud = false;

        @Expose
        @ConfigOption(name = "Day HUD", desc = "Shows the current lobby's age.")
        @ConfigEditorBoolean
        public boolean showDayHud = false;

        @Expose
        @ConfigOption(name = "Armor Stack HUD", desc = "Shows the number of armor stacks for Crimson, Terror, Aurora, Hollow, and Fervor.")
        @ConfigEditorBoolean
        public boolean showArmorStackHud = false;

        @Expose
        @ConfigOption(name = "Arrow Poison Indicator", desc = "Shows Toxic/Twilight Arrow Poison uses left on your bows in the hotbar.")
        @ConfigEditorBoolean
        public boolean showPoisonIndicator = false;

        @Expose
        @ConfigOption(name = "Server Reboot Alert", desc = "Warning with sound and title when the current lobby restarts.")
        @ConfigEditorBoolean
        public boolean enableRebootAlert = true;
    }
}