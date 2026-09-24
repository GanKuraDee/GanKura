package com.deeply.gankura.data;

import com.deeply.gankura.gui.InventoryButtonPresets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * インベントリボタンの配置と、名前を付けて保存した並び。
 *
 * ResourcefulConfig は Map を扱えず、ボタンの並びも設定画面ではなく専用のエディタで編集するため、
 * 設定項目とは切り離して自前の JSON に置いている。
 */
public final class InventoryButtonStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.LOGGER_NAME);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Type BUTTONS_TYPE = new TypeToken<List<InventoryButton>>() {
    }.getType();
    private static final Type PRESETS_TYPE = new TypeToken<LinkedHashMap<String, List<InventoryButton>>>() {
    }.getType();

    /** 置いたボタンの一覧 */
    private static List<InventoryButton> buttons = InventoryButtonPresets.defaults();

    /** 名前を付けて保存した並び */
    private static LinkedHashMap<String, List<InventoryButton>> savedPresets = new LinkedHashMap<>();

    private InventoryButtonStore() {
    }

    private static File file() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "gankura");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "gankura_buttons.json");
    }

    public static List<InventoryButton> buttons() {
        return buttons;
    }

    public static LinkedHashMap<String, List<InventoryButton>> savedPresets() {
        return savedPresets;
    }

    public static void load() {
        File file = file();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) read(root);
            } catch (Exception e) {
                LOGGER.warn("Failed to read gankura_buttons.json, keeping the defaults", e);
            }
        }
        normalize();
        save();
    }

    /** 旧 MoulConfig 形式の設定ファイルに入っていたボタンを引き継ぐ */
    static void importLegacy(JsonObject inventoryButtons) {
        if (inventoryButtons == null) return;
        read(inventoryButtons);
        normalize();
        save();
    }

    private static void read(JsonObject root) {
        if (root.has("buttons")) {
            List<InventoryButton> loaded = GSON.fromJson(root.get("buttons"), BUTTONS_TYPE);
            if (loaded != null) buttons = loaded;
        }
        if (root.has("savedPresets")) {
            LinkedHashMap<String, List<InventoryButton>> loaded =
                    GSON.fromJson(root.get("savedPresets"), PRESETS_TYPE);
            if (loaded != null) savedPresets = loaded;
        }
    }

    // ボタンを全部消した状態も設定のうちなので、未設定(null)のときだけひな型に戻す
    private static void normalize() {
        if (buttons == null) buttons = InventoryButtonPresets.defaults();
        buttons.removeIf(Objects::isNull);
        InventoryButtonPresets.migrateLegacyPositions(buttons);

        if (savedPresets == null) savedPresets = new LinkedHashMap<>();
        for (List<InventoryButton> preset : savedPresets.values()) {
            if (preset != null) InventoryButtonPresets.migrateLegacyPositions(preset);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.add("buttons", GSON.toJsonTree(buttons, BUTTONS_TYPE));
        root.add("savedPresets", GSON.toJsonTree(savedPresets, PRESETS_TYPE));
        try (FileWriter writer = new FileWriter(file())) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save gankura_buttons.json", e);
        }
    }
}
