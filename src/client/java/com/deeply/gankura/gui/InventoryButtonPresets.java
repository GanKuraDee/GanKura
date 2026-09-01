package com.deeply.gankura.gui;

import com.deeply.gankura.data.InventoryButton;
import com.deeply.gankura.data.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ボタンの並びのひな型。
 *
 * Mod が持っている "Empty"(どこにも割り当てていない並び)と、
 * 利用者がエディタで保存した並びの2種類がある。
 * 保存した分は設定ファイルに入るので、次に開いたときもそのまま出る
 */
public final class InventoryButtonPresets {

    // 元に戻すためのひな型。上書きも削除もできない
    public static final String BUILT_IN_NAME = "Empty";

    // 保存できる数。これ以上は一覧の欄に並びきらない
    public static final int MAX_SAVED = 12;

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static LinkedHashMap<String, List<InventoryButton>> builtIn = null;

    private InventoryButtonPresets() {
    }

    // 一覧に出す並び。Mod が持っているものが先、保存したものが後
    public static LinkedHashMap<String, List<InventoryButton>> presets() {
        LinkedHashMap<String, List<InventoryButton>> presets = new LinkedHashMap<>(builtIn());
        presets.putAll(saved());
        return presets;
    }

    public static LinkedHashMap<String, List<InventoryButton>> saved() {
        return ModConfig.INSTANCE.inventoryButtons.savedPresets;
    }

    public static boolean isBuiltIn(String name) {
        return builtIn().containsKey(name);
    }

    /**
     * 今の並びに名前を付けて保存する。同じ名前があれば上書きする。
     *
     * 保存できなかったときはその理由を返す。保存できたときは null
     */
    public static String save(String name, List<InventoryButton> buttons) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "Enter a name first.";
        if (isBuiltIn(trimmed)) return "That name is reserved.";

        LinkedHashMap<String, List<InventoryButton>> saved = saved();
        if (!saved.containsKey(trimmed) && saved.size() >= MAX_SAVED) {
            return "No room left. Delete one first.";
        }

        saved.put(trimmed, copyOf(buttons));
        ModConfig.INSTANCE.saveNow();
        return null;
    }

    public static boolean delete(String name) {
        if (isBuiltIn(name) || saved().remove(name) == null) return false;

        ModConfig.INSTANCE.saveNow();
        return true;
    }

    // 名前を入れずに保存したときの名前。空いている番号を使う
    public static String nextName() {
        for (int i = 1; ; i++) {
            String name = "Preset " + i;
            if (!saved().containsKey(name)) return name;
        }
    }

    private static LinkedHashMap<String, List<InventoryButton>> builtIn() {
        if (builtIn != null) return builtIn;

        builtIn = new LinkedHashMap<>();
        try (InputStream stream = InventoryButtonPresets.class
                .getResourceAsStream("/assets/gankura/invbuttons/presets.json")) {
            if (stream != null) {
                JsonObject json = GSON.fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (entry.getValue().isJsonArray()) {
                        builtIn.put(entry.getKey(), readButtons(entry.getValue().getAsJsonArray()));
                    }
                }
            }
        } catch (Exception ignored) {
            // 読めなければひな型が出ないだけで、他の操作はできる
        }
        return builtIn;
    }

    private static List<InventoryButton> readButtons(JsonArray array) {
        List<InventoryButton> buttons = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;

            InventoryButton button = GSON.fromJson(element, InventoryButton.class);
            if (button != null) buttons.add(button);
        }
        return buttons;
    }

    // 作業台の枠の位置(スロット98,18)から、枠線1つ分だけ外に出した所
    private static final int CRAFTING_COLUMN = 97;
    private static final int CRAFTING_ROW = 17;

    /**
     * 何も設定していないときの並び。
     *
     * どれもコマンドが空なので、エディタを開くまでは画面に何も出ない。
     * 置き場所だけ先に用意しておいて、使いたい所を選んでもらう形にしている
     */
    public static List<InventoryButton> defaults() {
        List<InventoryButton> buttons = new ArrayList<>();

        // 作業台の2x2の枠の上
        buttons.add(empty(CRAFTING_COLUMN, CRAFTING_ROW, true, false, false));
        buttons.add(empty(CRAFTING_COLUMN + 18, CRAFTING_ROW, true, false, false));
        buttons.add(empty(CRAFTING_COLUMN, CRAFTING_ROW + 18, true, false, false));
        buttons.add(empty(CRAFTING_COLUMN + 18, CRAFTING_ROW + 18, true, false, false));
        // 作業台の完成品の枠
        buttons.add(empty(153, 27, true, false, false));

        // 画面の外側。下半分は下端を起点にして、背の高い画面でも同じ見た目にする
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            boolean bottom = y >= 80;
            buttons.add(empty(2, bottom ? y - 166 : y, false, true, bottom));
        }
        for (int i = 0; i < 8; i++) buttons.add(empty(4 + 21 * i, -19, false, false, false));
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            boolean bottom = y >= 80;
            buttons.add(empty(-19, bottom ? y - 166 : y, false, false, bottom));
        }
        for (int i = 0; i < 8; i++) buttons.add(empty(4 + 21 * i, 2, false, false, true));

        return buttons;
    }

    private static InventoryButton empty(int x, int y, boolean playerInvOnly, boolean anchorRight, boolean anchorBottom) {
        return new InventoryButton(x, y, "", playerInvOnly, anchorRight, anchorBottom, 0, "");
    }

    /**
     * 保存済みの並びを、今の置き場所に合わせ直す。
     *
     * 移植した当初は NotEnoughUpdates の 1.8 用の座標をそのまま使っていたため、
     * 作業台まわりのボタンがスロットとずれていた。これを読み替え、
     * 併せて、使わないことにした置き場所のボタンを取り除く
     */
    public static void migrateLegacyPositions(List<InventoryButton> buttons) {
        for (InventoryButton button : buttons) {
            if (button == null || !button.playerInvOnly) continue;
            if (button.anchorRight || button.anchorBottom) continue;

            int[] moved = LEGACY_POSITIONS.get(button.x + "," + button.y);
            if (moved == null) continue;

            button.x = moved[0];
            button.y = moved[1];
        }

        // 使わないことにした置き場所は、設定に残っていても取り除く
        buttons.removeIf(button -> button != null && button.playerInvOnly
                && !button.anchorRight && !button.anchorBottom
                && REMOVED_POSITIONS.contains(button.x + "," + button.y));
    }

    /**
     * もう置き場所として使わない所。
     *
     * 自分の姿が映っている枠の四隅と、作業台の上下に並べていた段。
     * 見た目の邪魔になるので、ひな型からも既定の並びからも外した
     */
    private static final Set<String> REMOVED_POSITIONS = Set.of(
            "26,8", "60,8", "26,60", "60,60",
            "97,0", "116,0", "135,0", "154,0",
            "97,55", "116,55", "135,55", "154,55");

    // 1.8 の座標 -> 今の座標。作業台の枠が (88,26) から (98,18) へ動いた分の差
    private static final Map<String, int[]> LEGACY_POSITIONS = Map.ofEntries(
            Map.entry("87,5", new int[]{97, 0}),
            Map.entry("108,5", new int[]{116, 0}),
            Map.entry("129,5", new int[]{135, 0}),
            Map.entry("150,5", new int[]{154, 0}),
            Map.entry("87,63", new int[]{97, 55}),
            Map.entry("108,63", new int[]{116, 55}),
            Map.entry("129,63", new int[]{135, 55}),
            Map.entry("150,63", new int[]{154, 55}),
            Map.entry("87,25", new int[]{97, 17}),
            Map.entry("105,25", new int[]{115, 17}),
            Map.entry("87,43", new int[]{97, 35}),
            Map.entry("105,43", new int[]{115, 35}),
            Map.entry("143,35", new int[]{153, 27}));

    // ひな型はそのまま渡すと編集で書き換わってしまうので、複製して渡す
    public static List<InventoryButton> copyOf(List<InventoryButton> buttons) {
        List<InventoryButton> copy = new ArrayList<>();
        for (InventoryButton button : buttons) {
            if (button != null) copy.add(button.copy());
        }
        return copy;
    }
}
