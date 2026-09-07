package com.deeply.gankura.handler;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.scanner.BeeNestScanner;
import com.deeply.gankura.scanner.CorpseScanner;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
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
            // 割り出した Wishing Compass の目標はロビーごとに変わるので、ワールドが変わったら捨てる
            WishingCompassHandler.reset();
            // 視点の固定は庭の中だけの話なので、ワールドが変わったら解く
            MousematHandler.reset();
            // 割り出した害虫の居場所も、庭を出たら意味を成さない
            PestVacuumHandler.reset();
            // 保存されていたSkyblock Equipmentを、レジストリアクセスが手に入ったこのタイミングで復元する
            EquipmentState.hydrate(handler.registryAccess());
        });

        // チャット受信時の処理 (純粋なルーター/ディスパッチャー)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // 1. アクションバーのメッセージは専用ハンドラーへ
            if (overlay) {
                ArmorStackHandler.handleActionBar(message);
                return true;
            }

            // messageはMojang環境ではComponentクラスとなりますが、getString()はそのまま使用可能です
            String msg = message.getString();
            String unformattedMsg = msg.replaceAll("§[0-9a-fk-or]", "");
            Minecraft client = Minecraft.getInstance();

            // 2. プレイヤーの発言はここで止める。
            // 下の担当者はどれもサーバーからの知らせだけを見ているので、
            // 誰かが同じ文言を発言しただけのときに反応させない。
            // 発言そのものを扱う機能を足すときは、ここより前で受け取る
            if (ModConstants.isPlayerChat(unformattedMsg)) return true;

            // 決まった文面の流し読み用のチャットは、ここで捨てる
            if (ChatFilterHandler.shouldHide(unformattedMsg)) return false;

            // 3. 各ドメイン(機能)への純粋な委譲
            // NetworkHandler自身は、メッセージの中身が何なのか一切気にせず担当者に投げるだけ！
            ServerRestartHandler.handleChat(unformattedMsg, client);
            PetHandler.handleMessage(message);
            CrimsonDropHandler.handleMessage(unformattedMsg);
            WarpCooldownHandler.handleMessage(unformattedMsg);
            GoldenFishHandler.handleMessage(unformattedMsg, client);
            ArachneHandler.handleMessage(unformattedMsg, client);
            ForagingHandler.handleMessage(unformattedMsg, client);
            CorpseScanner.handleMessage(unformattedMsg, client);
            CorpseProfitHandler.handleMessage(unformattedMsg, client);
            MousematHandler.handleMessage(unformattedMsg, client);
            PestSpawnHandler.handleMessage(unformattedMsg, client);
            // 来客の名前は色ごと題に出すので、色を持ったまま渡す
            VisitorHandler.handleMessage(message, client);
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
