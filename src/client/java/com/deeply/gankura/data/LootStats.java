package com.deeply.gankura.data;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Properties;

public class LootStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraLootStats");

    /** 種類ごとの DPS 集計 */
    public static final class DpsStats {
        public int fights = 0;
        public double totalDps = 0.0;
        public double maxDps = 0.0;

        public double averageDps() {
            return fights > 0 ? totalDps / fights : 0.0;
        }

        boolean record(double dps) {
            boolean newBest = dps > maxDps;
            fights++;
            totalDps += dps;
            if (newBest) maxDps = dps;
            return newBest;
        }
    }

    /** Dragon の DPS 記録結果。全体または種類別の最大 DPS を更新したかを返す */
    public record DragonDpsResult(boolean newOverallBest, boolean newTypeBest) {}

    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("loot_tracker.properties")
            .toFile();

    private static final Properties properties = new Properties();

    public static int epicGolemPets = 0;
    public static int legendaryGolemPets = 0;
    public static int tierBoostCores = 0;

    // End Stone Protector の DPS 記録 (戦闘数・合計DPS・最大DPS)
    public static int golemFights = 0;
    public static double golemTotalDps = 0.0;
    public static double golemMaxDps = 0.0;

    // ★追加: ドラゴンペットの取得数
    public static int epicDragonPets = 0;
    public static int legendaryDragonPets = 0;

    // Dragon の DPS 記録 (全体 + 種類別)
    public static int dragonFights = 0;
    public static double dragonTotalDps = 0.0;
    public static double dragonMaxDps = 0.0;
    private static final EnumMap<DragonAlertType, DpsStats> DRAGON_TYPE_DPS = new EnumMap<>(DragonAlertType.class);

    static {
        for (DragonAlertType type : DragonAlertType.values()) {
            DRAGON_TYPE_DPS.put(type, new DpsStats());
        }
    }

    // Crimson Isle ドロップ
    public static int kuudraKeys = 0;
    public static int hotKuudraKeys = 0;
    public static int magmaUrchins = 0;
    public static int ragnarockAxes = 0;
    public static int fireVeilWands = 0;
    public static int fireFreezeStaffs = 0;
    public static int wandsOfStrength = 0;
    public static int flamingFists = 0;
    public static int fireFuryStaffs = 0;
    public static int epicMagmaCubePets = 0;
    public static int legendaryMagmaCubePets = 0;

    static {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // ファイルがない場合は作成へ
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            // 数値変換エラー対策を追加
            try { epicGolemPets = Integer.parseInt(properties.getProperty("epicGolemPets", "0")); } catch (NumberFormatException e) { epicGolemPets = 0; }
            try { legendaryGolemPets = Integer.parseInt(properties.getProperty("legendaryGolemPets", "0")); } catch (NumberFormatException e) { legendaryGolemPets = 0; }
            try { tierBoostCores = Integer.parseInt(properties.getProperty("tierBoostCores", "0")); } catch (NumberFormatException e) { tierBoostCores = 0; }

            golemFights = parseNonNegativeInt(properties.getProperty("golemFights"));
            golemTotalDps = parseNonNegativeDouble(properties.getProperty("golemTotalDps"));
            golemMaxDps = parseNonNegativeDouble(properties.getProperty("golemMaxDps"));

            // ★追加: ドラゴンペットの読み込み
            try { epicDragonPets = Integer.parseInt(properties.getProperty("epicDragonPets", "0")); } catch (NumberFormatException e) { epicDragonPets = 0; }
            try { legendaryDragonPets = Integer.parseInt(properties.getProperty("legendaryDragonPets", "0")); } catch (NumberFormatException e) { legendaryDragonPets = 0; }

            dragonFights = parseNonNegativeInt(properties.getProperty("dragonFights"));
            dragonTotalDps = parseNonNegativeDouble(properties.getProperty("dragonTotalDps"));
            dragonMaxDps = parseNonNegativeDouble(properties.getProperty("dragonMaxDps"));
            for (DragonAlertType type : DragonAlertType.values()) {
                String prefix = dragonTypeKeyPrefix(type);
                DpsStats stats = DRAGON_TYPE_DPS.get(type);
                stats.fights = parseNonNegativeInt(properties.getProperty(prefix + "fights"));
                stats.totalDps = parseNonNegativeDouble(properties.getProperty(prefix + "totalDps"));
                stats.maxDps = parseNonNegativeDouble(properties.getProperty(prefix + "maxDps"));
            }
            // Crimson Isle
            try { kuudraKeys = Integer.parseInt(properties.getProperty("kuudraKeys", "0")); } catch (NumberFormatException e) { kuudraKeys = 0; }
            try { hotKuudraKeys = Integer.parseInt(properties.getProperty("hotKuudraKeys", "0")); } catch (NumberFormatException e) { hotKuudraKeys = 0; }
            try { magmaUrchins = Integer.parseInt(properties.getProperty("magmaUrchins", "0")); } catch (NumberFormatException e) { magmaUrchins = 0; }
            try { ragnarockAxes = Integer.parseInt(properties.getProperty("ragnarockAxes", "0")); } catch (NumberFormatException e) { ragnarockAxes = 0; }
            try { fireVeilWands = Integer.parseInt(properties.getProperty("fireVeilWands", "0")); } catch (NumberFormatException e) { fireVeilWands = 0; }
            try { fireFreezeStaffs = Integer.parseInt(properties.getProperty("fireFreezeStaffs", "0")); } catch (NumberFormatException e) { fireFreezeStaffs = 0; }
            try { wandsOfStrength = Integer.parseInt(properties.getProperty("wandsOfStrength", "0")); } catch (NumberFormatException e) { wandsOfStrength = 0; }
            try { flamingFists = Integer.parseInt(properties.getProperty("flamingFists", "0")); } catch (NumberFormatException e) { flamingFists = 0; }
            try { fireFuryStaffs = Integer.parseInt(properties.getProperty("fireFuryStaffs", "0")); } catch (NumberFormatException e) { fireFuryStaffs = 0; }
            try { epicMagmaCubePets = Integer.parseInt(properties.getProperty("epicMagmaCubePets", "0")); } catch (NumberFormatException e) { epicMagmaCubePets = 0; }
            try { legendaryMagmaCubePets = Integer.parseInt(properties.getProperty("legendaryMagmaCubePets", "0")); } catch (NumberFormatException e) { legendaryMagmaCubePets = 0; }
        } catch (IOException e) {
            LOGGER.error("Failed to load loot stats", e);
        }
    }

    public static void save() {
        // 親フォルダ (config/gankura) が存在しない場合は作成する
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOGGER.error("Failed to create config directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        properties.setProperty("epicGolemPets", String.valueOf(epicGolemPets));
        properties.setProperty("legendaryGolemPets", String.valueOf(legendaryGolemPets));
        properties.setProperty("tierBoostCores", String.valueOf(tierBoostCores));
        properties.setProperty("golemFights", String.valueOf(golemFights));
        properties.setProperty("golemTotalDps", String.valueOf(golemTotalDps));
        properties.setProperty("golemMaxDps", String.valueOf(golemMaxDps));

        // ★追加: ドラゴンペットの保存
        properties.setProperty("epicDragonPets", String.valueOf(epicDragonPets));
        properties.setProperty("legendaryDragonPets", String.valueOf(legendaryDragonPets));
        properties.setProperty("dragonFights", String.valueOf(dragonFights));
        properties.setProperty("dragonTotalDps", String.valueOf(dragonTotalDps));
        properties.setProperty("dragonMaxDps", String.valueOf(dragonMaxDps));
        for (DragonAlertType type : DragonAlertType.values()) {
            String prefix = dragonTypeKeyPrefix(type);
            DpsStats stats = DRAGON_TYPE_DPS.get(type);
            properties.setProperty(prefix + "fights", String.valueOf(stats.fights));
            properties.setProperty(prefix + "totalDps", String.valueOf(stats.totalDps));
            properties.setProperty(prefix + "maxDps", String.valueOf(stats.maxDps));
        }
        // Crimson Isle
        properties.setProperty("kuudraKeys", String.valueOf(kuudraKeys));
        properties.setProperty("hotKuudraKeys", String.valueOf(hotKuudraKeys));
        properties.setProperty("magmaUrchins", String.valueOf(magmaUrchins));
        properties.setProperty("ragnarockAxes", String.valueOf(ragnarockAxes));
        properties.setProperty("fireVeilWands", String.valueOf(fireVeilWands));
        properties.setProperty("fireFreezeStaffs", String.valueOf(fireFreezeStaffs));
        properties.setProperty("wandsOfStrength", String.valueOf(wandsOfStrength));
        properties.setProperty("flamingFists", String.valueOf(flamingFists));
        properties.setProperty("fireFuryStaffs", String.valueOf(fireFuryStaffs));
        properties.setProperty("epicMagmaCubePets", String.valueOf(epicMagmaCubePets));
        properties.setProperty("legendaryMagmaCubePets", String.valueOf(legendaryMagmaCubePets));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura Golem and Dragon Loot Tracker");
        } catch (IOException e) {
            LOGGER.error("Failed to save loot stats", e);
        }
    }

    public static void addEpicGolemPet() {
        epicGolemPets++;
        save();
    }

    public static void addLegendaryGolemPet() {
        legendaryGolemPets++;
        save();
    }

    public static void addTierBoostCore() {
        tierBoostCores++;
        save();
    }

    // ★追加: ドラゴンペット追加用メソッド
    public static void addEpicDragonPet() {
        epicDragonPets++;
        save();
    }

    public static void addLegendaryDragonPet() {
        legendaryDragonPets++;
        save();
    }

    public static synchronized boolean recordGolemDps(double dps) {
        if (!Double.isFinite(dps) || dps <= 0.0) return false;
        boolean newBest = dps > golemMaxDps;
        golemFights++;
        golemTotalDps += dps;
        if (newBest) golemMaxDps = dps;
        save();
        return newBest;
    }

    public static synchronized DragonDpsResult recordDragonDps(DragonAlertType type, double dps) {
        if (!Double.isFinite(dps) || dps <= 0.0) return new DragonDpsResult(false, false);
        boolean newOverallBest = dps > dragonMaxDps;
        dragonFights++;
        dragonTotalDps += dps;
        if (newOverallBest) dragonMaxDps = dps;
        boolean newTypeBest = false;
        if (type != null) {
            DpsStats stats = DRAGON_TYPE_DPS.get(type);
            if (stats != null) newTypeBest = stats.record(dps);
        }
        save();
        return new DragonDpsResult(newOverallBest, newTypeBest);
    }

    public static DpsStats dragonTypeDps(DragonAlertType type) {
        return DRAGON_TYPE_DPS.get(type);
    }

    private static String dragonTypeKeyPrefix(DragonAlertType type) {
        return "dragonType." + type.typeName().toLowerCase(Locale.ROOT) + ".";
    }

    public static double golemAverageDps() {
        return golemFights > 0 ? golemTotalDps / golemFights : 0.0;
    }

    public static double dragonAverageDps() {
        return dragonFights > 0 ? dragonTotalDps / dragonFights : 0.0;
    }

    /** チャットと Tracker HUD で揃えた DPS 表記 */
    public static String formatDps(double dps) {
        return dps >= 1000.0
                ? String.format(Locale.ROOT, "%,.1fk", dps / 1000.0)
                : String.format(Locale.ROOT, "%,.1f", dps);
    }

    /** DPS 値に応じた色コード (Hypixel SkyBlock のレベル色階調に準拠) */
    public static String dpsColor(double dps) {
        if (!Double.isFinite(dps) || dps <= 0.0) return "§7";
        if (dps < 10_000.0) return "§f";
        if (dps < 25_000.0) return "§e";
        if (dps < 50_000.0) return "§a";
        if (dps < 100_000.0) return "§2";
        if (dps < 250_000.0) return "§b";
        if (dps < 500_000.0) return "§3";
        if (dps < 1_000_000.0) return "§9";
        if (dps < 2_500_000.0) return "§d";
        if (dps < 4_000_000.0) return "§5";
        if (dps < 6_000_000.0) return "§6";
        if (dps < 8_000_000.0) return "§c";
        return "§4";
    }

    private static int parseNonNegativeInt(String value) {
        if (value == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseNonNegativeDouble(String value) {
        if (value == null) return 0.0;
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0.0 ? parsed : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Crimson Isle 追加用メソッド
    public static void addKuudraKey()              { kuudraKeys++;              save(); }
    public static void addHotKuudraKey()           { hotKuudraKeys++;           save(); }
    public static void addMagmaUrchin()            { magmaUrchins++;            save(); }
    public static void addRagnarockAxe()           { ragnarockAxes++;           save(); }
    public static void addFireVeilWand()           { fireVeilWands++;           save(); }
    public static void addFireFreezeStaff()        { fireFreezeStaffs++;        save(); }
    public static void addWandOfStrength()         { wandsOfStrength++;         save(); }
    public static void addFlamingFist()            { flamingFists++;            save(); }
    public static void addFireFuryStaff()          { fireFuryStaffs++;          save(); }
    public static void addEpicMagmaCubePet()       { epicMagmaCubePets++;       save(); }
    public static void addLegendaryMagmaCubePet()  { legendaryMagmaCubePets++;  save(); }
}