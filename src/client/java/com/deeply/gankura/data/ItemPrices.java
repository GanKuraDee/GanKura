package com.deeply.gankura.data;

import com.deeply.gankura.util.JsonFetch;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bazaar と Auction House の値段を控えておく。
 *
 * Bazaar は Hypixel の API をそのまま読む。
 * Auction House は全件だと数十MBあり毎分取り直せないので、
 * 最安 BIN だけをまとめて配っている外の置き場から読む
 */
public final class ItemPrices {

    private static final Logger LOGGER = LoggerFactory.getLogger("GanKura/ItemPrices");

    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";

    // 最安 BIN の置き場。上から順に試し、駄目なら次に移る
    private static final List<String> LOWEST_BIN_URLS = List.of(
            "https://hysky.de/api/auctions/lowestbins",
            "https://lb.tricked.pro/lowestbins");

    // 取り直す間隔(ミリ秒)。どちらの元も1分ごとに更新される
    private static final long REFRESH_MS = 60_000;

    /**
     * その品の Bazaar の値段。
     *
     * {@code instantBuy} と {@code instantSell} は上澄みをならした値で、
     * 見せる分にはこちらが落ち着いている。
     * {@code lowestOffer} と {@code highestOrder} は今出ている中の一番手そのもので、
     * 自分の注文が一番手かどうかを見るときはこちらでないと判断を誤る
     */
    public record Bazaar(double instantBuy, double instantSell, double lowestOffer, double highestOrder) {
    }

    private static volatile Map<String, Bazaar> bazaar = Map.of();
    private static volatile Map<String, Double> lowestBin = Map.of();
    private static volatile long lastAttempt = 0;
    // 落ちている間ログを埋めないよう、直前の結果を覚えておく
    private static volatile boolean lastFailed = false;
    private static final AtomicBoolean fetching = new AtomicBoolean();

    private ItemPrices() {
    }

    /** その品の Bazaar の値段。無ければ null */
    public static Bazaar bazaar(String itemId) {
        return bazaar.get(itemId);
    }

    /** その品の最安 BIN。無ければ null */
    public static Double lowestBin(String itemId) {
        return lowestBin.get(itemId);
    }

    /**
     * 値段が古ければ裏で取り直す。
     *
     * ツールチップから呼ばれるので、待たせずにすぐ戻る。
     * 取れるまでは前の値段を出し続ける
     */
    public static void refreshIfStale() {
        if (System.currentTimeMillis() - lastAttempt < REFRESH_MS) return;
        if (!fetching.compareAndSet(false, true)) return;

        lastAttempt = System.currentTimeMillis();
        JsonFetch.run(() -> {
            try {
                fetch();
            } finally {
                fetching.set(false);
            }
        });
    }

    // 片方が取れなくても、もう片方は出せるようにする
    private static void fetch() {
        boolean failed = false;

        try {
            bazaar = readBazaar();
        } catch (Exception e) {
            failed = true;
            if (!lastFailed) LOGGER.warn("Could not read the Bazaar prices: {}", e.toString());
        }

        try {
            lowestBin = readLowestBin();
        } catch (Exception e) {
            failed = true;
            if (!lastFailed) LOGGER.warn("Could not read the lowest BIN prices: {}", e.toString());
        }

        lastFailed = failed;
    }

    private static Map<String, Bazaar> readBazaar() throws IOException, InterruptedException {
        try (JsonReader reader = JsonFetch.open(BAZAAR_URL)) {
            if (reader == null) throw new IOException(BAZAAR_URL + " is gone");

            Map<String, Bazaar> prices = new HashMap<>();

            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("products")) {
                    reader.skipValue();
                    continue;
                }

                reader.beginObject();
                while (reader.hasNext()) {
                    String itemId = reader.nextName();
                    Bazaar price = readProduct(reader);
                    if (price != null) prices.put(itemId, price);
                }
                reader.endObject();
            }
            reader.endObject();

            return prices;
        }
    }

    private static Bazaar readProduct(JsonReader reader) throws IOException {
        double instantBuy = 0;
        double instantSell = 0;
        double lowestOffer = 0;
        double highestOrder = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "quick_status" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        switch (reader.nextName()) {
                            case "buyPrice" -> instantBuy = reader.nextDouble();
                            case "sellPrice" -> instantSell = reader.nextDouble();
                            default -> reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
                // 売りに出されている中で一番安いもの。買うならこの値段になる
                case "buy_summary" -> lowestOffer = readBest(reader, true);
                // 買い注文の中で一番高いもの。売るならこの値段になる
                case "sell_summary" -> highestOrder = readBest(reader, false);
                default -> reader.skipValue();
            }
        }
        reader.endObject();

        // 誰も出品していない品は 0 になる。値段として出せないので落とす
        return instantBuy <= 0 && instantSell <= 0
                ? null
                : new Bazaar(instantBuy, instantSell, lowestOffer, highestOrder);
    }

    // 並び順を当てにせず、その場で一番手を選ぶ
    private static double readBest(JsonReader reader, boolean lowest) throws IOException {
        double best = 0;

        reader.beginArray();
        while (reader.hasNext()) {
            double price = 0;

            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("pricePerUnit")) {
                    price = reader.nextDouble();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();

            if (price <= 0) continue;
            if (best <= 0 || (lowest ? price < best : price > best)) best = price;
        }
        reader.endArray();

        return best;
    }

    private static Map<String, Double> readLowestBin() throws IOException, InterruptedException {
        IOException last = null;

        for (String url : LOWEST_BIN_URLS) {
            try (JsonReader reader = JsonFetch.open(url)) {
                if (reader == null) {
                    last = new IOException(url + " is gone");
                    continue;
                }

                Map<String, Double> prices = new HashMap<>();

                reader.beginObject();
                while (reader.hasNext()) {
                    prices.put(reader.nextName(), reader.nextDouble());
                }
                reader.endObject();

                if (!prices.isEmpty()) return prices;
                last = new IOException(url + " answered with nothing");
            } catch (IOException e) {
                last = e;
            }
        }

        throw last == null ? new IOException("no lowest BIN source was listed") : last;
    }
}
