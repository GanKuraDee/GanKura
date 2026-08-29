package com.deeply.gankura.handler;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.scanner.BeeNestScanner;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("NetworkHandler");

    public static void init() {
        // サーバー参加・移動時の処理
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GameState.resetAll();
            PetHandler.reset();
            // Nether ボスの不在計測・ラッチは GameState の外にあるため個別にリセットする。
            // これを消さないとエリアを入り直しても前回のスキャン結果が残り、
            // Unknown ではなく Spawned/Killed から始まってしまう
            EntityHighlightManager.resetCrimsonBossTracking();
            // 見つけていた Floor Drop はワールドが変わると意味を成さないので捨てる
            FloorDropHandler.reset();
            BeeNestScanner.reset();
            // 保存されていたSkyblock Equipmentを、レジストリアクセスが手に入ったこのタイミングで復元する
            EquipmentState.hydrate(handler.getRegistryManager());
        });

        // チャット受信時の処理 (純粋なルーター/ディスパッチャー)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // 1. アクションバーのメッセージは専用ハンドラーへ
            if (overlay) {
                ArmorStackHandler.handleActionBar(message);
                return true;
            }

            String msg = message.getString();
            String unformattedMsg = msg.replaceAll("§[0-9a-fk-or]", "");
            MinecraftClient client = MinecraftClient.getInstance();

            // 2. 各ドメイン(機能)への純粋な委譲
            // NetworkHandler自身は、メッセージの中身が何なのか一切気にせず担当者に投げるだけ！
            ServerRestartHandler.handleChat(unformattedMsg, client);
            PetHandler.handleMessage(message);
            CrimsonDropHandler.handleMessage(unformattedMsg);
            WarpCooldownHandler.handleMessage(unformattedMsg);
            ArachneHandler.handleMessage(unformattedMsg, client);
            ForagingHandler.handleMessage(unformattedMsg, client);
            // 釣り上げの文言。短縮形に差し替えたときは、元のメッセージを出さない
            if (SeaCreatureCatchHandler.handleMessage(unformattedMsg, client)) return false;

            if (DragonHandler.handleMessage(msg, client)) {
                return true;
            }

            // Golemに関する全てのチャット処理を委譲
            GolemHandler.handleMessage(msg, client);

            return true;
        });
    }
}