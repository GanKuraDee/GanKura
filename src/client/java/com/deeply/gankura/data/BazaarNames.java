package com.deeply.gankura.data;

import com.deeply.gankura.util.JsonFetch;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 表示名から SkyBlock の ID を引く。
 *
 * Bazaar の注文画面に並ぶ品から ID が読めなかったときの控え。
 * 名前しか手掛かりが無いので、Hypixel が配っている品の一覧で引き当てる。
 * ID が読める品では一度も使われないので、必要になってから取りに行く
 */
public final class BazaarNames {

    private static final Logger LOGGER = LoggerFactory.getLogger("GanKura/BazaarNames");

    private static final String ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items";

    private static volatile Map<String, String> idsByName = Map.of();
    private static final AtomicBoolean asked = new AtomicBoolean();

    private BazaarNames() {
    }

    /** 分からなければ null。初めて聞かれたときは、裏で一覧を取りに行く */
    public static String idOf(String displayName) {
        if (asked.compareAndSet(false, true)) JsonFetch.run(BazaarNames::fetch);

        return idsByName.get(displayName);
    }

    private static void fetch() {
        try (JsonReader reader = JsonFetch.open(ITEMS_URL)) {
            if (reader == null) throw new IOException(ITEMS_URL + " is gone");

            idsByName = read(reader);
        } catch (Exception e) {
            // 次に聞かれたときにもう一度取りに行く
            asked.set(false);
            LOGGER.warn("Could not read the item names: {}", e.toString());
        }
    }

    private static Map<String, String> read(JsonReader reader) throws IOException {
        Map<String, String> names = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()) {
            if (!reader.nextName().equals("items")) {
                reader.skipValue();
                continue;
            }

            reader.beginArray();
            while (reader.hasNext()) {
                readItem(reader, names);
            }
            reader.endArray();
        }
        reader.endObject();

        return names;
    }

    private static void readItem(JsonReader reader, Map<String, String> names) throws IOException {
        String id = null;
        String name = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "id" -> id = reader.nextString();
                case "name" -> name = reader.nextString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();

        if (id != null && name != null) names.put(name.trim(), id);
    }
}
