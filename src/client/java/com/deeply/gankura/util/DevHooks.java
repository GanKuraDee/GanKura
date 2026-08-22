package com.deeply.gankura.util;

import com.deeply.gankura.data.ModConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 開発中に手元だけで動かす、一時的な機能の受け口。
 *
 * 中身は com.deeply.gankura.dev に置く。このパッケージは .gitignore の対象で、
 * かつ -PdevFeatures を付けないとビルドにも含まれないため、
 * コミットにも配布用の jar にも入らない。
 * 入口をここに集約しているので、本体側は「あれば動く・無ければ何もしない」で済む。
 */
public class DevHooks {

    private static final String ENTRYPOINT = "com.deeply.gankura.dev.DevFeatures";

    // ワールド描画のたびに呼ぶもの。一時的な機能が自分で登録する
    private static final List<Consumer<Minecraft>> WORLD_RENDERERS = new ArrayList<>();

    // /gankura の下にぶら下げるサブコマンド。一時的な機能が自分で登録する
    private static final List<Consumer<LiteralArgumentBuilder<FabricClientCommandSource>>> SUBCOMMANDS = new ArrayList<>();

    public static void load() {
        try {
            Class.forName(ENTRYPOINT).getMethod("register").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // 配布ビルドにはクラスごと入っていない。これが通常の状態
        } catch (ReflectiveOperationException e) {
            LoggerFactory.getLogger(ModConstants.LOGGER_NAME).warn("Failed to load dev features", e);
        }
    }

    public static void onWorldRender(Consumer<Minecraft> renderer) {
        WORLD_RENDERERS.add(renderer);
    }

    public static void onBuildCommand(Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> builder) {
        SUBCOMMANDS.add(builder);
    }

    public static void buildCommand(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        for (Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> builder : SUBCOMMANDS) {
            builder.accept(command);
        }
    }

    public static void renderWorld(Minecraft client) {
        for (Consumer<Minecraft> renderer : WORLD_RENDERERS) {
            renderer.accept(client);
        }
    }
}
