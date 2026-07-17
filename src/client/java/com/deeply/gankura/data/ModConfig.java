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
        INSTANCE.misc.resetHeldItemScale = () -> INSTANCE.misc.heldItemScale = 1.0f;
        INSTANCE.misc.resetHeldItemOffsetX = () -> INSTANCE.misc.heldItemOffsetX = 0.0f;
        INSTANCE.misc.resetHeldItemOffsetY = () -> INSTANCE.misc.heldItemOffsetY = 0.0f;

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
    // 各カテゴリの中身
    // ==========================================
    public static class GuiCategory {
        @ConfigOption(name = "Edit GUI Locations", desc = "Move HUD elements.")
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
        @ConfigOption(name = "End Stone Protector", desc = "End Stone Protector settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean golemSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "HUD settings.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showLootTrackerHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showGolemHealthHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Text and beacon settings.")
        @ConfigEditorAccordion(id = 2)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean worldLocationFolder = false;

        @Expose
        @ConfigOption(name = "Show Text", desc = "3D floating text.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Text = true;

        @Expose
        @ConfigOption(name = "Show Beacon Beam", desc = "Beacon beam.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 2)
        public boolean showGolemWorldLocation_Beacon = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Stage 4 alerts.")
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
        @ConfigOption(name = "Stage 5 Alert", desc = "Stage 5 alerts.")
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
        @ConfigOption(name = "Chat Settings", desc = "Chat messages.")
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
        @ConfigOption(name = "Day 30+ Alert Chat", desc = "Day 30+ warning.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean enableDay30Alert = true;

        @Expose
        @ConfigOption(name = "Rare Drop Notification", desc = "Rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Highlight settings.")
        @ConfigEditorAccordion(id = 6)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean golemHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Golem.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Golem.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean enableGolemTracer = true;

        // ========== Dragon section ==========
        @Expose
        @ConfigOption(name = "Dragon", desc = "Dragon settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean dragonSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "HUD settings.")
        @ConfigEditorAccordion(id = 11)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonStatusHud = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Loot Tracker HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 11)
        public boolean showDragonTrackerHud = true;

        @Expose
        @ConfigOption(name = "Spawn Alert Title", desc = "Spawn alerts.")
        @ConfigEditorAccordion(id = 12)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean spawnTitleFolder = false;

        @Expose
        @ConfigOption(name = "Protector", desc = "Protector Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Protector = true;

        @Expose
        @ConfigOption(name = "Old", desc = "Old Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Old = true;

        @Expose
        @ConfigOption(name = "Unstable", desc = "Unstable Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Unstable = true;

        @Expose
        @ConfigOption(name = "Young", desc = "Young Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Young = true;

        @Expose
        @ConfigOption(name = "Strong", desc = "Strong Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Strong = true;

        @Expose
        @ConfigOption(name = "Wise", desc = "Wise Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Wise = true;

        @Expose
        @ConfigOption(name = "Superior", desc = "Superior Dragon alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 12)
        public boolean enableDragonAlert_Superior = true;

        @Expose
        @ConfigOption(name = "Chat Settings", desc = "Chat messages.")
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
        @ConfigOption(name = "Rare Drop Notification", desc = "Rare drop alert.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableDragonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Highlight settings.")
        @ConfigEditorAccordion(id = 14)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean dragonHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Dragon.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 14)
        public boolean enableDragonHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Dragon.")
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
        @ConfigOption(name = "Broodmother", desc = "Broodmother settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean broodmotherSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "HUD settings.")
        @ConfigEditorAccordion(id = 1)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHudFolder = false;

        @Expose
        @ConfigOption(name = "Status HUD", desc = "Status HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherStatusHud = true;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 1)
        public boolean showBroodmotherHealthHud = true;

        @Expose
        @ConfigOption(name = "Stage 4 Alert", desc = "Stage 4 alerts.")
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
        @ConfigOption(name = "Stage 5 Alert", desc = "Stage 5 alerts.")
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
        @ConfigOption(name = "Boss Highlight", desc = "Highlight settings.")
        @ConfigEditorAccordion(id = 4)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean broodmotherHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Broodmother.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Broodmother.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 4)
        public boolean enableBroodmotherTracer = true;

        @Expose
        @ConfigOption(name = "Arachne", desc = "Arachne settings.")
        @ConfigEditorAccordion(id = 5)
        @ConfigEditorBoolean
        public boolean arachneSection = false;

        @Expose
        @ConfigOption(name = "HUD Settings", desc = "HUD settings.")
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
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 6)
        public boolean showArachneHealthHud = true;

        @Expose
        @ConfigOption(name = "World Location Display", desc = "Shows floating text at altar.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean showArachneWorldText = true;

        @Expose
        @ConfigOption(name = "Boss Highlight", desc = "Highlight settings.")
        @ConfigEditorAccordion(id = 7)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 5)
        public boolean arachneHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Arachne.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 7)
        public boolean enableArachneHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Arachne.")
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
        @ConfigOption(name = "Rare Drop Notification", desc = "Rare drop alert.")
        @ConfigEditorBoolean
        public boolean enableCrimsonDropAlerts = true;

        @Expose
        @ConfigOption(name = "Loot Tracker HUD", desc = "Loot Tracker HUD.")
        @ConfigEditorBoolean
        public boolean showCrimsonLootTrackerHud = true;

        // ---- Barbarian Duke X (id: 0) ----
        @Expose
        @ConfigOption(name = "Barbarian Duke X", desc = "Barbarian Duke X settings.")
        @ConfigEditorAccordion(id = 0)
        @ConfigEditorBoolean
        public boolean barbarianSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean showBarbarianHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Barbarian Duke X.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableBarbarianHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Barbarian Duke X.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean enableBarbarianTracer = true;

        // ---- Bladesoul (id: 10) ----
        @Expose
        @ConfigOption(name = "Bladesoul", desc = "Bladesoul settings.")
        @ConfigEditorAccordion(id = 10)
        @ConfigEditorBoolean
        public boolean bladesoulSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean showBladesoulHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Bladesoul.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableBladesoulHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Bladesoul.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 10)
        public boolean enableBladesoulTracer = true;

        // ---- Mage Outlaw (id: 20) ----
        @Expose
        @ConfigOption(name = "Mage Outlaw", desc = "Mage Outlaw settings.")
        @ConfigEditorAccordion(id = 20)
        @ConfigEditorBoolean
        public boolean mageOutlawSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean showMageOutlawHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Mage Outlaw.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean enableMageOutlawHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Mage Outlaw.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 20)
        public boolean enableMageOutlawTracer = true;

        // ---- Ashfang (id: 30, 33) ----
        @Expose
        @ConfigOption(name = "Ashfang", desc = "Ashfang settings.")
        @ConfigEditorAccordion(id = 30)
        @ConfigEditorBoolean
        public boolean ashfangSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean showAshfangHealthHud = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Ashfang.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean enableAshfangHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Ashfang.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean enableAshfangTracer = true;

        @Expose
        @ConfigOption(name = "Follower Highlight", desc = "Follower highlight settings.")
        @ConfigEditorAccordion(id = 33)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 30)
        public boolean ashfangFollowerHighlightFolder = false;

        @Expose
        @ConfigOption(name = "Follower Glowing", desc = "Highlight Followers.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerHighlight = true;

        @Expose
        @ConfigOption(name = "Follower Tracer", desc = "Tracer to Followers.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangFollowerTracer = true;

        @Expose
        @ConfigOption(name = "Acolyte Glowing", desc = "Highlight Acolytes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteHighlight = true;

        @Expose
        @ConfigOption(name = "Acolyte Tracer", desc = "Tracer to Acolytes.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangAcolyteTracer = true;

        @Expose
        @ConfigOption(name = "Underling Glowing", desc = "Highlight Underlings.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingHighlight = true;

        @Expose
        @ConfigOption(name = "Underling Tracer", desc = "Tracer to Underlings.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 33)
        public boolean enableAshfangUnderlingTracer = true;

        // ---- Magma Boss (id: 40) ----
        @Expose
        @ConfigOption(name = "Magma Boss", desc = "Magma Boss settings.")
        @ConfigEditorAccordion(id = 40)
        @ConfigEditorBoolean
        public boolean magmaBossSection = false;

        @Expose
        @ConfigOption(name = "HP HUD", desc = "HP HUD.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean showMagmaBossHealthHud = true;

        @Expose
        @ConfigOption(name = "Stage Status Title", desc = "Shows stage status title.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossTitle = true;

        @Expose
        @ConfigOption(name = "Glowing", desc = "Highlight Magma Boss.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossHighlight = true;

        @Expose
        @ConfigOption(name = "Tracer", desc = "Tracer to Magma Boss.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 40)
        public boolean enableMagmaBossTracer = true;
    }

    public static class MiscCategory {
        @Expose
        @ConfigOption(name = "HUD Settings", desc = "HUD settings.")
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
        @ConfigOption(name = "Armor HUD Orientation", desc = "Armor HUD layout.")
        @ConfigEditorAccordion(id = 51)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean armorOrientationFolder = false;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Horizontal or vertical.")
        @ConfigEditorDropdown
        @ConfigAccordionId(id = 51)
        public HudOrientation equipmentHudOrientation = HudOrientation.HORIZONTAL;

        @Expose
        @ConfigOption(name = "Equipment HUD", desc = "Shows Necklace/Cloak/Belt/Gloves.")
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean showGearHud = false;

        @Expose
        @ConfigOption(name = "Equipment HUD Orientation", desc = "Equipment HUD layout.")
        @ConfigEditorAccordion(id = 52)
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 50)
        public boolean gearOrientationFolder = false;

        @Expose
        @ConfigOption(name = "Orientation", desc = "Horizontal or vertical.")
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
        @ConfigOption(name = "Server Reboot Alert", desc = "Lobby restart warning.")
        @ConfigEditorBoolean
        public boolean enableRebootAlert = true;

        @Expose
        @ConfigOption(name = "Warp Cooldown Queue", desc = "Shows cooldown, queues /warp.")
        @ConfigEditorBoolean
        public boolean enableWarpQueue = true;

        @Expose
        @ConfigOption(name = "Keep Cursor Position", desc = "Keep cursor on quick menu swap.")
        @ConfigEditorBoolean
        public boolean enableCursorRestoreOnRapidReopen = true;

        @Expose
        @ConfigOption(name = "Held Item Size", desc = "Held item size and position.")
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
