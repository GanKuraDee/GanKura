package com.deeply.gankura.util;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * 外から JSON を取ってくるための共通の道具。
 *
 * 通信でゲームを止めないよう、取得は専用のスレッドに回す
 */
public final class JsonFetch {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    // 値段の取り直しと、レシピの取得が互いを待たずに済むだけの数
    private static final int WORKERS = 2;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Executor FETCHER = Executors.newFixedThreadPool(WORKERS, runnable -> {
        Thread thread = new Thread(runnable, "GanKura Fetch");
        thread.setDaemon(true);
        return thread;
    });

    private JsonFetch() {
    }

    /** 裏で走らせる。呼んだ側は待たない */
    public static void run(Runnable task) {
        FETCHER.execute(task);
    }

    /** 置いていないものは null。読み終えたら閉じること */
    public static JsonReader open(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                // Bazaar は数MBあるので、縮めて送ってもらう
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", "GanKura")
                .GET()
                .build();

        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            response.body().close();
            return null;
        }
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException(url + " answered " + response.statusCode());
        }

        InputStream body = response.body();
        try {
            if (response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip")) {
                body = new GZIPInputStream(body);
            }
        } catch (IOException e) {
            body.close();
            throw e;
        }

        return new JsonReader(new InputStreamReader(body, StandardCharsets.UTF_8));
    }
}
