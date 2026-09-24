package com.deeply.gankura.data;

import com.deeply.gankura.gui.InventoryButtonEditorScreen;
import com.deeply.gankura.gui.WaypointScreen;
import com.deeply.gankura.render.HudEditorScreen;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import com.teamresourceful.resourcefulconfig.api.patching.ConfigPatchEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 設定の定義そのもの。ResourcefulConfig がこのクラスを読んで、
 * 設定画面と config/gankura.json の読み書きを両方とも組み立てる。
 *
 * ResourcefulConfig の決まりごと:
 *   - 設定値のフィールドは public static（final は不可）
 *   - ボタンは public static final Runnable
 *   - カテゴリはネストした static クラスで、親の categories に並べた順に表示される
 *   - 表示名と説明は翻訳キー。文言は assets/gankura/lang/en_us.json にある
 */
@Config(value = "gankura", version = ModConfig.CONFIG_VERSION, categories = {
        ModConfig.Gui.class,
        ModConfig.Combat.class,
        ModConfig.Farming.class,
        ModConfig.Foraging.class,
        ModConfig.Fishing.class,
        ModConfig.Mining.class,
        ModConfig.GeneralHud.class,
        ModConfig.MobVisuals.class,
        ModConfig.Waypoints.class,
        ModConfig.InventoryButtons.class,
        ModConfig.HeldItem.class,
        ModConfig.Interface.class,
        ModConfig.ChatFilter.class,
        ModConfig.Keybinds.class,
        ModConfig.Misc.class
})
@ConfigInfo(title = "GanKura", description = "A Hypixel Skyblock Mod focused on Area Mini-bosses.")
public final class ModConfig {

    /** 設定ファイル名かつ ResourcefulConfig 上の識別子 */
    public static final String CONFIG_ID = "gankura";

    /**
     * 設定ファイルの版。項目の置き場所を変えたら上げて、{@link #patch} に前の版からの移し方を足す。
     *   0 → 1: 戦闘まわりの HUD と Low Soulflow Alert を General HUD から Combat へ移した
     */
    public static final int CONFIG_VERSION = 1;

    /** ResourcefulConfig はキー未割り当てを 0 で表す（MoulConfig の -1 に相当） */
    public static final int KEY_NONE = 0;

    private static final Configurator CONFIGURATOR = new Configurator("gankura");

    /**
     * 設定画面で開いている折りたたみ見出しのキー。MoulConfig の頃と同じく、開閉の状態も保存しておく。
     * 画面には出さない
     */
    @ConfigEntry(id = "expandedAccordions")
    @ConfigOption.Hidden
    public static String[] expandedAccordions = new String[0];

    public enum HudOrientation {
        HORIZONTAL, VERTICAL;

        @Override
        public String toString() {
            return this == HORIZONTAL ? "Horizontal" : "Vertical";
        }
    }

    private ModConfig() {
    }

    /** 起動時に一度だけ呼ぶ。旧 MoulConfig 形式が残っていれば先に取り込む */
    public static void load() {
        LegacyConfigMigration.run();
        CONFIGURATOR.register(ModConfig.class, ModConfig::patch);
        InventoryButtonStore.load();
    }

    public static void save() {
        CONFIGURATOR.saveConfig(ModConfig.class);
        InventoryButtonStore.save();
    }

    /** 古い版の設定ファイルを、今の置き場所に合わせて書き換える */
    private static void patch(ConfigPatchEvent event) {
        event.register(0, json -> moveEntries(json, "generalHud", "combat",
                "showArmorStackHud", "showFerocityHud", "showQuiverHud", "showSoulflowHud",
                "showSoulflowLowAlert", "soulflowLowThreshold"));
    }

    /**
     * カテゴリをまたいで項目を移す。
     * ResourcefulConfig の move() は元に無い項目も null で書き込むので、元にあるものだけを移す
     */
    private static JsonObject moveEntries(JsonObject json, String from, String to, String... ids) {
        if (!(json.get(from) instanceof JsonObject source)) return json;
        JsonObject target = json.get(to) instanceof JsonObject existing ? existing : new JsonObject();
        json.add(to, target);
        for (String id : ids) {
            JsonElement value = source.remove(id);
            if (value != null) target.add(id, value);
        }
        return json;
    }

    public static Configurator configurator() {
        return CONFIGURATOR;
    }


    @Category(value = "gui")
    @ConfigInfo(title = "Edit HUD Locations", description = "Move and scale everything this mod draws on screen.")
    public static final class Gui {

        // ボタンには @Expose は付けず、代わりに transient を付けます！
        @ConfigButton(title = "gankura.config.gui.openHudEditor", text = "gankura.config.gui.openHudEditor.button")
        @Comment(value = "Opens HUD editor.")
        public static final Runnable openHudEditor = () -> {
            // ボタンが押されたら、マイクラの画面をHudEditorScreenに切り替える
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
            });
        };
    }

    @Category(value = "combat", categories = {Combat.TheEnd.class, Combat.SpidersDen.class, Combat.CrimsonIsle.class})
    @ConfigInfo(title = "Combat", description = "Bosses and combat features of each area.")
    public static final class Combat {

        @ConfigEntry(id = "hideDamageSplash", translation = "gankura.config.combat.hideDamageSplash")
        @Comment(value = "Hides the damage numbers popping off mobs.", translation = "gankura.config.combat.hideDamageSplash.desc")
        public static boolean hideDamageSplash = false;

        @ConfigEntry(id = "hideFireOverlay", translation = "gankura.config.combat.hideFireOverlay")
        @Comment(value = "Hides the flames drawn at the bottom of the screen while you are burning.", translation = "gankura.config.combat.hideFireOverlay.desc")
        public static boolean hideFireOverlay = false;

        @ConfigEntry(id = "enableCocoonCatchTitle", translation = "gankura.config.combat.enableCocoonCatchTitle")
        @Comment(value = "Shows a title when you cocoon a mob.", translation = "gankura.config.combat.enableCocoonCatchTitle.desc")
        public static boolean enableCocoonCatchTitle = true;

        @ConfigEntry(id = "showPoisonIndicator", translation = "gankura.config.combat.showPoisonIndicator")
        @Comment(value = "Shows arrow poison uses left.", translation = "gankura.config.combat.showPoisonIndicator.desc")
        public static boolean showPoisonIndicator = true;

        @ConfigEntry(id = "showArmorStackHud", translation = "gankura.config.combat.showArmorStackHud")
        @Comment(value = "Shows armor stack counts.", translation = "gankura.config.combat.showArmorStackHud.desc")
        public static boolean showArmorStackHud = false;

        @ConfigEntry(id = "showFerocityHud", translation = "gankura.config.combat.showFerocityHud")
        @Comment(value = "Shows ferocity. Hidden while it cannot be read.\n§eNeeds the Ferocity Stats "
            + "Widget.\n§e(/widget -> Stats Widget -> Enable Ferocity)", translation = "gankura.config.combat.showFerocityHud.desc")
        public static boolean showFerocityHud = false;

        @ConfigEntry(id = "showQuiverHud", translation = "gankura.config.combat.showQuiverHud")
        @Comment(value = "Shows selected arrow and how many are left.", translation = "gankura.config.combat.showQuiverHud.desc")
        public static boolean showQuiverHud = false;

        @ConfigEntry(id = "enableQuiverAlert", translation = "gankura.config.combat.enableQuiverAlert")
        @Comment(value = "Shows a title when the arrows in your quiver are running out.", translation = "gankura.config.combat.enableQuiverAlert.desc")
        public static boolean enableQuiverAlert = false;

        @ConfigEntry(id = "quiverLowThreshold", translation = "gankura.config.combat.quiverLowThreshold")
        @Comment(value = "How many arrows are left when the alert shows.", translation = "gankura.config.combat.quiverLowThreshold.desc")
        @ConfigOption.Range(min = 1, max = 2880)
        public static int quiverLowThreshold = 50;

        @ConfigEntry(id = "showSoulflowHud", translation = "gankura.config.combat.showSoulflowHud")
        @Comment(value = "Shows soulflow.\n§eNeeds Soulflow in the Profile Widget.\n"
            + "§e(/widget -> Profile Widget -> Show Soulflow)", translation = "gankura.config.combat.showSoulflowHud.desc")
        public static boolean showSoulflowHud = false;

        @ConfigEntry(id = "showSoulflowLowAlert", translation = "gankura.config.combat.showSoulflowLowAlert")
        @Comment(value = "Shows a title when soulflow runs down to the threshold.\n§eNeeds Soulflow in the Profile "
            + "Widget.", translation = "gankura.config.combat.showSoulflowLowAlert.desc")
        public static boolean showSoulflowLowAlert = false;

        @ConfigEntry(id = "soulflowLowThreshold", translation = "gankura.config.combat.soulflowLowThreshold")
        @Comment(value = "How much soulflow is left when the alert shows.", translation = "gankura.config.combat.soulflowLowThreshold.desc")
        @ConfigOption.Range(min = 1, max = 1000000)
        public static int soulflowLowThreshold = 500;

        @Category(value = "theEnd")
        @ConfigInfo(title = "The End", description = "End Stone Protector and Dragon.")
        public static final class TheEnd {

            @ConfigEntry(id = "showGolemStatusHud", translation = "gankura.config.combat.theEnd.showGolemStatusHud")
            @Comment(value = "Shows status HUD.", translation = "gankura.config.combat.theEnd.showGolemStatusHud.desc")
            public static boolean showGolemStatusHud = true;

            @ConfigEntry(id = "showLootTrackerHud", translation = "gankura.config.combat.theEnd.showLootTrackerHud")
            @Comment(value = "Shows loot tracker HUD.", translation = "gankura.config.combat.theEnd.showLootTrackerHud.desc")
            public static boolean showLootTrackerHud = true;

            @ConfigEntry(id = "enableDropAlerts", translation = "gankura.config.combat.theEnd.enableDropAlerts")
            @Comment(value = "Shows rare drop alert.", translation = "gankura.config.combat.theEnd.enableDropAlerts.desc")
            public static boolean enableDropAlerts = true;

            @ConfigEntry(id = "trackedGolemDrops", translation = "gankura.config.combat.theEnd.trackedGolemDrops")
            @Comment(value = "Change which drops are scanned and shown on the loot tracker HUD.", translation = "gankura.config.combat.theEnd.trackedGolemDrops.desc")
            @ConfigOption.Draggable({})
            public static GolemRareDrop[] trackedGolemDrops = GolemRareDrop.defaults().toArray(new GolemRareDrop[0]);

            @ConfigEntry(id = "showGolemWorldLocation_Text", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Text")
            @Comment(value = "Shows 3D floating text.", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Text.desc")
            public static boolean showGolemWorldLocation_Text = true;

            @ConfigEntry(id = "showGolemWorldLocation_Beacon", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Beacon")
            @Comment(value = "Shows beacon beam.", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Beacon.desc")
            public static boolean showGolemWorldLocation_Beacon = true;

            @ConfigEntry(id = "showGolemWorldLocation_Tracer", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Tracer")
            @Comment(value = "Draws a line pointing at the 3D floating text.", translation = "gankura.config.combat.theEnd.showGolemWorldLocation_Tracer.desc")
            public static boolean showGolemWorldLocation_Tracer = true;

            @ConfigEntry(id = "enableStage4Title", translation = "gankura.config.combat.theEnd.enableStage4Title")
            @Comment(value = "Shows stage 4 title.", translation = "gankura.config.combat.theEnd.enableStage4Title.desc")
            public static boolean enableStage4Title = true;

            @ConfigEntry(id = "enableStage4Sound", translation = "gankura.config.combat.theEnd.enableStage4Sound")
            @Comment(value = "Plays stage 4 sound.", translation = "gankura.config.combat.theEnd.enableStage4Sound.desc")
            public static boolean enableStage4Sound = true;

            @ConfigEntry(id = "enableStage5Title", translation = "gankura.config.combat.theEnd.enableStage5Title")
            @Comment(value = "Shows stage 5 title.", translation = "gankura.config.combat.theEnd.enableStage5Title.desc")
            public static boolean enableStage5Title = true;

            @ConfigEntry(id = "enableStage5Sound", translation = "gankura.config.combat.theEnd.enableStage5Sound")
            @Comment(value = "Plays stage 5 sound.", translation = "gankura.config.combat.theEnd.enableStage5Sound.desc")
            public static boolean enableStage5Sound = true;

            @ConfigEntry(id = "showStage4Duration", translation = "gankura.config.combat.theEnd.showStage4Duration")
            @Comment(value = "Shows stage 4→5 duration.", translation = "gankura.config.combat.theEnd.showStage4Duration.desc")
            public static boolean showStage4Duration = true;

            @ConfigEntry(id = "showDpsChat", translation = "gankura.config.combat.theEnd.showDpsChat")
            @Comment(value = "Shows DPS results in chat.", translation = "gankura.config.combat.theEnd.showDpsChat.desc")
            public static boolean showDpsChat = true;

            @ConfigEntry(id = "showLootQualityChat", translation = "gankura.config.combat.theEnd.showLootQualityChat")
            @Comment(value = "Shows loot quality in chat.", translation = "gankura.config.combat.theEnd.showLootQualityChat.desc")
            public static boolean showLootQualityChat = true;

            @ConfigEntry(id = "enableDay30Alert", translation = "gankura.config.combat.theEnd.enableDay30Alert")
            @Comment(value = "Alerts on day 30+.", translation = "gankura.config.combat.theEnd.enableDay30Alert.desc")
            public static boolean enableDay30Alert = true;

            @ConfigEntry(id = "showDragonStatusHud", translation = "gankura.config.combat.theEnd.showDragonStatusHud")
            @Comment(value = "Shows status HUD.", translation = "gankura.config.combat.theEnd.showDragonStatusHud.desc")
            public static boolean showDragonStatusHud = true;

            @ConfigEntry(id = "showDragonTrackerHud", translation = "gankura.config.combat.theEnd.showDragonTrackerHud")
            @Comment(value = "Shows loot tracker HUD.", translation = "gankura.config.combat.theEnd.showDragonTrackerHud.desc")
            public static boolean showDragonTrackerHud = true;

            @ConfigEntry(id = "enableDragonDropAlerts", translation = "gankura.config.combat.theEnd.enableDragonDropAlerts")
            @Comment(value = "Shows rare drop alert.", translation = "gankura.config.combat.theEnd.enableDragonDropAlerts.desc")
            public static boolean enableDragonDropAlerts = true;

            @ConfigEntry(id = "trackedDragonDrops", translation = "gankura.config.combat.theEnd.trackedDragonDrops")
            @Comment(value = "Change which drops are scanned and shown on the loot tracker HUD.", translation = "gankura.config.combat.theEnd.trackedDragonDrops.desc")
            @ConfigOption.Draggable({})
            public static DragonRareDrop[] trackedDragonDrops = DragonRareDrop.defaults().toArray(new DragonRareDrop[0]);

            @ConfigEntry(id = "enableDragonSpawnAlert", translation = "gankura.config.combat.theEnd.enableDragonSpawnAlert")
            @Comment(value = "Shows a spawn alert title for dragons.", translation = "gankura.config.combat.theEnd.enableDragonSpawnAlert.desc")
            public static boolean enableDragonSpawnAlert = true;

            @ConfigEntry(id = "dragonSpawnAlerts", translation = "gankura.config.combat.theEnd.dragonSpawnAlerts")
            @Comment(value = "Change which dragons show a spawn alert title.", translation = "gankura.config.combat.theEnd.dragonSpawnAlerts.desc")
            @ConfigOption.Draggable({})
            public static DragonAlertType[] dragonSpawnAlerts = DragonAlertType.defaults().toArray(new DragonAlertType[0]);

            @ConfigEntry(id = "showDragonDpsChat", translation = "gankura.config.combat.theEnd.showDragonDpsChat")
            @Comment(value = "Shows DPS results in chat.", translation = "gankura.config.combat.theEnd.showDragonDpsChat.desc")
            public static boolean showDragonDpsChat = true;

            @ConfigEntry(id = "showDragonLootQualityChat", translation = "gankura.config.combat.theEnd.showDragonLootQualityChat")
            @Comment(value = "Shows loot quality in chat.", translation = "gankura.config.combat.theEnd.showDragonLootQualityChat.desc")
            public static boolean showDragonLootQualityChat = true;
        }

        @Category(value = "spidersDen")
        @ConfigInfo(title = "Spider's Den", description = "Broodmother and Arachne.")
        public static final class SpidersDen {

            @ConfigEntry(id = "showBroodmotherStatusHud", translation = "gankura.config.combat.spidersDen.showBroodmotherStatusHud")
            @Comment(value = "Shows status HUD.", translation = "gankura.config.combat.spidersDen.showBroodmotherStatusHud.desc")
            public static boolean showBroodmotherStatusHud = true;

            @ConfigEntry(id = "enableStage4Title", translation = "gankura.config.combat.spidersDen.enableStage4Title")
            @Comment(value = "Shows stage 4 title.", translation = "gankura.config.combat.spidersDen.enableStage4Title.desc")
            public static boolean enableStage4Title = true;

            @ConfigEntry(id = "enableStage4Sound", translation = "gankura.config.combat.spidersDen.enableStage4Sound")
            @Comment(value = "Plays stage 4 sound.", translation = "gankura.config.combat.spidersDen.enableStage4Sound.desc")
            public static boolean enableStage4Sound = true;

            @ConfigEntry(id = "enableStage5Title", translation = "gankura.config.combat.spidersDen.enableStage5Title")
            @Comment(value = "Shows stage 5 title.", translation = "gankura.config.combat.spidersDen.enableStage5Title.desc")
            public static boolean enableStage5Title = true;

            @ConfigEntry(id = "enableStage5Sound", translation = "gankura.config.combat.spidersDen.enableStage5Sound")
            @Comment(value = "Plays stage 5 sound.", translation = "gankura.config.combat.spidersDen.enableStage5Sound.desc")
            public static boolean enableStage5Sound = true;

            @ConfigEntry(id = "showBroodmotherStage4Duration", translation = "gankura.config.combat.spidersDen.showBroodmotherStage4Duration")
            @Comment(value = "Shows stage 4→5 duration.", translation = "gankura.config.combat.spidersDen.showBroodmotherStage4Duration.desc")
            public static boolean showBroodmotherStage4Duration = true;

            @ConfigEntry(id = "showArachneStatusHud", translation = "gankura.config.combat.spidersDen.showArachneStatusHud")
            @Comment(value = "Shows spawn countdown.", translation = "gankura.config.combat.spidersDen.showArachneStatusHud.desc")
            public static boolean showArachneStatusHud = true;

            @ConfigEntry(id = "showArachneWorldText", translation = "gankura.config.combat.spidersDen.showArachneWorldText")
            @Comment(value = "Shows floating text at altar.", translation = "gankura.config.combat.spidersDen.showArachneWorldText.desc")
            public static boolean showArachneWorldText = true;
        }

        @Category(value = "crimsonIsle")
        @ConfigInfo(title = "Crimson Isle", description = "Crimson Isle bosses.")
        public static final class CrimsonIsle {

            @ConfigEntry(id = "showCrimsonIsleStatusHud", translation = "gankura.config.combat.crimsonIsle.showCrimsonIsleStatusHud")
            @Comment(value = "Shows boss status.", translation = "gankura.config.combat.crimsonIsle.showCrimsonIsleStatusHud.desc")
            public static boolean showCrimsonIsleStatusHud = true;

            @ConfigEntry(id = "showCrimsonLootTrackerHud", translation = "gankura.config.combat.crimsonIsle.showCrimsonLootTrackerHud")
            @Comment(value = "Shows loot tracker HUD.", translation = "gankura.config.combat.crimsonIsle.showCrimsonLootTrackerHud.desc")
            public static boolean showCrimsonLootTrackerHud = true;

            @ConfigEntry(id = "enableCrimsonDropAlerts", translation = "gankura.config.combat.crimsonIsle.enableCrimsonDropAlerts")
            @Comment(value = "Shows rare drop alert.", translation = "gankura.config.combat.crimsonIsle.enableCrimsonDropAlerts.desc")
            public static boolean enableCrimsonDropAlerts = true;

            @ConfigEntry(id = "trackedCrimsonDrops", translation = "gankura.config.combat.crimsonIsle.trackedCrimsonDrops")
            @Comment(value = "Change which drops are scanned and shown on the loot tracker HUD.", translation = "gankura.config.combat.crimsonIsle.trackedCrimsonDrops.desc")
            @ConfigOption.Draggable({})
            public static CrimsonRareDrop[] trackedCrimsonDrops = CrimsonRareDrop.defaults().toArray(new CrimsonRareDrop[0]);

            @ConfigEntry(id = "showCrimsonIsleWorldText", translation = "gankura.config.combat.crimsonIsle.showCrimsonIsleWorldText")
            @Comment(value = "Shows floating text at spawns.", translation = "gankura.config.combat.crimsonIsle.showCrimsonIsleWorldText.desc")
            public static boolean showCrimsonIsleWorldText = true;

            @ConfigEntry(id = "enableMagmaBossSpawnTitle", translation = "gankura.config.combat.crimsonIsle.enableMagmaBossSpawnTitle")
            @Comment(value = "Shows stage status title.", translation = "gankura.config.combat.crimsonIsle.enableMagmaBossSpawnTitle.desc")
            public static boolean enableMagmaBossSpawnTitle = true;
        }
    }

    @Category(value = "farming", categories = {Farming.Garden.class})
    @ConfigInfo(title = "Farming", description = "Farming features.")
    public static final class Farming {

        @ConfigEntry(id = "solveDnaAnalyzer", translation = "gankura.config.farming.solveDnaAnalyzer")
        @Comment(value = "Works out how to line the DNA up and colours the two slots to swap next.", translation = "gankura.config.farming.solveDnaAnalyzer.desc")
        public static boolean solveDnaAnalyzer = false;

        @ConfigEntry(id = "dnaAnalyzerMiddleClick", translation = "gankura.config.farming.dnaAnalyzerMiddleClick")
        @Comment(value = "Speeds up swapping by not picking items up.", translation = "gankura.config.farming.dnaAnalyzerMiddleClick.desc")
        public static boolean dnaAnalyzerMiddleClick = true;

        @ConfigEntry(id = "blockDnaAnalyzerClose", translation = "gankura.config.farming.blockDnaAnalyzerClose")
        @Comment(value = "Ignores clicks on the close button.", translation = "gankura.config.farming.blockDnaAnalyzerClose.desc")
        public static boolean blockDnaAnalyzerClose = true;

        @ConfigEntry(id = "hideDnaAnalyzerTooltips", translation = "gankura.config.farming.hideDnaAnalyzerTooltips")
        @Comment(value = "Hides the item tooltips over the board.", translation = "gankura.config.farming.hideDnaAnalyzerTooltips.desc")
        public static boolean hideDnaAnalyzerTooltips = true;

        @Category(value = "garden")
        @ConfigInfo(title = "Garden", description = "The Garden features.")
        public static final class Garden {

            @ConfigEntry(id = "showVisitorStatusHud", translation = "gankura.config.farming.garden.showVisitorStatusHud")
            @Comment(value = "Puts the tab list visitor lines on screen: how many are waiting and when the "
            + "next one comes.", translation = "gankura.config.farming.garden.showVisitorStatusHud.desc")
            public static boolean showVisitorStatusHud = false;

            @ConfigEntry(id = "showVisitorItems", translation = "gankura.config.farming.garden.showVisitorItems")
            @Comment(value = "Lists what the visitor is asking for beside the menu. Click a name to look it up "
            + "on the Bazaar.", translation = "gankura.config.farming.garden.showVisitorItems.desc")
            public static boolean showVisitorItems = false;

            @ConfigEntry(id = "highlightVisitorOffer", translation = "gankura.config.farming.garden.highlightVisitorOffer")
            @Comment(value = "Colours Accept Offer green once you have everything the visitor wants, red while "
            + "something is missing.", translation = "gankura.config.farming.garden.highlightVisitorOffer.desc")
            public static boolean highlightVisitorOffer = false;

            @ConfigEntry(id = "showVisitorArrivalTitle", translation = "gankura.config.farming.garden.showVisitorArrivalTitle")
            @Comment(value = "Shows the name in the middle of the screen when a visitor turns up at your "
            + "garden.", translation = "gankura.config.farming.garden.showVisitorArrivalTitle.desc")
            public static boolean showVisitorArrivalTitle = false;

            @ConfigEntry(id = "showVisitorQueueFullTitle", translation = "gankura.config.farming.garden.showVisitorQueueFullTitle")
            @Comment(value = "Shows a title once the visitor queue fills up and no more will turn up until you "
            + "serve some.", translation = "gankura.config.farming.garden.showVisitorQueueFullTitle.desc")
            public static boolean showVisitorQueueFullTitle = false;

            @ConfigEntry(id = "highlightContestRewards", translation = "gankura.config.farming.garden.highlightContestRewards")
            @Comment(value = "Colours contests green once their reward has been claimed, yellow while it is "
            + "still waiting.", translation = "gankura.config.farming.garden.highlightContestRewards.desc")
            public static boolean highlightContestRewards = false;

            @ConfigEntry(id = "showPestStatusHud", translation = "gankura.config.farming.garden.showPestStatusHud")
            @Comment(value = "Puts the tab list pest lines on screen: when the next ones come, how many are "
            + "alive and where.", translation = "gankura.config.farming.garden.showPestStatusHud.desc")
            public static boolean showPestStatusHud = false;

            @ConfigEntry(id = "showPestSpawnTitle", translation = "gankura.config.farming.garden.showPestSpawnTitle")
            @Comment(value = "Shows how many pests turned up and where, in the middle of the screen.", translation = "gankura.config.farming.garden.showPestSpawnTitle.desc")
            public static boolean showPestSpawnTitle = false;

            @ConfigEntry(id = "showMaxPestsTitle", translation = "gankura.config.farming.garden.showMaxPestsTitle")
            @Comment(value = "Shows a title once the garden is full of pests and no more will spawn until you "
            + "clear some.", translation = "gankura.config.farming.garden.showMaxPestsTitle.desc")
            public static boolean showMaxPestsTitle = false;

            @ConfigEntry(id = "showPestSoonTitle", translation = "gankura.config.farming.garden.showPestSoonTitle")
            @Comment(value = "Shows a title once the tab list says the next pests are within the seconds set "
            + "below.", translation = "gankura.config.farming.garden.showPestSoonTitle.desc")
            public static boolean showPestSoonTitle = false;

            @ConfigEntry(id = "pestSoonSeconds", translation = "gankura.config.farming.garden.pestSoonSeconds")
            @Comment(value = "Seconds left on the next pests to warn at.", translation = "gankura.config.farming.garden.pestSoonSeconds.desc")
            @ConfigOption.Slider
            @ConfigOption.Range(min = 0, max = 60)
            public static int pestSoonSeconds = 5;

            @ConfigEntry(id = "showPestVacuumWaypoint", translation = "gankura.config.farming.garden.showPestVacuumWaypoint")
            @Comment(value = "Marks where the next pest is when you left-click with a vacuum.", translation = "gankura.config.farming.garden.showPestVacuumWaypoint.desc")
            public static boolean showPestVacuumWaypoint = false;

            @ConfigEntry(id = "showPestVacuumTracer", translation = "gankura.config.farming.garden.showPestVacuumTracer")
            @Comment(value = "Draws a line to the pest the vacuum points at.", translation = "gankura.config.farming.garden.showPestVacuumTracer.desc")
            public static boolean showPestVacuumTracer = true;

            @ConfigEntry(id = "hidePestVacuumParticles", translation = "gankura.config.farming.garden.hidePestVacuumParticles")
            @Comment(value = "Hides the particles the vacuum sends.", translation = "gankura.config.farming.garden.hidePestVacuumParticles.desc")
            public static boolean hidePestVacuumParticles = true;

            @ConfigEntry(id = "pestVacuumSeconds", translation = "gankura.config.farming.garden.pestVacuumSeconds")
            @Comment(value = "Seconds the waypoint and tracer stay.", translation = "gankura.config.farming.garden.pestVacuumSeconds.desc")
            @ConfigOption.Slider
            @ConfigOption.Range(min = 5, max = 20)
            public static int pestVacuumSeconds = 15;

            @ConfigEntry(id = "lockViewOnMousemat", translation = "gankura.config.farming.garden.lockViewOnMousemat")
            @Comment(value = "Stops the mouse from turning you once the mousemat has snapped you to its "
            + "direction. Right-click the mousemat again to release it.", translation = "gankura.config.farming.garden.lockViewOnMousemat.desc")
            public static boolean lockViewOnMousemat = false;

            @ConfigEntry(id = "unlockViewOnTeleport", translation = "gankura.config.farming.garden.unlockViewOnTeleport")
            @Comment(value = "Releases the view when you are teleported to another plot, where the direction "
            + "no longer fits.", translation = "gankura.config.farming.garden.unlockViewOnTeleport.desc")
            public static boolean unlockViewOnTeleport = true;

            @ConfigEntry(id = "releaseViewKeybind", translation = "gankura.config.farming.garden.releaseViewKeybind")
            @Comment(value = "Sets a key that releases the view.", translation = "gankura.config.farming.garden.releaseViewKeybind.desc")
            @ConfigOption.Keybind
            public static int releaseViewKeybind = KEY_NONE;
        }
    }

    @Category(value = "foraging")
    @ConfigInfo(title = "Foraging", description = "Foraging features.")
    public static final class Foraging {

        @ConfigEntry(id = "enableTreeFelledTitle", translation = "gankura.config.foraging.enableTreeFelledTitle")
        @Comment(value = "Shows a title when Timber fells a whole tree, yours or another player's.", translation = "gankura.config.foraging.enableTreeFelledTitle.desc")
        public static boolean enableTreeFelledTitle = false;

        @ConfigEntry(id = "enableTreeMobTitle", translation = "gankura.config.foraging.enableTreeMobTitle")
        @Comment(value = "Shows a title when a mob falls from the felled tree.", translation = "gankura.config.foraging.enableTreeMobTitle.desc")
        public static boolean enableTreeMobTitle = false;

        @ConfigEntry(id = "enableTikiWaypoints", translation = "gankura.config.foraging.enableTikiWaypoints")
        @Comment(value = "Marks where Sneaky / Shrieky / Cheeky Tikis spawn.", translation = "gankura.config.foraging.enableTikiWaypoints.desc")
        public static boolean enableTikiWaypoints = false;

        @ConfigEntry(id = "enableBeeheemothSpawnTitle", translation = "gankura.config.foraging.enableBeeheemothSpawnTitle")
        @Comment(value = "Shows a title with the sub-area when a Beeheemoth spawns.", translation = "gankura.config.foraging.enableBeeheemothSpawnTitle.desc")
        public static boolean enableBeeheemothSpawnTitle = true;

        @ConfigEntry(id = "showCapturedCrittersHud", translation = "gankura.config.foraging.showCapturedCrittersHud")
        @Comment(value = "Lists every Critter Safari critter and marks the ones already captured.", translation = "gankura.config.foraging.showCapturedCrittersHud.desc")
        public static boolean showCapturedCrittersHud = true;

        @ConfigEntry(id = "enableFloorDrops", translation = "gankura.config.foraging.enableFloorDrops")
        @Comment(value = "Marks the foraging drops lying on the ground.", translation = "gankura.config.foraging.enableFloorDrops.desc")
        public static boolean enableFloorDrops = false;

        @ConfigEntry(id = "enableBeeNestWaypoints", translation = "gankura.config.foraging.enableBeeNestWaypoints")
        @Comment(value = "Marks the bee nests in the Forest Biome.", translation = "gankura.config.foraging.enableBeeNestWaypoints.desc")
        public static boolean enableBeeNestWaypoints = false;

        @ConfigEntry(id = "hideBeeNestsWhenCaptured", translation = "gankura.config.foraging.hideBeeNestsWhenCaptured")
        @Comment(value = "Drops the waypoints once Honeybug has been captured.", translation = "gankura.config.foraging.hideBeeNestsWhenCaptured.desc")
        public static boolean hideBeeNestsWhenCaptured = false;

        @ConfigEntry(id = "enableSafariFishHighlight", translation = "gankura.config.foraging.enableSafariFishHighlight")
        @Comment(value = "Highlights the fish you can feed to Scrappy.", translation = "gankura.config.foraging.enableSafariFishHighlight.desc")
        public static boolean enableSafariFishHighlight = false;

        @ConfigEntry(id = "hideSafariFishWhenCaptured", translation = "gankura.config.foraging.hideSafariFishWhenCaptured")
        @Comment(value = "Drops the highlight once Scrappy has been captured.", translation = "gankura.config.foraging.hideSafariFishWhenCaptured.desc")
        public static boolean hideSafariFishWhenCaptured = false;

        @ConfigEntry(id = "enableRockmiteMoundHighlight", translation = "gankura.config.foraging.enableRockmiteMoundHighlight")
        @Comment(value = "Highlights the Rockmite Mounds hiding around the Cavern Biome.", translation = "gankura.config.foraging.enableRockmiteMoundHighlight.desc")
        public static boolean enableRockmiteMoundHighlight = false;

        @ConfigEntry(id = "hideRockmiteMoundsWhenCaptured", translation = "gankura.config.foraging.hideRockmiteMoundsWhenCaptured")
        @Comment(value = "Drops the highlight once Rockmite has been captured.", translation = "gankura.config.foraging.hideRockmiteMoundsWhenCaptured.desc")
        public static boolean hideRockmiteMoundsWhenCaptured = false;

        @ConfigEntry(id = "enableWumpaSpawnTitle", translation = "gankura.config.foraging.enableWumpaSpawnTitle")
        @Comment(value = "Shows a title when Wumpa spawns in the Icy Biome.", translation = "gankura.config.foraging.enableWumpaSpawnTitle.desc")
        public static boolean enableWumpaSpawnTitle = true;

        @ConfigEntry(id = "enableWumpaCapsuleMessage", translation = "gankura.config.foraging.enableWumpaCapsuleMessage")
        @Comment(value = "Posts how many Critter Capsules the capture took.", translation = "gankura.config.foraging.enableWumpaCapsuleMessage.desc")
        public static boolean enableWumpaCapsuleMessage = true;

        @ConfigEntry(id = "enableWumpaWaypoint", translation = "gankura.config.foraging.enableWumpaWaypoint")
        @Comment(value = "Marks the hole used to re-enter the Wumpa arena.", translation = "gankura.config.foraging.enableWumpaWaypoint.desc")
        public static boolean enableWumpaWaypoint = true;

        @ConfigEntry(id = "enableDoomspiralCapsuleMessage", translation = "gankura.config.foraging.enableDoomspiralCapsuleMessage")
        @Comment(value = "Posts how many Critter Capsules the capture took.", translation = "gankura.config.foraging.enableDoomspiralCapsuleMessage.desc")
        public static boolean enableDoomspiralCapsuleMessage = true;

        @ConfigEntry(id = "enableMacawSpawnTitle", translation = "gankura.config.foraging.enableMacawSpawnTitle")
        @Comment(value = "Shows a title when two Macaws are attracted to the Birdfeeder.", translation = "gankura.config.foraging.enableMacawSpawnTitle.desc")
        public static boolean enableMacawSpawnTitle = true;
    }

    @Category(value = "fishing")
    @ConfigInfo(title = "Fishing", description = "Fishing features.")
    public static final class Fishing {

        @ConfigEntry(id = "showBiteCountdownHud", translation = "gankura.config.fishing.showBiteCountdownHud")
        @Comment(value = "Shows how long until something bites while fishing.", translation = "gankura.config.fishing.showBiteCountdownHud.desc")
        public static boolean showBiteCountdownHud = false;

        @ConfigEntry(id = "showBaitHud", translation = "gankura.config.fishing.showBaitHud")
        @Comment(value = "Shows the bait on the rod and how much is left.", translation = "gankura.config.fishing.showBaitHud.desc")
        public static boolean showBaitHud = false;

        @ConfigEntry(id = "shortenSeaCreatureMessage", translation = "gankura.config.fishing.shortenSeaCreatureMessage")
        @Comment(value = "Replaces the long sea creature catch message with a short one.", translation = "gankura.config.fishing.shortenSeaCreatureMessage.desc")
        public static boolean shortenSeaCreatureMessage = false;

        @ConfigEntry(id = "showBaitLowAlert", translation = "gankura.config.fishing.showBaitLowAlert")
        @Comment(value = "Shows a title when the bait on the rod is running out.", translation = "gankura.config.fishing.showBaitLowAlert.desc")
        public static boolean showBaitLowAlert = false;

        @ConfigEntry(id = "baitLowThreshold", translation = "gankura.config.fishing.baitLowThreshold")
        @Comment(value = "How much bait is left when the alert shows.", translation = "gankura.config.fishing.baitLowThreshold.desc")
        @ConfigOption.Range(min = 1, max = 100000)
        public static int baitLowThreshold = 16;

        @ConfigEntry(id = "showCastTimer", translation = "gankura.config.fishing.showCastTimer")
        @Comment(value = "Shows how long the bobber has been floating, above the bobber.", translation = "gankura.config.fishing.showCastTimer.desc")
        public static boolean showCastTimer = false;

        @ConfigEntry(id = "castTimerOnLiquidTouch", translation = "gankura.config.fishing.castTimerOnLiquidTouch")
        @Comment(value = "Starts counting when the bobber lands, instead of when it is cast.", translation = "gankura.config.fishing.castTimerOnLiquidTouch.desc")
        public static boolean castTimerOnLiquidTouch = true;

        @ConfigEntry(id = "showHotspotGuess", translation = "gankura.config.fishing.showHotspotGuess")
        @Comment(value = "Reads the Hotspot Radar trail and marks where the hotspot should be.", translation = "gankura.config.fishing.showHotspotGuess.desc")
        public static boolean showHotspotGuess = false;

        @ConfigEntry(id = "showHotspotTracer", translation = "gankura.config.fishing.showHotspotTracer")
        @Comment(value = "Draws a line to the guessed hotspot.", translation = "gankura.config.fishing.showHotspotTracer.desc")
        public static boolean showHotspotTracer = false;

        @ConfigEntry(id = "showHotspotGoneTitle", translation = "gankura.config.fishing.showHotspotGoneTitle")
        @Comment(value = "Shows a title when the hotspot you are fishing in disappears.", translation = "gankura.config.fishing.showHotspotGoneTitle.desc")
        public static boolean showHotspotGoneTitle = false;

        @ConfigEntry(id = "showHotspotCircle", translation = "gankura.config.fishing.showHotspotCircle")
        @Comment(value = "Draws the hotspot edge as a circle.", translation = "gankura.config.fishing.showHotspotCircle.desc")
        public static boolean showHotspotCircle = false;

        @ConfigEntry(id = "hideHotspotParticles", translation = "gankura.config.fishing.hideHotspotParticles")
        @Comment(value = "Hides the particles Hypixel draws around the hotspot.", translation = "gankura.config.fishing.hideHotspotParticles.desc")
        public static boolean hideHotspotParticles = true;

        @ConfigEntry(id = "sharedHotspotPerks", translation = "gankura.config.fishing.sharedHotspotPerks")
        @Comment(value = "Hotspot perks worth being told about.\n§eNothing is picked to start with, so the "
            + "alert stays quiet\n§euntil you add the perks you care about.", translation = "gankura.config.fishing.sharedHotspotPerks.desc")
        @ConfigOption.Draggable({})
        public static HotspotPerk[] sharedHotspotPerks = new HotspotPerk[0];

        @ConfigEntry(id = "showHotspotFoundTitle", translation = "gankura.config.fishing.showHotspotFoundTitle")
        @Comment(value = "Shows a title naming the perk.", translation = "gankura.config.fishing.showHotspotFoundTitle.desc")
        public static boolean showHotspotFoundTitle = true;

        @ConfigEntry(id = "playHotspotFoundSound", translation = "gankura.config.fishing.playHotspotFoundSound")
        @Comment(value = "Plays an alert sound.", translation = "gankura.config.fishing.playHotspotFoundSound.desc")
        public static boolean playHotspotFoundSound = true;

        @ConfigEntry(id = "showHotspotFoundWaypoint", translation = "gankura.config.fishing.showHotspotFoundWaypoint")
        @Comment(value = "Marks the hotspot until it runs out.", translation = "gankura.config.fishing.showHotspotFoundWaypoint.desc")
        public static boolean showHotspotFoundWaypoint = true;

        @ConfigEntry(id = "showHotspotFoundTracer", translation = "gankura.config.fishing.showHotspotFoundTracer")
        @Comment(value = "Draws a line pointing at the waypoint.", translation = "gankura.config.fishing.showHotspotFoundTracer.desc")
        public static boolean showHotspotFoundTracer = true;

        @ConfigEntry(id = "hotspotFoundSeconds", translation = "gankura.config.fishing.hotspotFoundSeconds")
        @Comment(value = "Seconds the waypoint and tracer stay.", translation = "gankura.config.fishing.hotspotFoundSeconds.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 10, max = 300)
        public static int hotspotFoundSeconds = 60;

        @ConfigEntry(id = "shareHotspot", translation = "gankura.config.fishing.shareHotspot")
        @Comment(value = "Posts the coordinates in chat with a button that sends them to the channel "
            + "below.", translation = "gankura.config.fishing.shareHotspot.desc")
        public static boolean shareHotspot = false;

        @ConfigEntry(id = "hotspotShareChannel", translation = "gankura.config.fishing.hotspotShareChannel")
        @Comment(value = "Where the coordinates are sent.", translation = "gankura.config.fishing.hotspotShareChannel.desc")
        public static HotspotShareChannel hotspotShareChannel = HotspotShareChannel.PARTY;

        @ConfigEntry(id = "replaceLavaTexture", translation = "gankura.config.fishing.replaceLavaTexture")
        @Comment(value = "Draws lava with the water texture.", translation = "gankura.config.fishing.replaceLavaTexture.desc")
        public static boolean replaceLavaTexture = false;

        @ConfigEntry(id = "hideLavaFog", translation = "gankura.config.fishing.hideLavaFog")
        @Comment(value = "Clears the orange view while you are in lava.", translation = "gankura.config.fishing.hideLavaFog.desc")
        public static boolean hideLavaFog = true;

        @ConfigEntry(id = "lavaTextureArea", translation = "gankura.config.fishing.lavaTextureArea")
        @Comment(value = "Where lava is drawn as water.", translation = "gankura.config.fishing.lavaTextureArea.desc")
        public static LavaTextureArea lavaTextureArea = LavaTextureArea.CRIMSON_ISLE;

        @ConfigEntry(id = "showGoldenFishTimer", translation = "gankura.config.fishing.showGoldenFishTimer")
        @Comment(value = "Tracks the Golden Fish on the Crimson Isle. Everything below needs this on.", translation = "gankura.config.fishing.showGoldenFishTimer.desc")
        public static boolean showGoldenFishTimer = false;

        @ConfigEntry(id = "highlightGoldenFish", translation = "gankura.config.fishing.highlightGoldenFish")
        @Comment(value = "Outlines the Golden Fish once it surfaces.", translation = "gankura.config.fishing.highlightGoldenFish.desc")
        public static boolean highlightGoldenFish = true;

        @ConfigEntry(id = "tracerGoldenFish", translation = "gankura.config.fishing.tracerGoldenFish")
        @Comment(value = "Draws a line to the Golden Fish once it surfaces.", translation = "gankura.config.fishing.tracerGoldenFish.desc")
        public static boolean tracerGoldenFish = true;

        @ConfigEntry(id = "warnGoldenFishRod", translation = "gankura.config.fishing.warnGoldenFishRod")
        @Comment(value = "Warns before the spawn window is lost because the rod has not been thrown.", translation = "gankura.config.fishing.warnGoldenFishRod.desc")
        public static boolean warnGoldenFishRod = true;

        @ConfigEntry(id = "goldenFishRodWarningSeconds", translation = "gankura.config.fishing.goldenFishRodWarningSeconds")
        @Comment(value = "Seconds left when the warning shows.", translation = "gankura.config.fishing.goldenFishRodWarningSeconds.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 5, max = 60)
        public static int goldenFishRodWarningSeconds = 20;

        @ConfigEntry(id = "goldfinShardLevel", translation = "gankura.config.fishing.goldfinShardLevel")
        @Comment(value = "Cuts 30 seconds off the spawn window per level.", translation = "gankura.config.fishing.goldfinShardLevel.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 0, max = 10)
        public static int goldfinShardLevel = 0;

        // Attribute Menu を一度でも読めたか。
        // レベル0と「未確認」を見分けるために持つ。
        // スライダーを手で動かしても確認済みにはならないので、設定画面には出さない
        @ConfigEntry(id = "goldfinShardRead")
        @ConfigOption.Hidden
        public static boolean goldfinShardRead = false;
    }

    @Category(value = "mining")
    @ConfigInfo(title = "Mining", description = "Mining features.")
    public static final class Mining {

        @ConfigEntry(id = "highlightCompletedCommissions", translation = "gankura.config.mining.highlightCompletedCommissions")
        @Comment(value = "Paints the commissions that are done, so the ones waiting to be claimed stand "
            + "out.", translation = "gankura.config.mining.highlightCompletedCommissions.desc")
        public static boolean highlightCompletedCommissions = false;

        @ConfigEntry(id = "showGemstoneCommissionWaypoints", translation = "gankura.config.mining.showGemstoneCommissionWaypoints")
        @Comment(value = "Labels the gemstone spots in the Glacite Tunnels while you have that gemstone's "
            + "commission.", translation = "gankura.config.mining.showGemstoneCommissionWaypoints.desc")
        public static boolean showGemstoneCommissionWaypoints = false;

        @ConfigEntry(id = "showCorpseWaypoints", translation = "gankura.config.mining.showCorpseWaypoints")
        @Comment(value = "Labels the frozen corpses in the mineshaft, with the kind of key each one needs.", translation = "gankura.config.mining.showCorpseWaypoints.desc")
        public static boolean showCorpseWaypoints = false;

        @ConfigEntry(id = "showCorpseTracer", translation = "gankura.config.mining.showCorpseTracer")
        @Comment(value = "Draws a line to each frozen corpse, in the colour of the key it needs.", translation = "gankura.config.mining.showCorpseTracer.desc")
        public static boolean showCorpseTracer = true;

        @ConfigEntry(id = "showVanguardTitle", translation = "gankura.config.mining.showVanguardTitle")
        @Comment(value = "Shows a title when a Vanguard corpse turns up in the mineshaft you are in.", translation = "gankura.config.mining.showVanguardTitle.desc")
        public static boolean showVanguardTitle = false;

        @ConfigEntry(id = "showCorpseProfit", translation = "gankura.config.mining.showCorpseProfit")
        @Comment(value = "Adds up what a corpse dropped and posts it in chat. The key each corpse needs is "
            + "taken off the total.", translation = "gankura.config.mining.showCorpseProfit.desc")
        public static boolean showCorpseProfit = false;

        @ConfigEntry(id = "solveWishingCompass", translation = "gankura.config.mining.solveWishingCompass")
        @Comment(value = "Works out where the wishing compass points. Use it from two spots at least 8 "
            + "blocks apart.", translation = "gankura.config.mining.solveWishingCompass.desc")
        public static boolean solveWishingCompass = false;

        @ConfigEntry(id = "showWishingCompassWaypoint", translation = "gankura.config.mining.showWishingCompassWaypoint")
        @Comment(value = "Marks the solved location in the world.", translation = "gankura.config.mining.showWishingCompassWaypoint.desc")
        public static boolean showWishingCompassWaypoint = false;
    }

    @Category(value = "generalHud")
    @ConfigInfo(title = "General HUD", description = "The HUDs that stay on screen everywhere.")
    public static final class GeneralHud {

        @ConfigEntry(id = "showEquipmentHud", translation = "gankura.config.generalHud.showEquipmentHud")
        @Comment(value = "Shows equipped armor.", translation = "gankura.config.generalHud.showEquipmentHud.desc")
        public static boolean showEquipmentHud = false;

        @ConfigEntry(id = "equipmentHudOrientation", translation = "gankura.config.generalHud.equipmentHudOrientation")
        @Comment(value = "Sets horizontal or vertical.", translation = "gankura.config.generalHud.equipmentHudOrientation.desc")
        public static HudOrientation equipmentHudOrientation = HudOrientation.HORIZONTAL;

        @ConfigEntry(id = "showGearHud", translation = "gankura.config.generalHud.showGearHud")
        @Comment(value = "Shows Necklace/Cloak/Belt/Gloves.", translation = "gankura.config.generalHud.showGearHud.desc")
        public static boolean showGearHud = false;

        @ConfigEntry(id = "gearHudOrientation", translation = "gankura.config.generalHud.gearHudOrientation")
        @Comment(value = "Sets horizontal or vertical.", translation = "gankura.config.generalHud.gearHudOrientation.desc")
        public static HudOrientation gearHudOrientation = HudOrientation.HORIZONTAL;

        @ConfigEntry(id = "showYawPitchHud", translation = "gankura.config.generalHud.showYawPitchHud")
        @Comment(value = "Shows where you are looking.", translation = "gankura.config.generalHud.showYawPitchHud.desc")
        public static boolean showYawPitchHud = false;

        @ConfigEntry(id = "yawPrecision", translation = "gankura.config.generalHud.yawPrecision")
        @Comment(value = "Sets decimals shown for yaw.", translation = "gankura.config.generalHud.yawPrecision.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 1, max = 10)
        public static int yawPrecision = 4;

        @ConfigEntry(id = "pitchPrecision", translation = "gankura.config.generalHud.pitchPrecision")
        @Comment(value = "Sets decimals shown for pitch.", translation = "gankura.config.generalHud.pitchPrecision.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 1, max = 10)
        public static int pitchPrecision = 4;

        @ConfigEntry(id = "showPetHud", translation = "gankura.config.generalHud.showPetHud")
        @Comment(value = "Shows active pet.", translation = "gankura.config.generalHud.showPetHud.desc")
        public static boolean showPetHud = false;

        @ConfigEntry(id = "showTpsHud", translation = "gankura.config.generalHud.showTpsHud")
        @Comment(value = "Shows server TPS.", translation = "gankura.config.generalHud.showTpsHud.desc")
        public static boolean showTpsHud = false;

        @ConfigEntry(id = "showDayHud", translation = "gankura.config.generalHud.showDayHud")
        @Comment(value = "Shows lobby day.", translation = "gankura.config.generalHud.showDayHud.desc")
        public static boolean showDayHud = false;
    }

    @Category(value = "mobVisuals")
    @ConfigInfo(title = "Mob Visuals", description = "Which mobs get a highlight, tracer or nameplate.")
    public static final class MobVisuals {


        // ネームプレートの基準サイズ。1.0 が GUI スケール4相当の見かけになる
        public static final float DEFAULT_NAMEPLATE_SCALE = 1.0f;

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

        /** 「なし」ボタン用。要素数0の同じ型の配列を返す */
        private static <T> T[] empty(T[] values) {
            return java.util.Arrays.copyOf(values, 0);
        }

        @ConfigEntry(id = "enableHighlight", translation = "gankura.config.mobVisuals.enableHighlight")
        @Comment(value = "Outlines the target mobs with a glow.", translation = "gankura.config.mobVisuals.enableHighlight.desc")
        public static boolean enableHighlight = true;

        @ConfigEntry(id = "enableTracer", translation = "gankura.config.mobVisuals.enableTracer")
        @Comment(value = "Draws a line to the target mobs.", translation = "gankura.config.mobVisuals.enableTracer.desc")
        public static boolean enableTracer = true;

        @ConfigEntry(id = "tracerMode", translation = "gankura.config.mobVisuals.tracerMode")
        @Comment(value = "Nearest: only the closest mob of each kind. All: every mob found.", translation = "gankura.config.mobVisuals.tracerMode.desc")
        public static TracerMode tracerMode = TracerMode.NEAREST;

        @ConfigEntry(id = "enableNameplate", translation = "gankura.config.mobVisuals.enableNameplate")
        @Comment(value = "Shows a nameplate on the target mobs.", translation = "gankura.config.mobVisuals.enableNameplate.desc")
        public static boolean enableNameplate = true;

        @ConfigEntry(id = "nameplateScale", translation = "gankura.config.mobVisuals.nameplateScale")
        @Comment(value = "Changes nameplate text size.", translation = "gankura.config.mobVisuals.nameplateScale.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 0.25, max = 3.0)
        public static float nameplateScale = DEFAULT_NAMEPLATE_SCALE;

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.resetNameplateScale", text = "gankura.config.mobVisuals.resetNameplateScale.button")
        @Comment(value = "Reset to default.")
        public static final Runnable resetNameplateScale = () -> nameplateScale = DEFAULT_NAMEPLATE_SCALE;

        @ConfigEntry(id = "showNameplateHealth", translation = "gankura.config.mobVisuals.showNameplateHealth")
        @Comment(value = "Shows the mob's health under its name.", translation = "gankura.config.mobVisuals.showNameplateHealth.desc")
        public static boolean showNameplateHealth = true;

        @ConfigEntry(id = "showNameplateCapsule", translation = "gankura.config.mobVisuals.showNameplateCapsule")
        @Comment(value = "Shows how many Critter Capsules have been thrown, under the mob's name. Only "
            + "Wumpa and Doomspiral have this.", translation = "gankura.config.mobVisuals.showNameplateCapsule.desc")
        public static boolean showNameplateCapsule = true;

        @ConfigEntry(id = "targetsTheEnd", translation = "gankura.config.mobVisuals.targetsTheEnd")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsTheEnd.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.TheEnd[] targetsTheEnd = new MobVisual.TheEnd[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllTheEnd", text = "gankura.config.mobVisuals.enableAllTheEnd.button")
        @Comment(value = "Puts every The End mob into the list above.")
        public static final Runnable enableAllTheEnd = () -> targetsTheEnd = MobVisual.TheEnd.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllTheEnd", text = "gankura.config.mobVisuals.disableAllTheEnd.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllTheEnd = () -> targetsTheEnd = empty(targetsTheEnd);

        @ConfigEntry(id = "targetsSpidersDen", translation = "gankura.config.mobVisuals.targetsSpidersDen")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSpidersDen.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SpidersDen[] targetsSpidersDen = new MobVisual.SpidersDen[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSpidersDen", text = "gankura.config.mobVisuals.enableAllSpidersDen.button")
        @Comment(value = "Puts every Spider's Den mob into the list above.")
        public static final Runnable enableAllSpidersDen = () -> targetsSpidersDen = MobVisual.SpidersDen.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSpidersDen", text = "gankura.config.mobVisuals.disableAllSpidersDen.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSpidersDen = () -> targetsSpidersDen = empty(targetsSpidersDen);

        @ConfigEntry(id = "targetsCrimsonIsle", translation = "gankura.config.mobVisuals.targetsCrimsonIsle")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsCrimsonIsle.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.CrimsonIsle[] targetsCrimsonIsle = new MobVisual.CrimsonIsle[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllCrimsonIsle", text = "gankura.config.mobVisuals.enableAllCrimsonIsle.button")
        @Comment(value = "Puts every Crimson Isle mob into the list above.")
        public static final Runnable enableAllCrimsonIsle = () -> targetsCrimsonIsle = MobVisual.CrimsonIsle.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllCrimsonIsle", text = "gankura.config.mobVisuals.disableAllCrimsonIsle.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllCrimsonIsle = () -> targetsCrimsonIsle = empty(targetsCrimsonIsle);

        @ConfigEntry(id = "targetsCrystalHollows", translation = "gankura.config.mobVisuals.targetsCrystalHollows")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsCrystalHollows.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.CrystalHollows[] targetsCrystalHollows = new MobVisual.CrystalHollows[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllCrystalHollows", text = "gankura.config.mobVisuals.enableAllCrystalHollows.button")
        @Comment(value = "Puts every Crystal Hollows mob into the list above.")
        public static final Runnable enableAllCrystalHollows = () -> targetsCrystalHollows = MobVisual.CrystalHollows.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllCrystalHollows", text = "gankura.config.mobVisuals.disableAllCrystalHollows.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllCrystalHollows = () -> targetsCrystalHollows = empty(targetsCrystalHollows);

        @ConfigEntry(id = "targetsMoongladeMarsh", translation = "gankura.config.mobVisuals.targetsMoongladeMarsh")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsMoongladeMarsh.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.MoongladeMarsh[] targetsMoongladeMarsh = new MobVisual.MoongladeMarsh[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllMoongladeMarsh", text = "gankura.config.mobVisuals.enableAllMoongladeMarsh.button")
        @Comment(value = "Puts every Moonglade Marsh mob into the list above.")
        public static final Runnable enableAllMoongladeMarsh = () -> targetsMoongladeMarsh = MobVisual.MoongladeMarsh.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllMoongladeMarsh", text = "gankura.config.mobVisuals.disableAllMoongladeMarsh.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllMoongladeMarsh = () -> targetsMoongladeMarsh = empty(targetsMoongladeMarsh);

        @ConfigEntry(id = "targetsTorrhusCanyon", translation = "gankura.config.mobVisuals.targetsTorrhusCanyon")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsTorrhusCanyon.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.TorrhusCanyon[] targetsTorrhusCanyon = new MobVisual.TorrhusCanyon[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllTorrhusCanyon", text = "gankura.config.mobVisuals.enableAllTorrhusCanyon.button")
        @Comment(value = "Puts every Torrhus Canyon mob into the list above.")
        public static final Runnable enableAllTorrhusCanyon = () -> targetsTorrhusCanyon = MobVisual.TorrhusCanyon.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllTorrhusCanyon", text = "gankura.config.mobVisuals.disableAllTorrhusCanyon.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllTorrhusCanyon = () -> targetsTorrhusCanyon = empty(targetsTorrhusCanyon);

        @ConfigEntry(id = "targetsLotusAtoll", translation = "gankura.config.mobVisuals.targetsLotusAtoll")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsLotusAtoll.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.LotusAtoll[] targetsLotusAtoll = new MobVisual.LotusAtoll[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllLotusAtoll", text = "gankura.config.mobVisuals.enableAllLotusAtoll.button")
        @Comment(value = "Puts every Lotus Atoll mob into the list above.")
        public static final Runnable enableAllLotusAtoll = () -> targetsLotusAtoll = MobVisual.LotusAtoll.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllLotusAtoll", text = "gankura.config.mobVisuals.disableAllLotusAtoll.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllLotusAtoll = () -> targetsLotusAtoll = empty(targetsLotusAtoll);

        @ConfigEntry(id = "targetsGlaciteMineshaft", translation = "gankura.config.mobVisuals.targetsGlaciteMineshaft")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsGlaciteMineshaft.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.GlaciteMineshaft[] targetsGlaciteMineshaft = new MobVisual.GlaciteMineshaft[0];

        @ConfigButton(title = "gankura.config.mobVisuals.enableAllGlaciteMineshaft", text = "gankura.config.mobVisuals.enableAllGlaciteMineshaft.button")
        @Comment(value = "Puts every Glacite Mineshaft mob into the list above.")
        public static final Runnable enableAllGlaciteMineshaft = 
                () -> targetsGlaciteMineshaft = MobVisual.GlaciteMineshaft.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllGlaciteMineshaft", text = "gankura.config.mobVisuals.disableAllGlaciteMineshaft.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllGlaciteMineshaft = () -> targetsGlaciteMineshaft = empty(targetsGlaciteMineshaft);

        @ConfigEntry(id = "targetsGarden", translation = "gankura.config.mobVisuals.targetsGarden")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsGarden.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.Garden[] targetsGarden = new MobVisual.Garden[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllGarden", text = "gankura.config.mobVisuals.enableAllGarden.button")
        @Comment(value = "Puts every Garden pest into the list above.")
        public static final Runnable enableAllGarden = () -> targetsGarden = MobVisual.Garden.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllGarden", text = "gankura.config.mobVisuals.disableAllGarden.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllGarden = () -> targetsGarden = empty(targetsGarden);

        @ConfigEntry(id = "hideCapturedCritters", translation = "gankura.config.mobVisuals.hideCapturedCritters")
        @Comment(value = "Drops critters already captured from Highlight, Tracer and Nameplate. The "
            + "Captured Critters HUD keeps showing them.", translation = "gankura.config.mobVisuals.hideCapturedCritters.desc")
        public static boolean hideCapturedCritters = false;

        @ConfigEntry(id = "keepShownSafariCavern", translation = "gankura.config.mobVisuals.keepShownSafariCavern")
        @Comment(value = "Cavern critters to leave showing even after they have been captured.", translation = "gankura.config.mobVisuals.keepShownSafariCavern.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariCavern[] keepShownSafariCavern = new MobVisual.SafariCavern[0];

        @ConfigEntry(id = "keepShownSafariForest", translation = "gankura.config.mobVisuals.keepShownSafariForest")
        @Comment(value = "Forest critters to leave showing even after they have been captured.", translation = "gankura.config.mobVisuals.keepShownSafariForest.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariForest[] keepShownSafariForest = new MobVisual.SafariForest[0];

        @ConfigEntry(id = "keepShownSafariHaunted", translation = "gankura.config.mobVisuals.keepShownSafariHaunted")
        @Comment(value = "Haunted critters to leave showing even after they have been captured.", translation = "gankura.config.mobVisuals.keepShownSafariHaunted.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariHaunted[] keepShownSafariHaunted = new MobVisual.SafariHaunted[0];

        @ConfigEntry(id = "keepShownSafariIcy", translation = "gankura.config.mobVisuals.keepShownSafariIcy")
        @Comment(value = "Icy critters to leave showing even after they have been captured.", translation = "gankura.config.mobVisuals.keepShownSafariIcy.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariIcy[] keepShownSafariIcy = new MobVisual.SafariIcy[0];

        @ConfigEntry(id = "targetsSafariCavern", translation = "gankura.config.mobVisuals.targetsSafariCavern")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSafariCavern.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariCavern[] targetsSafariCavern = new MobVisual.SafariCavern[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSafariCavern", text = "gankura.config.mobVisuals.enableAllSafariCavern.button")
        @Comment(value = "Puts every Cavern mob into the list above.")
        public static final Runnable enableAllSafariCavern = () -> targetsSafariCavern = MobVisual.SafariCavern.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSafariCavern", text = "gankura.config.mobVisuals.disableAllSafariCavern.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSafariCavern = () -> targetsSafariCavern = empty(targetsSafariCavern);

        @ConfigEntry(id = "targetsSafariForest", translation = "gankura.config.mobVisuals.targetsSafariForest")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSafariForest.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariForest[] targetsSafariForest = new MobVisual.SafariForest[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSafariForest", text = "gankura.config.mobVisuals.enableAllSafariForest.button")
        @Comment(value = "Puts every Forest mob into the list above.")
        public static final Runnable enableAllSafariForest = () -> targetsSafariForest = MobVisual.SafariForest.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSafariForest", text = "gankura.config.mobVisuals.disableAllSafariForest.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSafariForest = () -> targetsSafariForest = empty(targetsSafariForest);

        @ConfigEntry(id = "targetsSafariHaunted", translation = "gankura.config.mobVisuals.targetsSafariHaunted")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSafariHaunted.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariHaunted[] targetsSafariHaunted = new MobVisual.SafariHaunted[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSafariHaunted", text = "gankura.config.mobVisuals.enableAllSafariHaunted.button")
        @Comment(value = "Puts every Haunted mob into the list above.")
        public static final Runnable enableAllSafariHaunted = () -> targetsSafariHaunted = MobVisual.SafariHaunted.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSafariHaunted", text = "gankura.config.mobVisuals.disableAllSafariHaunted.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSafariHaunted = () -> targetsSafariHaunted = empty(targetsSafariHaunted);

        @ConfigEntry(id = "targetsSafariIcy", translation = "gankura.config.mobVisuals.targetsSafariIcy")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSafariIcy.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SafariIcy[] targetsSafariIcy = new MobVisual.SafariIcy[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSafariIcy", text = "gankura.config.mobVisuals.enableAllSafariIcy.button")
        @Comment(value = "Puts every Icy mob into the list above.")
        public static final Runnable enableAllSafariIcy = () -> targetsSafariIcy = MobVisual.SafariIcy.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSafariIcy", text = "gankura.config.mobVisuals.disableAllSafariIcy.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSafariIcy = () -> targetsSafariIcy = empty(targetsSafariIcy);

        @ConfigEntry(id = "enableSeaCreatureTitle", translation = "gankura.config.mobVisuals.enableSeaCreatureTitle")
        @Comment(value = "Shows a title with the name when a listed Sea Creature is found.", translation = "gankura.config.mobVisuals.enableSeaCreatureTitle.desc")
        public static boolean enableSeaCreatureTitle = true;

        @ConfigEntry(id = "enableSeaCreatureSound", translation = "gankura.config.mobVisuals.enableSeaCreatureSound")
        @Comment(value = "Plays a sound together with the title.", translation = "gankura.config.mobVisuals.enableSeaCreatureSound.desc")
        public static boolean enableSeaCreatureSound = true;

        @ConfigEntry(id = "targetsSeaCreature", translation = "gankura.config.mobVisuals.targetsSeaCreature")
        @Comment(value = "Mobs to show. Applies to highlight, tracer and nameplate.", translation = "gankura.config.mobVisuals.targetsSeaCreature.desc")
        @ConfigOption.Draggable({})
        public static MobVisual.SeaCreature[] targetsSeaCreature = new MobVisual.SeaCreature[0];

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.mobVisuals.enableAllSeaCreature", text = "gankura.config.mobVisuals.enableAllSeaCreature.button")
        @Comment(value = "Puts every Sea Creature into the list above.")
        public static final Runnable enableAllSeaCreature = () -> targetsSeaCreature = MobVisual.SeaCreature.values();

        @ConfigButton(title = "gankura.config.mobVisuals.disableAllSeaCreature", text = "gankura.config.mobVisuals.disableAllSeaCreature.button")
        @Comment(value = "Empties the list above.")
        public static final Runnable disableAllSeaCreature = () -> targetsSeaCreature = empty(targetsSeaCreature);
    }

    @Category(value = "waypoints")
    @ConfigInfo(title = "Custom Waypoints", description = "Your own waypoints.")
    public static final class Waypoints {

        // MoulConfig では表を出せないので専用の画面を開く
        @ConfigButton(title = "gankura.config.waypoints.openWaypointScreen", text = "gankura.config.waypoints.openWaypointScreen.button")
        @Comment(value = "Opens the waypoint list of the area you are in.")
        public static final Runnable openWaypointScreen = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().gui.setScreen(new WaypointScreen(Minecraft.getInstance().gui.screen()));
            });
        };
    }

    @Category(value = "inventoryButtons")
    @ConfigInfo(title = "Inventory Buttons", description = "Buttons around inventory menus that run commands.")
    public static final class InventoryButtons {

        @ConfigEntry(id = "enableInventoryButtons", translation = "gankura.config.inventoryButtons.enableInventoryButtons")
        @Comment(value = "Shows the buttons around inventory menus.", translation = "gankura.config.inventoryButtons.enableInventoryButtons.desc")
        public static boolean enableInventoryButtons = true;

        // ボタンは保存対象外なので @Expose を付けず transient にする
        @ConfigButton(title = "gankura.config.inventoryButtons.openButtonEditor", text = "gankura.config.inventoryButtons.openButtonEditor.button")
        @Comment(value = "Opens the screen where buttons are placed and edited.")
        public static final Runnable openButtonEditor = () -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().gui.setScreen(
                        new InventoryButtonEditorScreen(Minecraft.getInstance().gui.screen()));
            });
        };

        @ConfigEntry(id = "hideInDungeonMenus", translation = "gankura.config.inventoryButtons.hideInDungeonMenus")
        @Comment(value = "Hides the buttons in dungeon puzzle menus.", translation = "gankura.config.inventoryButtons.hideInDungeonMenus.desc")
        public static boolean hideInDungeonMenus = false;

        @ConfigEntry(id = "clickType", translation = "gankura.config.inventoryButtons.clickType")
        @Comment(value = "When the command runs: on press or on release.", translation = "gankura.config.inventoryButtons.clickType.desc")
        public static ButtonClickType clickType = ButtonClickType.MOUSE_DOWN;

        @ConfigEntry(id = "tooltipDelay", translation = "gankura.config.inventoryButtons.tooltipDelay")
        @Comment(value = "Milliseconds the cursor has to rest on a button before its command is shown.", translation = "gankura.config.inventoryButtons.tooltipDelay.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 0, max = 1500)
        public static int tooltipDelay = 600;
    }

    @Category(value = "heldItem")
    @ConfigInfo(title = "Viewmodel", description = "Size and position of the item in your hand.")
    public static final class HeldItem {

        @ConfigEntry(id = "heldItemScale", translation = "gankura.config.heldItem.heldItemScale")
        @Comment(value = "Changes held item size.", translation = "gankura.config.heldItem.heldItemScale.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 0.01, max = 1.0)
        public static float heldItemScale = 1.0f;

        // ボタンには @Expose は付けず、代わりに transient を付けます！
        @ConfigButton(title = "gankura.config.heldItem.resetHeldItemScale", text = "gankura.config.heldItem.resetHeldItemScale.button")
        @Comment(value = "Reset to default.")
        public static final Runnable resetHeldItemScale = () -> heldItemScale = 1.0f;

        @ConfigEntry(id = "heldItemOffsetX", translation = "gankura.config.heldItem.heldItemOffsetX")
        @Comment(value = "Shifts item horizontally.", translation = "gankura.config.heldItem.heldItemOffsetX.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = -1.0, max = 1.0)
        public static float heldItemOffsetX = 0.0f;

        @ConfigButton(title = "gankura.config.heldItem.resetHeldItemOffsetX", text = "gankura.config.heldItem.resetHeldItemOffsetX.button")
        @Comment(value = "Reset to default.")
        public static final Runnable resetHeldItemOffsetX = () -> heldItemOffsetX = 0.0f;

        @ConfigEntry(id = "heldItemOffsetY", translation = "gankura.config.heldItem.heldItemOffsetY")
        @Comment(value = "Shifts item vertically.", translation = "gankura.config.heldItem.heldItemOffsetY.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = -1.0, max = 1.0)
        public static float heldItemOffsetY = 0.0f;

        @ConfigButton(title = "gankura.config.heldItem.resetHeldItemOffsetY", text = "gankura.config.heldItem.resetHeldItemOffsetY.button")
        @Comment(value = "Reset to default.")
        public static final Runnable resetHeldItemOffsetY = () -> heldItemOffsetY = 0.0f;
    }

    @Category(value = "interfaceSettings")
    @ConfigInfo(title = "Interface", description = "Tweaks to the vanilla menus, tooltips and tab list.")
    public static final class Interface {

        @ConfigEntry(id = "highlightOpacity", translation = "gankura.config.interfaceSettings.highlightOpacity")
        @Comment(value = "How solid the coloured slot highlights are drawn behind the items.", translation = "gankura.config.interfaceSettings.highlightOpacity.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 0, max = 100)
        public static int highlightOpacity = 50;

        @ConfigEntry(id = "enableSearchInputScreen", translation = "gankura.config.interfaceSettings.enableSearchInputScreen")
        @Comment(value = "Replaces the sign used by every search box with a text box.", translation = "gankura.config.interfaceSettings.enableSearchInputScreen.desc")
        public static boolean enableSearchInputScreen = true;

        @ConfigEntry(id = "enableInventoryTweaks", translation = "gankura.config.interfaceSettings.enableInventoryTweaks")
        @Comment(value = "Turns on the inventory tweaks below.", translation = "gankura.config.interfaceSettings.enableInventoryTweaks.desc")
        public static boolean enableInventoryTweaks = false;

        @ConfigEntry(id = "hideInventoryEffects", translation = "gankura.config.interfaceSettings.hideInventoryEffects")
        @Comment(value = "Hides the effect list beside the inventory.", translation = "gankura.config.interfaceSettings.hideInventoryEffects.desc")
        public static boolean hideInventoryEffects = true;

        @ConfigEntry(id = "hideInventoryDim", translation = "gankura.config.interfaceSettings.hideInventoryDim")
        @Comment(value = "Removes the dark tint behind inventory menus.", translation = "gankura.config.interfaceSettings.hideInventoryDim.desc")
        public static boolean hideInventoryDim = true;

        @ConfigEntry(id = "hideRecipeBook", translation = "gankura.config.interfaceSettings.hideRecipeBook")
        @Comment(value = "Hides the recipe book and its button.", translation = "gankura.config.interfaceSettings.hideRecipeBook.desc")
        public static boolean hideRecipeBook = true;

        @ConfigEntry(id = "enableTabListTweaks", translation = "gankura.config.interfaceSettings.enableTabListTweaks")
        @Comment(value = "Turns on the tab list tweaks below.", translation = "gankura.config.interfaceSettings.enableTabListTweaks.desc")
        public static boolean enableTabListTweaks = false;

        @ConfigEntry(id = "hideTabListAds", translation = "gankura.config.interfaceSettings.hideTabListAds")
        @Comment(value = "Hides the Hypixel adverts above and below the tab list.", translation = "gankura.config.interfaceSettings.hideTabListAds.desc")
        public static boolean hideTabListAds = true;

        @ConfigEntry(id = "hideTabListPing", translation = "gankura.config.interfaceSettings.hideTabListPing")
        @Comment(value = "Hides the connection bars on every row.", translation = "gankura.config.interfaceSettings.hideTabListPing.desc")
        public static boolean hideTabListPing = true;

        @ConfigEntry(id = "hideTabListHeads", translation = "gankura.config.interfaceSettings.hideTabListHeads")
        @Comment(value = "Hides the head on rows that are not a player.", translation = "gankura.config.interfaceSettings.hideTabListHeads.desc")
        public static boolean hideTabListHeads = true;

        @ConfigEntry(id = "compactTabList", translation = "gankura.config.interfaceSettings.compactTabList")
        @Comment(value = "Drops the duplicated rows and the empty columns.", translation = "gankura.config.interfaceSettings.compactTabList.desc")
        public static boolean compactTabList = true;

        @ConfigEntry(id = "fitTabColumns", translation = "gankura.config.interfaceSettings.fitTabColumns")
        @Comment(value = "Sizes every column to its own longest line.", translation = "gankura.config.interfaceSettings.fitTabColumns.desc")
        public static boolean fitTabColumns = true;

        @ConfigEntry(id = "shrinkTabList", translation = "gankura.config.interfaceSettings.shrinkTabList")
        @Comment(value = "Scales the whole list down when it is wider than the screen.", translation = "gankura.config.interfaceSettings.shrinkTabList.desc")
        public static boolean shrinkTabList = true;

        @ConfigEntry(id = "enableItemTooltipTweaks", translation = "gankura.config.interfaceSettings.enableItemTooltipTweaks")
        @Comment(value = "Turns on the tooltip tweaks below, Enchant and Item Price included.", translation = "gankura.config.interfaceSettings.enableItemTooltipTweaks.desc")
        public static boolean enableItemTooltipTweaks = false;

        @ConfigEntry(id = "enableScrollableTooltips", translation = "gankura.config.interfaceSettings.enableScrollableTooltips")
        @Comment(value = "Moves the tooltip of the item under the cursor with the mouse wheel.", translation = "gankura.config.interfaceSettings.enableScrollableTooltips.desc")
        public static boolean enableScrollableTooltips = true;

        @ConfigEntry(id = "invertTooltipScroll", translation = "gankura.config.interfaceSettings.invertTooltipScroll")
        @Comment(value = "Turns the wheel the other way when moving a tooltip.", translation = "gankura.config.interfaceSettings.invertTooltipScroll.desc")
        public static boolean invertTooltipScroll = false;

        @ConfigEntry(id = "tooltipFromTop", translation = "gankura.config.interfaceSettings.tooltipFromTop")
        @Comment(value = "Starts tooltips that do not fit on screen at the top instead.", translation = "gankura.config.interfaceSettings.tooltipFromTop.desc")
        public static boolean tooltipFromTop = false;

        @ConfigEntry(id = "enableEnchantTooltipTweaks", translation = "gankura.config.interfaceSettings.enableEnchantTooltipTweaks")
        @Comment(value = "Turns on the enchantment tweaks below.", translation = "gankura.config.interfaceSettings.enableEnchantTooltipTweaks.desc")
        public static boolean enableEnchantTooltipTweaks = false;

        @ConfigEntry(id = "enableNumericEnchantTiers", translation = "gankura.config.interfaceSettings.enableNumericEnchantTiers")
        @Comment(value = "Writes enchantment levels as numbers instead of Roman numerals.", translation = "gankura.config.interfaceSettings.enableNumericEnchantTiers.desc")
        public static boolean enableNumericEnchantTiers = true;

        @ConfigEntry(id = "enableMaxEnchantChroma", translation = "gankura.config.interfaceSettings.enableMaxEnchantChroma")
        @Comment(value = "Cycles the colour of enchantments that are already at their maximum level.", translation = "gankura.config.interfaceSettings.enableMaxEnchantChroma.desc")
        public static boolean enableMaxEnchantChroma = true;

        @ConfigEntry(id = "enableBookEnchantGold", translation = "gankura.config.interfaceSettings.enableBookEnchantGold")
        @Comment(value = "Colours enchantments above the enchantment table's highest tier in gold.", translation = "gankura.config.interfaceSettings.enableBookEnchantGold.desc")
        public static boolean enableBookEnchantGold = true;

        @ConfigEntry(id = "ignoreUltimateEnchants", translation = "gankura.config.interfaceSettings.ignoreUltimateEnchants")
        @Comment(value = "Leaves Ultimate Enchantments in their normal colour.", translation = "gankura.config.interfaceSettings.ignoreUltimateEnchants.desc")
        public static boolean ignoreUltimateEnchants = true;

        @ConfigEntry(id = "enableItemPrice", translation = "gankura.config.interfaceSettings.enableItemPrice")
        @Comment(value = "Writes the market price at the bottom of item tooltips.", translation = "gankura.config.interfaceSettings.enableItemPrice.desc")
        public static boolean enableItemPrice = false;

        @ConfigEntry(id = "showLowestBin", translation = "gankura.config.interfaceSettings.showLowestBin")
        @Comment(value = "Shows the cheapest Buy It Now price on the Auction House.", translation = "gankura.config.interfaceSettings.showLowestBin.desc")
        public static boolean showLowestBin = true;

        @ConfigEntry(id = "showBazaarPrice", translation = "gankura.config.interfaceSettings.showBazaarPrice")
        @Comment(value = "Shows the price the Bazaar buys and sells at.", translation = "gankura.config.interfaceSettings.showBazaarPrice.desc")
        public static boolean showBazaarPrice = true;

        @ConfigEntry(id = "bazaarPriceType", translation = "gankura.config.interfaceSettings.bazaarPriceType")
        @Comment(value = "Which side of the Bazaar to show.", translation = "gankura.config.interfaceSettings.bazaarPriceType.desc")
        public static BazaarPriceType bazaarPriceType = BazaarPriceType.BOTH;

        @ConfigEntry(id = "showCraftCost", translation = "gankura.config.interfaceSettings.showCraftCost")
        @Comment(value = "Shows what the materials of one craft cost, buying them now and by order.", translation = "gankura.config.interfaceSettings.showCraftCost.desc")
        public static boolean showCraftCost = true;

        @ConfigEntry(id = "showStackPrice", translation = "gankura.config.interfaceSettings.showStackPrice")
        @Comment(value = "Adds the price of the whole stack when you hold more than one.", translation = "gankura.config.interfaceSettings.showStackPrice.desc")
        public static boolean showStackPrice = true;

        @ConfigEntry(id = "shortPriceNumbers", translation = "gankura.config.interfaceSettings.shortPriceNumbers")
        @Comment(value = "Writes large prices as 1.2M instead of 1,200,000.", translation = "gankura.config.interfaceSettings.shortPriceNumbers.desc")
        public static boolean shortPriceNumbers = true;

        @ConfigEntry(id = "enableAuctionTweaks", translation = "gankura.config.interfaceSettings.enableAuctionTweaks")
        @Comment(value = "Turns on the Auction House and Bazaar tweaks below.", translation = "gankura.config.interfaceSettings.enableAuctionTweaks.desc")
        public static boolean enableAuctionTweaks = false;

        @ConfigEntry(id = "highlightBazaarOrders", translation = "gankura.config.interfaceSettings.highlightBazaarOrders")
        @Comment(value = "Marks your own Bazaar orders. Yellow when someone outbid you, green when the "
            + "order is filled.", translation = "gankura.config.interfaceSettings.highlightBazaarOrders.desc")
        public static boolean highlightBazaarOrders = true;

        @ConfigEntry(id = "highlightOwnAuctions", translation = "gankura.config.interfaceSettings.highlightOwnAuctions")
        @Comment(value = "Marks your own auctions. Green when sold, red when expired.", translation = "gankura.config.interfaceSettings.highlightOwnAuctions.desc")
        public static boolean highlightOwnAuctions = true;

        @ConfigEntry(id = "highlightUndercutAuctions", translation = "gankura.config.interfaceSettings.highlightUndercutAuctions")
        @Comment(value = "Marks an unsold auction yellow while the same item is listed cheaper than yours.", translation = "gankura.config.interfaceSettings.highlightUndercutAuctions.desc")
        public static boolean highlightUndercutAuctions = true;

        @ConfigEntry(id = "enableAttributeMenuTweaks", translation = "gankura.config.interfaceSettings.enableAttributeMenuTweaks")
        @Comment(value = "Turns on the Attribute menu tweaks below.", translation = "gankura.config.interfaceSettings.enableAttributeMenuTweaks.desc")
        public static boolean enableAttributeMenuTweaks = false;

        @ConfigEntry(id = "enableAttributeTierNumbers", translation = "gankura.config.interfaceSettings.enableAttributeTierNumbers")
        @Comment(value = "Writes Attribute tiers as numbers, and marks undiscovered ones with 0.", translation = "gankura.config.interfaceSettings.enableAttributeTierNumbers.desc")
        public static boolean enableAttributeTierNumbers = true;

        @ConfigEntry(id = "highlightAttributeProgress", translation = "gankura.config.interfaceSettings.highlightAttributeProgress")
        @Comment(value = "Tints maxed attributes green and the rest red.", translation = "gankura.config.interfaceSettings.highlightAttributeProgress.desc")
        public static boolean highlightAttributeProgress = true;

        @ConfigEntry(id = "showAttributeTier", translation = "gankura.config.interfaceSettings.showAttributeTier")
        @Comment(value = "Writes the current tier in the corner of every attribute.", translation = "gankura.config.interfaceSettings.showAttributeTier.desc")
        public static boolean showAttributeTier = true;

        @ConfigEntry(id = "hideMaxedAttributeTier", translation = "gankura.config.interfaceSettings.hideMaxedAttributeTier")
        @Comment(value = "Leaves the tier off attributes that are already maxed.", translation = "gankura.config.interfaceSettings.hideMaxedAttributeTier.desc")
        public static boolean hideMaxedAttributeTier = true;

        @ConfigEntry(id = "showAttributeCosts", translation = "gankura.config.interfaceSettings.showAttributeCosts")
        @Comment(value = "Lists what the shards for the unfinished attributes cost, cheapest first, beside "
            + "the menu.", translation = "gankura.config.interfaceSettings.showAttributeCosts.desc")
        public static boolean showAttributeCosts = true;

        @ConfigEntry(id = "attributeCostTarget", translation = "gankura.config.interfaceSettings.attributeCostTarget")
        @Comment(value = "Whether to price the next tier or all the way to max. Clicking Next or Max in "
            + "the box switches it too.", translation = "gankura.config.interfaceSettings.attributeCostTarget.desc")
        public static AttributeCostTarget attributeCostTarget = AttributeCostTarget.NEXT_TIER;

        @ConfigEntry(id = "attributeCostSort", translation = "gankura.config.interfaceSettings.attributeCostSort")
        @Comment(value = "Which price the list is ordered by. Clicking either heading in the box switches "
            + "it too.", translation = "gankura.config.interfaceSettings.attributeCostSort.desc")
        public static AttributeCostSort attributeCostSort = AttributeCostSort.INSTANT;

        @ConfigEntry(id = "attributeCostRows", translation = "gankura.config.interfaceSettings.attributeCostRows")
        @Comment(value = "How many attributes the box lists.", translation = "gankura.config.interfaceSettings.attributeCostRows.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 3, max = 20)
        public static int attributeCostRows = 10;

        @ConfigEntry(id = "enableBestiaryMenuTweaks", translation = "gankura.config.interfaceSettings.enableBestiaryMenuTweaks")
        @Comment(value = "Turns on the Bestiary menu tweaks below.", translation = "gankura.config.interfaceSettings.enableBestiaryMenuTweaks.desc")
        public static boolean enableBestiaryMenuTweaks = false;

        @ConfigEntry(id = "enableBestiaryTierNumbers", translation = "gankura.config.interfaceSettings.enableBestiaryTierNumbers")
        @Comment(value = "Writes Bestiary tiers as numbers, and marks unlocked families with 0.", translation = "gankura.config.interfaceSettings.enableBestiaryTierNumbers.desc")
        public static boolean enableBestiaryTierNumbers = true;

        @ConfigEntry(id = "highlightBestiaryProgress", translation = "gankura.config.interfaceSettings.highlightBestiaryProgress")
        @Comment(value = "Tints finished entries green and unfinished ones red.", translation = "gankura.config.interfaceSettings.highlightBestiaryProgress.desc")
        public static boolean highlightBestiaryProgress = true;

        @ConfigEntry(id = "showBestiaryTier", translation = "gankura.config.interfaceSettings.showBestiaryTier")
        @Comment(value = "Writes the current tier in the corner of every entry.", translation = "gankura.config.interfaceSettings.showBestiaryTier.desc")
        public static boolean showBestiaryTier = true;

        @ConfigEntry(id = "hideMaxedBestiaryTier", translation = "gankura.config.interfaceSettings.hideMaxedBestiaryTier")
        @Comment(value = "Leaves the tier off entries that are already maxed.", translation = "gankura.config.interfaceSettings.hideMaxedBestiaryTier.desc")
        public static boolean hideMaxedBestiaryTier = true;

        @ConfigEntry(id = "enableHuntingBoxTweaks", translation = "gankura.config.interfaceSettings.enableHuntingBoxTweaks")
        @Comment(value = "Turns on the Hunting Box tweaks below.", translation = "gankura.config.interfaceSettings.enableHuntingBoxTweaks.desc")
        public static boolean enableHuntingBoxTweaks = false;

        @ConfigEntry(id = "showShardsOwned", translation = "gankura.config.interfaceSettings.showShardsOwned")
        @Comment(value = "Writes how many of each shard you hold in the corner of its slot.", translation = "gankura.config.interfaceSettings.showShardsOwned.desc")
        public static boolean showShardsOwned = true;

        @ConfigEntry(id = "showShardValues", translation = "gankura.config.interfaceSettings.showShardValues")
        @Comment(value = "Lists what the shards on this page are worth, with the page total. Click a name "
            + "to look it up on the Bazaar.", translation = "gankura.config.interfaceSettings.showShardValues.desc")
        public static boolean showShardValues = true;

        @ConfigEntry(id = "shardValueRows", translation = "gankura.config.interfaceSettings.shardValueRows")
        @Comment(value = "How many shards the box lists. The total still covers the whole page.", translation = "gankura.config.interfaceSettings.shardValueRows.desc")
        @ConfigOption.Slider
        @ConfigOption.Range(min = 3, max = 20)
        public static int shardValueRows = 10;

        @ConfigEntry(id = "shardValueSort", translation = "gankura.config.interfaceSettings.shardValueSort")
        @Comment(value = "Which price the list is ordered by. Clicking either heading in the box switches "
            + "it too.", translation = "gankura.config.interfaceSettings.shardValueSort.desc")
        public static AttributeCostSort shardValueSort = AttributeCostSort.INSTANT;

        @ConfigEntry(id = "enableHeartMenuTweaks", translation = "gankura.config.interfaceSettings.enableHeartMenuTweaks")
        @Comment(value = "Turns on the tweaks below for both menus.", translation = "gankura.config.interfaceSettings.enableHeartMenuTweaks.desc")
        public static boolean enableHeartMenuTweaks = false;

        @ConfigEntry(id = "highlightHeartStatus", translation = "gankura.config.interfaceSettings.highlightHeartStatus")
        @Comment(value = "Tints enabled perks green and disabled ones red. Perks you have not unlocked are "
            + "left alone.", translation = "gankura.config.interfaceSettings.highlightHeartStatus.desc")
        public static boolean highlightHeartStatus = true;

        @ConfigEntry(id = "showHeartLevel", translation = "gankura.config.interfaceSettings.showHeartLevel")
        @Comment(value = "Writes the current level in the corner of every perk that has one.", translation = "gankura.config.interfaceSettings.showHeartLevel.desc")
        public static boolean showHeartLevel = true;

        @ConfigEntry(id = "enableEnchantedBookSlots", translation = "gankura.config.interfaceSettings.enableEnchantedBookSlots")
        @Comment(value = "Turns on the enchanted book tweaks below.", translation = "gankura.config.interfaceSettings.enableEnchantedBookSlots.desc")
        public static boolean enableEnchantedBookSlots = false;

        @ConfigEntry(id = "showEnchantedBookName", translation = "gankura.config.interfaceSettings.showEnchantedBookName")
        @Comment(value = "Writes the enchantment's initials along the top of the slot.", translation = "gankura.config.interfaceSettings.showEnchantedBookName.desc")
        public static boolean showEnchantedBookName = true;

        @ConfigEntry(id = "showEnchantedBookTier", translation = "gankura.config.interfaceSettings.showEnchantedBookTier")
        @Comment(value = "Writes the level in the corner of the slot.", translation = "gankura.config.interfaceSettings.showEnchantedBookTier.desc")
        public static boolean showEnchantedBookTier = true;

        @ConfigEntry(id = "enablePetTweaks", translation = "gankura.config.interfaceSettings.enablePetTweaks")
        @Comment(value = "Turns on the pet tweaks below.", translation = "gankura.config.interfaceSettings.enablePetTweaks.desc")
        public static boolean enablePetTweaks = false;

        @ConfigEntry(id = "highlightActivePet", translation = "gankura.config.interfaceSettings.highlightActivePet")
        @Comment(value = "Marks the pet you currently have out.", translation = "gankura.config.interfaceSettings.highlightActivePet.desc")
        public static boolean highlightActivePet = true;

        @ConfigEntry(id = "showPetLevel", translation = "gankura.config.interfaceSettings.showPetLevel")
        @Comment(value = "Writes the current level in the corner of every pet.", translation = "gankura.config.interfaceSettings.showPetLevel.desc")
        public static boolean showPetLevel = true;

        @ConfigEntry(id = "hideMaxedPetLevel", translation = "gankura.config.interfaceSettings.hideMaxedPetLevel")
        @Comment(value = "Leaves the level off pets that are already maxed.", translation = "gankura.config.interfaceSettings.hideMaxedPetLevel.desc")
        public static boolean hideMaxedPetLevel = false;

        @ConfigEntry(id = "enablePersonalCompactorPreview", translation = "gankura.config.interfaceSettings.enablePersonalCompactorPreview")
        @Comment(value = "Shows the items a Personal Compactor or Personal Deletor is set to, under its name.", translation = "gankura.config.interfaceSettings.enablePersonalCompactorPreview.desc")
        public static boolean enablePersonalCompactorPreview = false;

        @ConfigEntry(id = "showPersonalCompactorStatus", translation = "gankura.config.interfaceSettings.showPersonalCompactorStatus")
        @Comment(value = "Writes whether it is turned on above the items.", translation = "gankura.config.interfaceSettings.showPersonalCompactorStatus.desc")
        public static boolean showPersonalCompactorStatus = true;

        @ConfigEntry(id = "enableCursorRestoreOnRapidReopen", translation = "gankura.config.interfaceSettings.enableCursorRestoreOnRapidReopen")
        @Comment(value = "Prevents cursor reset on quick swap.", translation = "gankura.config.interfaceSettings.enableCursorRestoreOnRapidReopen.desc")
        public static boolean enableCursorRestoreOnRapidReopen = false;
    }

    @Category(value = "chatFilter")
    @ConfigInfo(title = "Chat Filter", description = "Repeated messages to drop from chat.")
    public static final class ChatFilter {

        @ConfigEntry(id = "hideProfileMessage", translation = "gankura.config.chatFilter.hideProfileMessage")
        @Comment(value = "Hides \"You are playing on profile: ...\".", translation = "gankura.config.chatFilter.hideProfileMessage.desc")
        public static boolean hideProfileMessage = false;

        @ConfigEntry(id = "hideProfileIdMessage", translation = "gankura.config.chatFilter.hideProfileIdMessage")
        @Comment(value = "Hides \"Profile ID: ...\".", translation = "gankura.config.chatFilter.hideProfileIdMessage.desc")
        public static boolean hideProfileIdMessage = false;

        @ConfigEntry(id = "hideStashMessage", translation = "gankura.config.chatFilter.hideStashMessage")
        @Comment(value = "Hides the stash reminder sent every minute.", translation = "gankura.config.chatFilter.hideStashMessage.desc")
        public static boolean hideStashMessage = false;

        @ConfigEntry(id = "hideBlankMessages", translation = "gankura.config.chatFilter.hideBlankMessages")
        @Comment(value = "Hides chat lines that have no text.", translation = "gankura.config.chatFilter.hideBlankMessages.desc")
        public static boolean hideBlankMessages = false;
    }

    @Category(value = "keybinds")
    @ConfigInfo(title = "Keybinds", description = "Keys that open menus and switch sets.")
    public static final class Keybinds {

        @ConfigEntry(id = "enableOpenMenuKeybind", translation = "gankura.config.keybinds.enableOpenMenuKeybind")
        @Comment(value = "Opens menus via configured keys.", translation = "gankura.config.keybinds.enableOpenMenuKeybind.desc")
        public static boolean enableOpenMenuKeybind = false;

        @ConfigEntry(id = "openLoadoutsKeybind", translation = "gankura.config.keybinds.openLoadoutsKeybind")
        @Comment(value = "Sets key for /loadouts.", translation = "gankura.config.keybinds.openLoadoutsKeybind.desc")
        @ConfigOption.Keybind
        public static int openLoadoutsKeybind = KEY_NONE;

        @ConfigEntry(id = "openWardrobeKeybind", translation = "gankura.config.keybinds.openWardrobeKeybind")
        @Comment(value = "Sets key for /wardrobe.", translation = "gankura.config.keybinds.openWardrobeKeybind.desc")
        @ConfigOption.Keybind
        public static int openWardrobeKeybind = KEY_NONE;

        @ConfigEntry(id = "openEquipmentKeybind", translation = "gankura.config.keybinds.openEquipmentKeybind")
        @Comment(value = "Sets key for /equipment.", translation = "gankura.config.keybinds.openEquipmentKeybind.desc")
        @ConfigOption.Keybind
        public static int openEquipmentKeybind = KEY_NONE;

        @ConfigEntry(id = "enableLoadoutsKeybind", translation = "gankura.config.keybinds.enableLoadoutsKeybind")
        @Comment(value = "Switches Loadouts via configured keys.", translation = "gankura.config.keybinds.enableLoadoutsKeybind.desc")
        public static boolean enableLoadoutsKeybind = false;

        @ConfigEntry(id = "loadoutsKeybindSlot1", translation = "gankura.config.keybinds.loadoutsKeybindSlot1")
        @Comment(value = "Sets key for slot 1.", translation = "gankura.config.keybinds.loadoutsKeybindSlot1.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot1 = GLFW.GLFW_KEY_1;

        @ConfigEntry(id = "loadoutsKeybindSlot2", translation = "gankura.config.keybinds.loadoutsKeybindSlot2")
        @Comment(value = "Sets key for slot 2.", translation = "gankura.config.keybinds.loadoutsKeybindSlot2.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot2 = GLFW.GLFW_KEY_2;

        @ConfigEntry(id = "loadoutsKeybindSlot3", translation = "gankura.config.keybinds.loadoutsKeybindSlot3")
        @Comment(value = "Sets key for slot 3.", translation = "gankura.config.keybinds.loadoutsKeybindSlot3.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot3 = GLFW.GLFW_KEY_3;

        @ConfigEntry(id = "loadoutsKeybindSlot4", translation = "gankura.config.keybinds.loadoutsKeybindSlot4")
        @Comment(value = "Sets key for slot 4.", translation = "gankura.config.keybinds.loadoutsKeybindSlot4.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot4 = GLFW.GLFW_KEY_4;

        @ConfigEntry(id = "loadoutsKeybindSlot5", translation = "gankura.config.keybinds.loadoutsKeybindSlot5")
        @Comment(value = "Sets key for slot 5.", translation = "gankura.config.keybinds.loadoutsKeybindSlot5.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot5 = GLFW.GLFW_KEY_5;

        @ConfigEntry(id = "loadoutsKeybindSlot6", translation = "gankura.config.keybinds.loadoutsKeybindSlot6")
        @Comment(value = "Sets key for slot 6.", translation = "gankura.config.keybinds.loadoutsKeybindSlot6.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot6 = GLFW.GLFW_KEY_6;

        @ConfigEntry(id = "loadoutsKeybindSlot7", translation = "gankura.config.keybinds.loadoutsKeybindSlot7")
        @Comment(value = "Sets key for slot 7.", translation = "gankura.config.keybinds.loadoutsKeybindSlot7.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot7 = GLFW.GLFW_KEY_7;

        @ConfigEntry(id = "loadoutsKeybindSlot8", translation = "gankura.config.keybinds.loadoutsKeybindSlot8")
        @Comment(value = "Sets key for slot 8.", translation = "gankura.config.keybinds.loadoutsKeybindSlot8.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot8 = GLFW.GLFW_KEY_8;

        @ConfigEntry(id = "loadoutsKeybindSlot9", translation = "gankura.config.keybinds.loadoutsKeybindSlot9")
        @Comment(value = "Sets key for slot 9.", translation = "gankura.config.keybinds.loadoutsKeybindSlot9.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot9 = GLFW.GLFW_KEY_9;

        @ConfigEntry(id = "loadoutsKeybindSlot10", translation = "gankura.config.keybinds.loadoutsKeybindSlot10")
        @Comment(value = "Sets key for slot 10.", translation = "gankura.config.keybinds.loadoutsKeybindSlot10.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot10 = GLFW.GLFW_KEY_0;

        @ConfigEntry(id = "loadoutsKeybindSlot11", translation = "gankura.config.keybinds.loadoutsKeybindSlot11")
        @Comment(value = "Sets key for slot 11.", translation = "gankura.config.keybinds.loadoutsKeybindSlot11.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot11 = GLFW.GLFW_KEY_MINUS;

        @ConfigEntry(id = "loadoutsKeybindSlot12", translation = "gankura.config.keybinds.loadoutsKeybindSlot12")
        @Comment(value = "Sets key for slot 12.", translation = "gankura.config.keybinds.loadoutsKeybindSlot12.desc")
        @ConfigOption.Keybind
        public static int loadoutsKeybindSlot12 = GLFW.GLFW_KEY_EQUAL;

        @ConfigEntry(id = "enableArmorSetKeybind", translation = "gankura.config.keybinds.enableArmorSetKeybind")
        @Comment(value = "Switches Armor Sets via configured keys.", translation = "gankura.config.keybinds.enableArmorSetKeybind.desc")
        public static boolean enableArmorSetKeybind = false;

        @ConfigEntry(id = "armorSetKeybindSlot1", translation = "gankura.config.keybinds.armorSetKeybindSlot1")
        @Comment(value = "Sets key for slot 1.", translation = "gankura.config.keybinds.armorSetKeybindSlot1.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot1 = GLFW.GLFW_KEY_1;

        @ConfigEntry(id = "armorSetKeybindSlot2", translation = "gankura.config.keybinds.armorSetKeybindSlot2")
        @Comment(value = "Sets key for slot 2.", translation = "gankura.config.keybinds.armorSetKeybindSlot2.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot2 = GLFW.GLFW_KEY_2;

        @ConfigEntry(id = "armorSetKeybindSlot3", translation = "gankura.config.keybinds.armorSetKeybindSlot3")
        @Comment(value = "Sets key for slot 3.", translation = "gankura.config.keybinds.armorSetKeybindSlot3.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot3 = GLFW.GLFW_KEY_3;

        @ConfigEntry(id = "armorSetKeybindSlot4", translation = "gankura.config.keybinds.armorSetKeybindSlot4")
        @Comment(value = "Sets key for slot 4.", translation = "gankura.config.keybinds.armorSetKeybindSlot4.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot4 = GLFW.GLFW_KEY_4;

        @ConfigEntry(id = "armorSetKeybindSlot5", translation = "gankura.config.keybinds.armorSetKeybindSlot5")
        @Comment(value = "Sets key for slot 5.", translation = "gankura.config.keybinds.armorSetKeybindSlot5.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot5 = GLFW.GLFW_KEY_5;

        @ConfigEntry(id = "armorSetKeybindSlot6", translation = "gankura.config.keybinds.armorSetKeybindSlot6")
        @Comment(value = "Sets key for slot 6.", translation = "gankura.config.keybinds.armorSetKeybindSlot6.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot6 = GLFW.GLFW_KEY_6;

        @ConfigEntry(id = "armorSetKeybindSlot7", translation = "gankura.config.keybinds.armorSetKeybindSlot7")
        @Comment(value = "Sets key for slot 7.", translation = "gankura.config.keybinds.armorSetKeybindSlot7.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot7 = GLFW.GLFW_KEY_7;

        @ConfigEntry(id = "armorSetKeybindSlot8", translation = "gankura.config.keybinds.armorSetKeybindSlot8")
        @Comment(value = "Sets key for slot 8.", translation = "gankura.config.keybinds.armorSetKeybindSlot8.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot8 = GLFW.GLFW_KEY_8;

        @ConfigEntry(id = "armorSetKeybindSlot9", translation = "gankura.config.keybinds.armorSetKeybindSlot9")
        @Comment(value = "Sets key for slot 9.", translation = "gankura.config.keybinds.armorSetKeybindSlot9.desc")
        @ConfigOption.Keybind
        public static int armorSetKeybindSlot9 = GLFW.GLFW_KEY_9;

        @ConfigEntry(id = "enableEquipmentSetKeybind", translation = "gankura.config.keybinds.enableEquipmentSetKeybind")
        @Comment(value = "Switches Equipment Sets via configured keys.", translation = "gankura.config.keybinds.enableEquipmentSetKeybind.desc")
        public static boolean enableEquipmentSetKeybind = false;

        @ConfigEntry(id = "equipmentSetKeybindSlot1", translation = "gankura.config.keybinds.equipmentSetKeybindSlot1")
        @Comment(value = "Sets key for slot 1.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot1.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot1 = GLFW.GLFW_KEY_1;

        @ConfigEntry(id = "equipmentSetKeybindSlot2", translation = "gankura.config.keybinds.equipmentSetKeybindSlot2")
        @Comment(value = "Sets key for slot 2.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot2.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot2 = GLFW.GLFW_KEY_2;

        @ConfigEntry(id = "equipmentSetKeybindSlot3", translation = "gankura.config.keybinds.equipmentSetKeybindSlot3")
        @Comment(value = "Sets key for slot 3.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot3.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot3 = GLFW.GLFW_KEY_3;

        @ConfigEntry(id = "equipmentSetKeybindSlot4", translation = "gankura.config.keybinds.equipmentSetKeybindSlot4")
        @Comment(value = "Sets key for slot 4.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot4.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot4 = GLFW.GLFW_KEY_4;

        @ConfigEntry(id = "equipmentSetKeybindSlot5", translation = "gankura.config.keybinds.equipmentSetKeybindSlot5")
        @Comment(value = "Sets key for slot 5.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot5.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot5 = GLFW.GLFW_KEY_5;

        @ConfigEntry(id = "equipmentSetKeybindSlot6", translation = "gankura.config.keybinds.equipmentSetKeybindSlot6")
        @Comment(value = "Sets key for slot 6.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot6.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot6 = GLFW.GLFW_KEY_6;

        @ConfigEntry(id = "equipmentSetKeybindSlot7", translation = "gankura.config.keybinds.equipmentSetKeybindSlot7")
        @Comment(value = "Sets key for slot 7.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot7.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot7 = GLFW.GLFW_KEY_7;

        @ConfigEntry(id = "equipmentSetKeybindSlot8", translation = "gankura.config.keybinds.equipmentSetKeybindSlot8")
        @Comment(value = "Sets key for slot 8.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot8.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot8 = GLFW.GLFW_KEY_8;

        @ConfigEntry(id = "equipmentSetKeybindSlot9", translation = "gankura.config.keybinds.equipmentSetKeybindSlot9")
        @Comment(value = "Sets key for slot 9.", translation = "gankura.config.keybinds.equipmentSetKeybindSlot9.desc")
        @ConfigOption.Keybind
        public static int equipmentSetKeybindSlot9 = GLFW.GLFW_KEY_9;
    }

    @Category(value = "misc")
    @ConfigInfo(title = "Misc", description = "Miscellaneous features.")
    public static final class Misc {

        @ConfigEntry(id = "showEtherwarpTarget", translation = "gankura.config.misc.showEtherwarpTarget")
        @Comment(value = "Outlines the block Etherwarp would take you on top of.", translation = "gankura.config.misc.showEtherwarpTarget.desc")
        public static boolean showEtherwarpTarget = false;

        @ConfigEntry(id = "etherwarpOnlySneaking", translation = "gankura.config.misc.etherwarpOnlySneaking")
        @Comment(value = "Only draws it while you are sneaking.", translation = "gankura.config.misc.etherwarpOnlySneaking.desc")
        public static boolean etherwarpOnlySneaking = true;

        @ConfigEntry(id = "showEtherwarpReason", translation = "gankura.config.misc.showEtherwarpReason")
        @Comment(value = "Names why a warp would be refused.", translation = "gankura.config.misc.showEtherwarpReason.desc")
        public static boolean showEtherwarpReason = true;

        @ConfigEntry(id = "enableRebootAlert", translation = "gankura.config.misc.enableRebootAlert")
        @Comment(value = "Warns of lobby restart.", translation = "gankura.config.misc.enableRebootAlert.desc")
        public static boolean enableRebootAlert = false;

        @ConfigEntry(id = "enableWarpQueue", translation = "gankura.config.misc.enableWarpQueue")
        @Comment(value = "Shows cooldown, queues /warp.", translation = "gankura.config.misc.enableWarpQueue.desc")
        public static boolean enableWarpQueue = false;

        @ConfigEntry(id = "keepAbilityMenuOpen", translation = "gankura.config.misc.keepAbilityMenuOpen")
        @Comment(value = "Stops an item swap from closing a menu an ability just opened.", translation = "gankura.config.misc.keepAbilityMenuOpen.desc")
        public static boolean keepAbilityMenuOpen = false;

        @ConfigEntry(id = "hideBlockBreakParticles", translation = "gankura.config.misc.hideBlockBreakParticles")
        @Comment(value = "Hides the fragments thrown out when a block is broken.", translation = "gankura.config.misc.hideBlockBreakParticles.desc")
        public static boolean hideBlockBreakParticles = false;

        @ConfigEntry(id = "ignoreArmorStandClicks", translation = "gankura.config.misc.ignoreArmorStandClicks")
        @Comment(value = "Lets rods and abilities fire with a hologram in the way.", translation = "gankura.config.misc.ignoreArmorStandClicks.desc")
        public static boolean ignoreArmorStandClicks = false;
    }
}
