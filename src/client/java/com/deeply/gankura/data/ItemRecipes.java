package com.deeply.gankura.data;

import com.deeply.gankura.util.JsonFetch;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作り方を控えておく。
 *
 * Hypixel の API は作り方を配っていないので、
 * NotEnoughUpdates が集めている一覧から、その品の分だけを取ってくる。
 * 一度読んだものは覚えておくので、同じ品で読み直すことはない
 */
public final class ItemRecipes {

    private static final Logger LOGGER = LoggerFactory.getLogger("GanKura/ItemRecipes");

    private static final String ITEM_URL =
            "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/%s.json";

    // 作業台の枠。並び順は値段に関わらないので、あるものを数えるだけでよい
    private static final String[] GRID_SLOTS = {"A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3"};

    /** 材料と、それで何個できるか */
    public record Recipe(Map<String, Integer> ingredients, int count) {
    }

    // 作り方が無いことも答えのうちなので、それ用の目印を控えておく
    private static final Recipe NONE = new Recipe(Map.of(), 0);

    private static final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    private static final Set<String> asked = ConcurrentHashMap.newKeySet();

    private ItemRecipes() {
    }

    /**
     * その品の作り方。
     *
     * まだ手元に無ければ裏で取りに行き、その場は null を返す。
     * 次に同じ品を見たときには間に合っている
     */
    public static Recipe of(String itemId) {
        Recipe recipe = recipes.get(itemId);
        if (recipe != null) return recipe == NONE ? null : recipe;

        if (asked.add(itemId)) JsonFetch.run(() -> fetch(itemId));
        return null;
    }

    private static void fetch(String itemId) {
        try (JsonReader reader = JsonFetch.open(String.format(ITEM_URL, itemId))) {
            if (reader == null) {
                // 一覧に載っていない品。聞き直しても無駄なので、無いものとして覚える
                recipes.put(itemId, NONE);
                return;
            }

            Recipe recipe = parse(JsonParser.parseReader(reader).getAsJsonObject());
            recipes.put(itemId, recipe == null ? NONE : recipe);
        } catch (Exception e) {
            // 通信に失敗しただけかもしれないので、覚えずにもう一度聞けるようにする
            asked.remove(itemId);
            LOGGER.warn("Could not read the recipe of {}: {}", itemId, e.toString());
        }
    }

    private static Recipe parse(JsonObject root) {
        // 昔からの置き方。作業台の枠がそのまま並んでいる
        if (root.has("recipe")) return fromGrid(root.getAsJsonObject("recipe"), 1);

        if (!root.has("recipes")) return null;

        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject entry = element.getAsJsonObject();
            int count = entry.has("count") ? entry.get("count").getAsInt() : 1;

            // Forge などは材料が一列に並んでいる
            Recipe recipe = entry.has("inputs")
                    ? fromInputs(entry, count)
                    : fromGrid(entry, count);
            if (recipe != null) return recipe;
        }

        return null;
    }

    private static Recipe fromGrid(JsonObject grid, int count) {
        Map<String, Integer> ingredients = new HashMap<>();

        for (String slot : GRID_SLOTS) {
            JsonElement element = grid.get(slot);
            if (element != null && element.isJsonPrimitive()) add(ingredients, element.getAsString());
        }

        return ingredients.isEmpty() ? null : new Recipe(ingredients, Math.max(count, 1));
    }

    private static Recipe fromInputs(JsonObject entry, int count) {
        Map<String, Integer> ingredients = new HashMap<>();

        for (JsonElement element : entry.getAsJsonArray("inputs")) {
            if (element.isJsonPrimitive()) add(ingredients, element.getAsString());
        }

        return ingredients.isEmpty() ? null : new Recipe(ingredients, Math.max(count, 1));
    }

    /**
     * "ENCHANTED_IRON:32" のような1枠分を足す。
     *
     * ID にも ":" が入ることがある("INK_SACK:3:16" は INK_SACK:3 が16個)ので、
     * 後ろの ":" で切る
     */
    private static void add(Map<String, Integer> ingredients, String token) {
        if (token.isEmpty()) return;

        String id = token;
        int amount = 1;

        int mark = token.lastIndexOf(':');
        if (mark > 0) {
            try {
                amount = Integer.parseInt(token.substring(mark + 1));
                id = token.substring(0, mark);
            } catch (NumberFormatException ignored) {
                // 個数ではなく ID の一部だった。token をそのまま ID として使う
            }
        }

        if (amount <= 0) return;
        ingredients.merge(marketId(id), amount, Integer::sum);
    }

    /**
     * 値段表と同じ形の ID にそろえる。
     *
     * 一覧は木材などの種類を "LOG_2-1" と書くが、Bazaar は "LOG_2:1" と書く
     */
    private static String marketId(String id) {
        int mark = id.lastIndexOf('-');
        if (mark <= 0 || mark == id.length() - 1) return id;

        for (int i = mark + 1; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) return id;
        }

        return id.substring(0, mark) + ':' + id.substring(mark + 1);
    }
}
