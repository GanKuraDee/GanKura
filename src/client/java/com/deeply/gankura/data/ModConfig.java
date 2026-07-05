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

    public enum HudOrientation {
        HORIZONTAL, VERTICAL;

        @Override
        public String toString() {
            return this == HORIZONTAL ? "Horizontal" : "Vertical";
        }
    }

    public static ModConfig INSTANCE = new ModConfig();

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private static File getConfigFile() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "gankura");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "gankura_config.properties");
    }

    public static void load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (Exception e) {
                System.err.println("[GanKura] Old config format detected or file corrupted. Overwriting with JSON...");
            }
        }

        if (INSTANCE.gui == null) INSTANCE.gui = new GuiCategory();
        if (INSTANCE.theEnd == null) INSTANCE.theEnd = new TheEndCategory();
        if (INSTANCE.spidersDen == null) INSTANCE.spidersDen = new SpidersDenCategory();
        if (INSTANCE.crimsonIsle == null) INSTANCE.crimsonIsle = new CrimsonIsleCategory();

        INSTANCE.gui.openHudEditor = () -> {
            MinecraftClient.getInstance().send(() -> {
                MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            });
        };

        INSTANCE.saveNow();
    }

    @Override
    public StructuredText getTitle() {
        String version = getModVersion();
        return StructuredText.of("GanKura (Release: " + version + ") by GanKuraDee");
    }

    private String getModVersion() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("gankura");
        if (container.isPresent()) {
            return container.get().getMetadata().getVersion().getFriendlyString();
        }
        return "Unknown";
    }

    @Override
    public void saveNow() {
        try (FileWriter writer = new FileWriter(getConfigFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("Failed to save GanKura config!");
            e.printStackTrace();
        }
    }

    // ==========================================
    // カテゴリの定義
    // ==========================================
    @Expose
    @Category(name = "GUI", desc = "Edit HUD locations and GUI settings.")
    public GuiCategory gui = new GuiCategory();

    @Expose
    @Category(name = "The End", desc = "Settings for End Stone Protector and Dragon features.")
    public TheEndCategory theEnd = new TheEndCategory();

    @Expose
    @Category(name = "Spider's Den", desc = "Settings for Broodmother features.")
    public SpidersDenCategory spidersDen = new SpidersDenCategory();

    @Expose
    @Category(name = "Crimson Isle", desc = "Settings for Crimson Isle bosses.")
    public CrimsonIsleCategory crimsonIsle = new CrimsonIsleCategory();

    @Expose
    @Category(name = "Misc", desc = "Settings for Miscellaneous features.")
    public MiscCategory misc = new MiscCategory();


    // ==========================================
    // 各カテゴリの中身
    // ==========================================
    public static class GuiCategory {
        @ConfigOption(name = "Edit GUI Locations", desc = "Click to move HUD elements on your screen.")
        @ConfigEditorButton(buttonText = "Open")
        public transient Runnable openHudEditor = () -> {
            MinecraftClient.getInstance().send(() -> {
                MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            });
        };
    }

    // ==========================================
    // The End: End Stone Protector + Dragon
    // ==========================================
    public static class TheEndCategory {

        // ========== End Stone Protector section ==========
        @Expose
        @ConfigOption(name = "End Stone Protector", desc = "Expand to configure End Stone Protector settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean golemSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Golem Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Toggles the Golem Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Golem Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemHealthHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Expand to configure Text and Beacon settings.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean worldLocationFolder = false;

        @Expose
        @ConfigOption(name = "Show Text", desc = "Toggles the 3D floating text (e.g. GOLEM (Spawned)).")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Text = true;

        @Expose
        @ConfigOption(name = "Show Beacon Beam", desc = "Toggles the beacon beam light.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Beacon = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expand to configure Stage 4 alerts.")
        @ConfigEditorAccordion(id = 3)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean stage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Golem's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays an Iron Golem hurt sound when the Golem's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage4Sound = true;

        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expand to configure Stage 5 alerts.")
        @ConfigEditorAccordion(id = 4)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean stage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Golem's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays an anvil land sound when the Golem's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableStage5Sound = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expand to configure chat messages and alerts.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemChatFolder = false;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows the time it took for the Golem to reach stage 5 in the chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showStage4Duration = true;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows the DPS you and the top 3 players have dealt to the Golem after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows your Golem's loot quality after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Day 30+ Alert Chat", desc = "Alerts if the Golem's stage is 4 and the lobby's day is 30 or higher.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean enableDay30Alert = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Toggles the notification for rare drops from the Golem in title and chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights the Golem through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to the Golem.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemTracer = true;

        // ========== Dragon section ==========
        @Expose
        @ConfigOption(name = "Dragon", desc = "Expand to configure Dragon settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean dragonSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure Dragon HUD elements.")
        @ConfigEditorAccordion(id = 11)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Dragon Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Toggles the Dragon Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonTrackerHud = true;

        @Expose
        @ConfigOption(name = "Spawn Alert Title", desc = "Expand to configure spawn alerts per dragon type.")
        @ConfigEditorAccordion(id = 12)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean spawnTitleFolder = false;

        @Expose
        @ConfigOption(name = "Protector", desc = "Toggles title alert for Protector Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Protector = true;

        @Expose
        @ConfigOption(name = "Old", desc = "Toggles title alert for Old Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Old = true;

        @Expose
        @ConfigOption(name = "Unstable", desc = "Toggles title alert for Unstable Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Unstable = true;

        @Expose
        @ConfigOption(name = "Young", desc = "Toggles title alert for Young Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Young = true;

        @Expose
        @ConfigOption(name = "Strong", desc = "Toggles title alert for Strong Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Strong = true;

        @Expose
        @ConfigOption(name = "Wise", desc = "Toggles title alert for Wise Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Wise = true;

        @Expose
        @ConfigOption(name = "Superior", desc = "Toggles title alert for Superior Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Superior = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Expand to configure Dragon chat messages.")
        @ConfigEditorAccordion(id = 13)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonChatFolder = false;

        @Expose
        @ConfigOption(name = "DPS Chat", desc = "Shows the DPS you and the top 3 players have dealt to the Dragon after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonDpsChat = true;

        @Expose
        @ConfigOption(name = "Loot Quality Chat", desc = "Shows your Dragon's Loot Quality after the fight.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 13)
        public boolean showDragonLootQualityChat = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Toggles the notification for rare drops from the Dragon in title and chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableDragonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 14)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights the Dragon through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 14)
        public boolean enableDragonHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to the Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 14)
        public boolean enableDragonTracer = true;
    }

    // ==========================================
    // Spider's Den: Broodmother
    // ==========================================
    public static class SpidersDenCategory {

        // ========== Broodmother section ==========
        @Expose
        @ConfigOption(name = "Broodmother", desc = "Expand to configure Broodmother settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean broodmotherSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure Broodmother HUD elements.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Toggles the Broodmother Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherStatusHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Broodmother Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherHealthHud = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Expand to configure Stage 4 (Soon) alerts.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherStage4Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Broodmother's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays a Creeper hurt sound when the Broodmother's stage is 4.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableStage4Sound = true;

        @Expose
        @ConfigOption(name = "Stage 5 Alert", desc = "Expand to configure Stage 5 (Spawned) alerts.")
        @ConfigEditorAccordion(id = 3)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherStage5Folder = false;

        @Expose
        @ConfigOption(name = "Show Title", desc = "Shows a title on screen when the Broodmother's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Title = true;

        @Expose
        @ConfigOption(name = "Play Sound", desc = "Plays a Zombie breaks door sound when the Broodmother's stage is 5.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 3)
        public boolean enableStage5Sound = true;

        @Expose
        @ConfigOption(name = "Stage 4 Duration Chat", desc = "Shows the time it took for the Broodmother to reach stage 5 in the chat.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBroodmotherStage4Duration = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 4)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights the Broodmother through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to the Broodmother.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherTracer = true;
    }

    // ==========================================
    // Crimson Isle: Status HUD + 5 bosses
    // ==========================================
    public static class CrimsonIsleCategory {

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Shows Spawned/Unknown status for all Crimson Isle bosses.")
        @ConfigEditorBoolean
        public boolean showCrimsonIsleStatusHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Shows boss name and status as floating text above each boss spawn location.")
        @ConfigEditorBoolean
        public boolean showCrimsonIsleWorldText = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Toggles the notification for rare drops from Crimson Isle bosses in title and chat.")
        @ConfigEditorBoolean
        public boolean enableCrimsonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Toggles the Crimson Isle Loot Tracker HUD.")
        @ConfigEditorBoolean
        public boolean showCrimsonLootTrackerHud = true;

        // ---- Barbarian Duke X (id: 0, 1, 2) ----
        @Expose
        @ConfigOption(name = "Barbarian Duke X", desc = "Expand to configure Barbarian Duke X settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean barbarianSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean barbarianHudFolder = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Barbarian Duke X Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBarbarianHealthHud = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean barbarianHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Barbarian Duke X through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableBarbarianHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to Barbarian Duke X.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean enableBarbarianTracer = true;

        // ---- Bladesoul (id: 10, 11, 12) ----
        @Expose
        @ConfigOption(name = "Bladesoul", desc = "Expand to configure Bladesoul settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean bladesoulSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 11)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean bladesoulHudFolder = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Bladesoul Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showBladesoulHealthHud = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 12)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean bladesoulHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Bladesoul through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableBladesoulHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to Bladesoul.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableBladesoulTracer = true;

        // ---- Mage Outlaw (id: 20, 21, 22) ----
        @Expose
        @ConfigOption(name = "Mage Outlaw", desc = "Expand to configure Mage Outlaw settings.")
        @ConfigEditorAccordion(id = 20)
        @ConfigEditorBoolean
        public boolean mageOutlawSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 21)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean mageOutlawHudFolder = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Mage Outlaw Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 21)
        public boolean showMageOutlawHealthHud = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 22)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean mageOutlawHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Mage Outlaw through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 22)
        public boolean enableMageOutlawHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to Mage Outlaw.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 22)
        public boolean enableMageOutlawTracer = true;

        // ---- Ashfang (id: 30, 31, 32) ----
        @Expose
        @ConfigOption(name = "Ashfang", desc = "Expand to configure Ashfang settings.")
        @ConfigEditorAccordion(id = 30)
        @ConfigEditorBoolean
        public boolean ashfangSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 31)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean ashfangHudFolder = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Ashfang Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 31)
        public boolean showAshfangHealthHud = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 32)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean ashfangHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Ashfang through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 32)
        public boolean enableAshfangHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to Ashfang.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 32)
        public boolean enableAshfangTracer = true;

        @Expose
        @ConfigOption(name = "Follower Highlight", desc = "Expand to configure Ashfang follower highlight settings.")
        @ConfigEditorAccordion(id = 33)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean ashfangFollowerHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Follower Glowing", desc = "Highlights Ashfang Followers through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerHighlight = true;

        @Expose
        @ConfigOption(name = "Follower Tracer", desc = "Draws a tracer line from your position to Ashfang Followers.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerTracer = true;

        @Expose
        @ConfigOption(name = "Acolyte Glowing", desc = "Highlights Ashfang Acolytes through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteHighlight = true;

        @Expose
        @ConfigOption(name = "Acolyte Tracer", desc = "Draws a tracer line from your position to Ashfang Acolytes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteTracer = true;

        @Expose
        @ConfigOption(name = "Underling Glowing", desc = "Highlights Ashfang Underlings through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingHighlight = true;

        @Expose
        @ConfigOption(name = "Underling Tracer", desc = "Draws a tracer line from your position to Ashfang Underlings.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingTracer = true;

        // ---- Magma Boss (id: 40, 41, 42) ----
        @Expose
        @ConfigOption(name = "Magma Boss", desc = "Expand to configure Magma Boss settings.")
        @ConfigEditorAccordion(id = 40)
        @ConfigEditorBoolean
        public boolean magmaBossSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 41)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean magmaBossHudFolder = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "Toggles the Magma Boss Health HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 41)
        public boolean showMagmaBossHealthHud = true;

        @Expose
        @ConfigOption(name = "Stage Status Title", desc = "Shows a title when the Magma Boss stage status changes (75%, Kill the Magmas, etc.).")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossTitle = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Expand to configure boss highlight settings.")
        @ConfigEditorAccordion(id = 42)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean magmaBossHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlights Magma Boss through walls.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 42)
        public boolean enableMagmaBossHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Draws a tracer line from your position to Magma Boss.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 42)
        public boolean enableMagmaBossTracer = true;
    }

    public static class MiscCategory {
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "Expand to configure HUD elements.")
        @ConfigEditorAccordion(id = 50)
        @ConfigEditorBoolean
        public boolean hudFolder = false;

        @Expose
        @ConfigOption(name = "Pet HUD", desc = "Shows your currently active pet.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showPetHud = false;

        @Expose
        @ConfigOption(name = "TPS HUD", desc = "Shows the server's estimated TPS (ticks per second).")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showTpsHud = false;

        @Expose
        @ConfigOption(name = "Day HUD", desc = "Shows the current lobby's age.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showDayHud = false;

        @Expose
        @ConfigOption(name = "Armor HUD", desc = "Shows the armor pieces you currently have equipped.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showEquipmentHud = false;

        @Expose
        @ConfigOption(name = "Armor HUD Orientation", desc = "Lays the armor HUD out horizontally or vertically.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 50)
        public HudOrientation equipmentHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Equipment HUD", desc = "Shows the Necklace/Cloak/Belt/Gloves last scanned by opening /equipment.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showGearHud = false;

        @Expose
        @ConfigOption(name = "Equipment HUD Orientation", desc = "Lays the Equipment HUD out horizontally or vertically.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 50)
        public HudOrientation gearHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Armor Stack HUD", desc = "Shows the number of armor stacks for Crimson, Terror, Aurora, Hollow, and Fervor.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showArmorStackHud = false;

        @Expose
        @ConfigOption(name = "Arrow Poison Indicator", desc = "Shows Toxic/Twilight Arrow Poison uses left on your bows in the hotbar.")
        @ConfigEditorBoolean
        public boolean showPoisonIndicator = false;

        @Expose
        @ConfigOption(name = "Server Reboot Alert", desc = "Warning with sound and title when the current lobby restarts.")
        @ConfigEditorBoolean
        public boolean enableRebootAlert = true;

        @Expose
        @ConfigOption(name = "Warp Cooldown Queue", desc = "Shows a 5s countdown after using /warp. If /warp is used again during the cooldown, it is queued and sent automatically once the cooldown ends.")
        @ConfigEditorBoolean
        public boolean enableWarpQueue = true;
    }
}
