package com.deeply.gankura.data;

import com.deeply.gankura.config.ConfigGroups;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * MoulConfig を使っていた頃の設定ファイルを、一度だけ ResourcefulConfig 側へ移す。
 *
 * カテゴリ名もフィールド名も当時のまま引き継いでいるので、
 * 旧 JSON のキーと {@link Category#value()} / {@link ConfigEntry#id()} をそのまま突き合わせれば足りる。
 * 取り込みが済んだファイルは拡張子を変えて残しておく（消さずに戻せるようにしておく）。
 */
final class LegacyConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.LOGGER_NAME);

    private static final String LEGACY_NAME = "gankura_config.properties";
    private static final String DONE_SUFFIX = ".migrated";

    private static final Gson GSON = new Gson();

    private static final String GROUP_PREFIX = "gankura.config.";
    private static final String GROUP_SEPARATOR = ".sep.";

    private LegacyConfigMigration() {
    }

    static void run() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "gankura");
        File legacy = new File(dir, LEGACY_NAME);
        if (!legacy.exists()) return;

        JsonObject root;
        try (FileReader reader = new FileReader(legacy)) {
            root = GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            LOGGER.warn("Could not read the old config file, starting from the defaults", e);
            markDone(legacy);
            return;
        }
        if (root == null) {
            markDone(legacy);
            return;
        }

        // ModConfig.load() の中で register より前に呼ばれるため、
        // ここで入れた値がそのまま ResourcefulConfig の初期値として保存される
        applyCategories(root, ModConfig.class.getAnnotation(Config.class).categories());
        ModConfig.expandedAccordions = openAccordions(root);
        InventoryButtonStore.importLegacy(objectOrNull(root, "inventoryButtons"));

        markDone(legacy);
        LOGGER.info("Imported the old GanKura config into config/{}.jsonc", ModConfig.CONFIG_ID);
    }

    private static void applyCategories(JsonObject parent, Class<?>[] categories) {
        for (Class<?> category : categories) {
            Category data = category.getAnnotation(Category.class);
            if (data == null) continue;
            JsonObject json = objectOrNull(parent, data.value());
            if (json == null) continue;
            applyFields(json, category);
            applyCategories(json, data.categories());
        }
    }

    private static void applyFields(JsonObject json, Class<?> category) {
        for (Field field : category.getDeclaredFields()) {
            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null || !Modifier.isStatic(field.getModifiers())
                    || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            // キーバインドは引き継がない。26.3 で入力が GLFW から SDL に変わり、
            // 旧ファイルに入っている GLFW のキーコードは別のキーを指してしまうため
            if (field.isAnnotationPresent(ConfigOption.Keybind.class)) continue;

            JsonElement value = json.get(entry.id());
            if (value == null || value.isJsonNull()) continue;
            try {
                // 旧 List<列挙> は配列になっているが、Gson は JSON 配列から素直に読み替えてくれる
                field.set(null, GSON.fromJson(value, field.getGenericType()));
            } catch (Exception e) {
                LOGGER.warn("Could not carry over {}.{} from the old config",
                        category.getSimpleName(), field.getName(), e);
            }
        }
    }

    /**
     * MoulConfig ではアコーディオンの開閉も true/false で保存していたので、開いていたものは開いたまま引き継ぐ。
     * 見出しのキーは "gankura.config.<カテゴリの道筋>.sep.<見出しのフィールド名>" の形
     */
    private static String[] openAccordions(JsonObject root) {
        List<String> open = new ArrayList<>();
        for (String group : ConfigGroups.ALL) {
            String rest = group.substring(GROUP_PREFIX.length());
            int sep = rest.lastIndexOf(GROUP_SEPARATOR);
            if (sep < 0) continue;

            JsonObject json = root;
            for (String part : rest.substring(0, sep).split("\\.")) {
                json = json == null ? null : objectOrNull(json, part);
            }
            if (json == null) continue;

            JsonElement value = json.get(rest.substring(sep + GROUP_SEPARATOR.length()));
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                    && value.getAsBoolean()) {
                open.add(group);
            }
        }
        return open.toArray(new String[0]);
    }

    private static JsonObject objectOrNull(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static void markDone(File legacy) {
        File done = new File(legacy.getParentFile(), LEGACY_NAME + DONE_SUFFIX);
        if (done.exists()) done.delete();
        if (!legacy.renameTo(done)) {
            LOGGER.warn("Could not rename {}; it will be read again on the next launch", LEGACY_NAME);
        }
    }
}
