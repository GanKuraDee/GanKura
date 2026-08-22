package com.deeply.gankura.data;

import java.util.List;

/**
 * Highlight / Tracer / Nameplate の対象モブ。
 * 対象はエリアごとのリスト1つで、3つの機能で共通に使う。
 * 実際に表示するかどうかは、機能ごとの全体トグルで切り替える。
 *
 * ドラッグリストの選択候補はリストの総称型(= enum の種類)から決まるため、
 * エリアごとに別の enum として定義している。共通の扱いができるよう、
 * 判定用のメソッドはこのインターフェースにまとめている。
 *
 * ドラッグリストは toString() を表示に使うため、ラベルには色コードをそのまま入れている。
 * 色は Glowing の輪郭(RGB)と Tracer の線(ARGB)で別々に持つ。ARGB 側は不透明固定。
 */
public interface MobVisual {

    String label();

    int glowColorRGB();

    /** このモブが属するエリアの対象リスト。エリアごとに別なので実装側で指定する */
    List<? extends MobVisual> targets();

    /** 色コードを除いた表示名。ネームプレートのように色を別途付ける場合に使う */
    default String plainLabel() {
        return label().replaceAll("§.", "");
    }

    default int tracerColorARGB() {
        return 0xFF000000 | glowColorRGB();
    }

    /**
     * そのモブが出現するエリアに今いるか。
     *
     * 対象リストは4エリアぶんをまとめて持てるので、全エリアの対象を選んでいると
     * 別のエリアで同じ型のモブを拾ってしまうことがある。
     * enum の種類がそのままエリアを表すので、ここで絞り込む。
     */
    private static boolean inOwnArea(MobVisual target) {
        if (target instanceof TheEnd) return GameState.Server.isTheEnd();
        if (target instanceof SpidersDen) return GameState.Server.isSpidersDen();
        if (target instanceof CrimsonIsle) return GameState.Server.isCrimsonIsle();
        if (target instanceof CrystalHollows) return GameState.Server.isCrystalHollows();
        if (target instanceof MoongladeMarsh) return GameState.Server.isMoongladeMarsh();
        if (target instanceof TorrhusCanyon) {
            return GameState.Server.isTorrhusCanyon() || GameState.Server.isTorrhusHeights();
        }
        // 残りは Critter Safari の4バイオーム。バイオームの区別は座標で行う
        return GameState.Server.isSafari();
    }

    /**
     * キャプチャ済みとして表示から外すか。
     * Critter Safari は1種につき1体捕まえれば済むので、終わった種は消せるようにしている。
     * 対象は Critter Safari の4バイオームのモブだけで、他エリアのモブには効かない
     */
    private static boolean hiddenAsCaptured(MobVisual target) {
        if (!ModConfig.INSTANCE.mobVisuals.hideCapturedCritters) return false;
        if (target instanceof TheEnd || target instanceof SpidersDen
                || target instanceof CrimsonIsle || target instanceof CrystalHollows
                || target instanceof MoongladeMarsh || target instanceof TorrhusCanyon) {
            return false;
        }
        return GameState.CritterSafari.isCaptured(target.plainLabel());
    }

    // 対象リストに載っていて、今いるエリアのモブで、かつその機能の全体トグルが入っていれば表示する
    default boolean highlight() {
        return ModConfig.INSTANCE.mobVisuals.enableHighlight && shown(this);
    }

    default boolean tracer() {
        return ModConfig.INSTANCE.mobVisuals.enableTracer && shown(this);
    }

    default boolean nameplate() {
        return ModConfig.INSTANCE.mobVisuals.enableNameplate && shown(this);
    }

    // 機能の全体トグル以外の、3機能に共通する表示条件
    private static boolean shown(MobVisual target) {
        return inOwnArea(target) && target.targets().contains(target) && !hiddenAsCaptured(target);
    }

    /** どれか1つでも有効なら、そのモブを探す必要がある */
    default boolean anyEnabled() {
        return highlight() || tracer() || nameplate();
    }

    /** The End のモブ */
    enum TheEnd implements MobVisual {
        GOLEM("§6End Stone Protector", 0xFFAA00),
        DRAGON("§dDragon", 0xFF55FF);

        private final String label;
        private final int glowColorRGB;

        TheEnd(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<TheEnd> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsTheEnd;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Spider's Den のモブ */
    enum SpidersDen implements MobVisual {
        BROODMOTHER("§cBroodmother", 0xFF5555),
        ARACHNE("§5Arachne", 0xAA00AA),
        ARACHNE_BROOD("§dArachne's Brood", 0xFF55FF);

        private final String label;
        private final int glowColorRGB;

        SpidersDen(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<SpidersDen> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsSpidersDen;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Crimson Isle のモブ */
    enum CrimsonIsle implements MobVisual {
        BARBARIAN_DUKE_X("§cBarbarian Duke X", 0xFF5555),
        BLADESOUL("§8Bladesoul", 0x555555),
        MAGE_OUTLAW("§5Mage Outlaw", 0xAA00AA),
        ASHFANG("§7Ashfang", 0xAAAAAA),
        ASHFANG_FOLLOWER("§8Ashfang Follower", 0x555555),
        ASHFANG_ACOLYTE("§9Ashfang Acolyte", 0x5555FF),
        ASHFANG_UNDERLING("§cAshfang Underling", 0xFF5555),
        MAGMA_BOSS("§6Magma Boss", 0xFFAA00),
        MAGMA_GLARE("§cMagma Glare", 0xFF5555),
        MATCHO("§bMatcho", 0x55FFFF);

        private final String label;
        private final int glowColorRGB;

        CrimsonIsle(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<CrimsonIsle> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsCrimsonIsle;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Crystal Hollows のモブ */
    enum CrystalHollows implements MobVisual {
        BOSS_CORLEONE("§dBoss Corleone", 0xFF55FF);

        private final String label;
        private final int glowColorRGB;

        CrystalHollows(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<CrystalHollows> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsCrystalHollows;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Critter Safari: Cavern のモブ */
    enum SafariCavern implements MobVisual {
        CAVERNFISH("§bCavernfish", 0x55FFFF),
        FLITTER("§eFlitter", 0xFFFF55),
        SHYWORM("§aShyworm", 0x55FF55),
        DRIFTLING("§9Driftling", 0x5555FF),
        CHUCKWALLA("§6Chuckwalla", 0xFFAA00),
        ROCKMITE("§7Rockmite", 0xAAAAAA),
        SCRAPPY("§cScrappy", 0xFF5555),
        SNOOZLE("§dSnoozle", 0xFF55FF),
        GEMZIE("§5Gemzie", 0xAA00AA);

        private final String label;
        private final int glowColorRGB;

        SafariCavern(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<SafariCavern> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsSafariCavern;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Critter Safari: Forest のモブ */
    enum SafariForest implements MobVisual {
        FOXTROT("§cFoxtrot", 0xFF5555),
        BLUEBIRD("§9Bluebird", 0x5555FF),
        HONEYBUG("§eHoneybug", 0xFFFF55),
        TREEFROG("§aTreefrog", 0x55FF55),
        WOODCHUCKER("§8Woodchucker", 0x555555),
        FLUFFLING("§fFluffling", 0xFFFFFF),
        HIDEONFLOOR("§aHideonfloor", 0x55FF55),
        PARAKEET("§aParakeet", 0x55FF55),
        MACAW("§cMacaw", 0xFF5555);

        private final String label;
        private final int glowColorRGB;

        SafariForest(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<SafariForest> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsSafariForest;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Critter Safari: Haunted のモブ */
    enum SafariHaunted implements MobVisual {
        AREITA("§5Areita", 0xAA00AA),
        BLOODBAT("§cBloodbat", 0xFF5555),
        DUPLICO("§dDuplico", 0xFF55FF),
        GAZER("§bGazer", 0x55FFFF),
        LITTERBUG("§8Litterbug", 0x555555),
        SOLSNATCHER("§eSolsnatcher", 0xFFFF55),
        GIMMIEGOLD("§6Gimmiegold", 0xFFAA00),
        HIDEONWALL("§5Hideonwall", 0xAA00AA),
        HIDEYHO("§fHideyho", 0xFFFFFF),
        DOOMSPIRAL("§5Doomspiral", 0xAA00AA);

        private final String label;
        private final int glowColorRGB;

        SafariHaunted(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<SafariHaunted> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsSafariHaunted;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Critter Safari: Icy のモブ */
    enum SafariIcy implements MobVisual {
        BILLYGOAT("§fBillygoat", 0xFFFFFF),
        MANTIS_SHRIMP("§cMantis Shrimp", 0xFF5555),
        NOZZLENOSE("§bNozzlenose", 0x55FFFF),
        POLARIS("§fPolaris", 0xFFFFFF),
        SHUDDERSQUID("§9Shuddersquid", 0x5555FF),
        STRONGARM("§fStrongarm", 0xFFFFFF),
        TEPID("§6Tepid", 0xFFAA00),
        TROODON("§aTroodon", 0x55FF55),
        WUMPA("§bWumpa", 0x55FFFF);

        private final String label;
        private final int glowColorRGB;

        SafariIcy(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<SafariIcy> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsSafariIcy;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Moonglade Marsh のモブ */
    enum MoongladeMarsh implements MobVisual {
        HIDEONLEAF("§aHideonleaf", 0x55FF55),
        CORALOT("§dCoralot", 0xFF55FF),
        MOSSYBIT("§aMossybit", 0x55FF55),
        COD("§7Cod", 0xAAAAAA),
        SALMON("§cSalmon", 0xFF5555),
        JOYDIVE("§bJoydive", 0x55FFFF),
        LUMISQUID("§9Lumisquid", 0x5555FF),
        SHELLWISE("§aShellwise", 0x55FF55),
        SPIKE("§eSpike", 0xFFFF55),
        BIRRIES("§8Birries", 0x555555),
        BAMBULEAF("§fBambuleaf", 0xFFFFFF),
        MOCHIBEAR("§6Mochibear", 0xFFAA00),
        PHANPYRE("§5Phanpyre", 0xAA00AA),
        PHANFLARE("§6Phanflare", 0xFFAA00),
        DREADWING("§cDreadwing", 0xFF5555),
        INVISIBUG("§dInvisibug", 0xFF55FF),
        AZURE("§bAzure", 0x55FFFF),
        VERDANT("§aVerdant", 0x55FF55),
        HEWVER("§6Hewver", 0xFFAA00),
        HONEYHOG("§eHoneyhog", 0xFFFF55),
        HONEYMITE("§5Honeymite", 0xAA00AA),
        MURKBAT("§8Murkbat", 0x555555),
        TIDETOT("§9Tidetot", 0x5555FF),
        CHILL("§bChill", 0x55FFFF),
        STAG_BEETLE("§7Stag Beetle", 0xAAAAAA),
        WOODLOUSE("§aWoodlouse", 0x55FF55);

        private final String label;
        private final int glowColorRGB;

        MoongladeMarsh(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<MoongladeMarsh> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsMoongladeMarsh;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Torrhus Canyon のモブ */
    enum TorrhusCanyon implements MobVisual {
        HIDEONSUN("§eHideonsun", 0xFFFF55),
        BLUE_JAY("§9Blue Jay", 0x5555FF),
        DUSTYBIT("§7Dustybit", 0xAAAAAA),
        SEPIALOT("§6Sepialot", 0xFFAA00),
        GOLDOLOT("§eGoldolot", 0xFFFF55),
        PANGOLIN("§bPangolin", 0x55FFFF),
        BUNBUN("§fBunbun", 0xFFFFFF),
        DRYBARK("§8Drybark", 0x555555),
        FIREFOX("§cFirefox", 0xFF5555),
        GROUNDHOG("§dGroundhog", 0xFF55FF),
        HIVETHIEF("§bHivethief", 0x55FFFF),
        MOUNTAIN_GOAT("§7Mountain Goat", 0xAAAAAA),
        PUCK("§5Puck", 0xAA00AA),
        BEEHEEMOTH("§6Beeheemoth", 0xFFAA00),
        HONEYBUZZ("§eHoneybuzz", 0xFFFF55),
        POLLENDART("§aPollendart", 0x55FF55),
        EMBER("§6Ember", 0xFFAA00),
        SOLAR("§eSolar", 0xFFFF55),
        TIMIL("§dTimil", 0xFF55FF),
        WATER_SNAKE("§9Water Snake", 0x5555FF),
        PARCHED("§6Parched", 0xFFAA00),
        GRIZZLY_BEAR("§6Grizzly Bear", 0xFFAA00),
        QUEEN_ANT("§5Queen Ant", 0xAA00AA),
        ANT("§cAnt", 0xFF5555),
        SNEAKY_TIKI("§aSneaky Tiki", 0x55FF55),
        SHRIEKY_TIKI("§dShrieky Tiki", 0xFF55FF),
        CHEEKY_TIKI("§bCheeky Tiki", 0x55FFFF);

        private final String label;
        private final int glowColorRGB;

        TorrhusCanyon(String label, int glowColorRGB) {
            this.label = label;
            this.glowColorRGB = glowColorRGB;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public int glowColorRGB() {
            return glowColorRGB;
        }

        @Override
        public List<TorrhusCanyon> targets() {
            return ModConfig.INSTANCE.mobVisuals.targetsTorrhusCanyon;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
