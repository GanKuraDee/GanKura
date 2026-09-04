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
 * シャードの名前から、Bazaar での ID を引く。
 *
 * シャードは中身が違っても custom_data の ID がどれも "ATTRIBUTE_SHARD" で、
 * 名前しか手掛かりが無い。
 * "Snoozle" が "SHARD_SNOOZLE" のように綴りどおりとは限らず
 * ("Bogged" は "SHARD_SEA_ARCHER")、決め打ちでは当てられないので、
 * NotEnoughUpdates が集めている対応表を使う
 */
public final class AttributeShards {

    private static final Logger LOGGER = LoggerFactory.getLogger("GanKura/AttributeShards");

    private static final String SHARDS_URL =
            "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/attribute_shards.json";

    private static volatile Map<String, String> idsByName = Map.of();
    private static final AtomicBoolean asked = new AtomicBoolean();

    private AttributeShards() {
    }

    /** 分からなければ null。初めて聞かれたときは、裏で対応表を取りに行く */
    public static String idOf(String shardName) {
        if (asked.compareAndSet(false, true)) JsonFetch.run(AttributeShards::fetch);

        return idsByName.get(shardName);
    }

    private static void fetch() {
        try (JsonReader reader = JsonFetch.open(SHARDS_URL)) {
            if (reader == null) throw new IOException(SHARDS_URL + " is gone");

            idsByName = read(reader);
        } catch (Exception e) {
            // 次に聞かれたときにもう一度取りに行く
            asked.set(false);
            LOGGER.warn("Could not read the shard names: {}", e.toString());
        }
    }

    private static Map<String, String> read(JsonReader reader) throws IOException {
        Map<String, String> names = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()) {
            if (!reader.nextName().equals("attributes")) {
                reader.skipValue();
                continue;
            }

            reader.beginArray();
            while (reader.hasNext()) {
                readShard(reader, names);
            }
            reader.endArray();
        }
        reader.endObject();

        return names;
    }

    private static void readShard(JsonReader reader, Map<String, String> names) throws IOException {
        String id = null;
        String name = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "bazaarName" -> id = reader.nextString();
                case "displayName" -> name = reader.nextString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();

        // Bazaar で売られていないシャードは、対応表でも ID が空になっている
        if (id != null && !id.isEmpty() && name != null) names.put(name.trim(), id);
    }
}
