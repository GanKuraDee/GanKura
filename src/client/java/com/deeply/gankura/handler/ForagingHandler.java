package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;

public class ForagingHandler {

    public static void handleMessage(String unformattedMsg, MinecraftClient client) {
        // 木を一度に切り倒せた合図。チャットに埋もれると見逃すのでタイトルで知らせる
        if (ModConfig.INSTANCE.foraging.enableTreeFelledTitle
                && ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.TREE_FELLED_MSG)) {
            MutableText title = Text.literal("TREE FELLED!")
                    .formatted(Formatting.GREEN, Formatting.BOLD);
            client.execute(() -> NotificationUtils.showTitle(client, title, null));
            return;
        }

        // Cocooning はエリアを問わず使えるので、Critter Safari の判定より先に見る
        if (handleCocoonCatch(unformattedMsg, client)) return;

        // Wumpa と Critter のキャプチャは Critter Safari 限定。
        // 他エリアで同じ文言が流れても反応させない
        if (GameState.Server.isSafari()) {
            if (handleWumpaSpawn(unformattedMsg, client)) return;
            if (handleMacawSpawn(unformattedMsg, client)) return;
            if (handleCritterCapture(unformattedMsg)) return;
            // Wumpa のキャプチャ成功。洞窟が開く合図なので、湧いている状態から Captured へ進める
            if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.WUMPA_CAPTURED_MSG)) {
                GameState.CritterSafari.wumpaStatus = GameState.CritterSafari.STATUS_CAPTURED;
                GameState.CritterSafari.markCaptured(ModConstants.WUMPA_NAME);
                if (ModConfig.INSTANCE.foraging.enableWumpaCapsuleMessage) {
                    if (ModConfig.INSTANCE.foraging.enableWumpaCapsuleMessage) {
                    announceCapsuleUsage(client, "§b§lWumpa", GameState.CritterSafari.wumpaCapsuleHits);
                }
                }
                return;
            }
            if (handleCapsuleThrow(unformattedMsg)) return;
            if (handleDoomspiral(unformattedMsg, client)) return;
        }

        if (!ModConfig.INSTANCE.foraging.enableTreeMobTitle) return;

        // 切り倒した木からモブが降ってくるパターン。何が降ってきたかが重要なので、
        // モブ名をタイトル本体に、状況説明をサブタイトルに出す
        Matcher matcher = ModConstants.TREE_MOB_FELL_PATTERN.matcher(unformattedMsg);
        if (!matcher.find()) return;

        String mobName = matcher.group(1).trim();
        MutableText title = Text.literal(mobName)
                .formatted(Formatting.RED, Formatting.BOLD);
        MutableText subtitle = Text.literal("Fell from the Tree!")
                .formatted(Formatting.YELLOW);
        client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
    }

    // Cocooning でモブを捕まえた合図。何を捕まえたかが重要なので、
    // モブ名をタイトル本体に、状況説明をサブタイトルに出す
    private static boolean handleCocoonCatch(String unformattedMsg, MinecraftClient client) {
        Matcher matcher = ModConstants.COCOON_CAUGHT_PATTERN.matcher(unformattedMsg);
        if (!matcher.find()) return false;

        if (ModConfig.INSTANCE.foraging.enableCocoonCatchTitle) {
            String mobName = matcher.group(1).trim();
            MutableText title = Text.literal(mobName)
                    .formatted(Formatting.GREEN, Formatting.BOLD);
            MutableText subtitle = Text.literal("Cocooned!")
                    .formatted(Formatting.YELLOW);
            client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
        }
        return true;
    }

    // Wumpa のスポーンは足音のメッセージでしか分からないので、見逃さないようタイトルで知らせる
    private static boolean handleWumpaSpawn(String unformattedMsg, MinecraftClient client) {
        if (!ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.WUMPA_SPAWN_MSG)) return false;

        // Wumpaが湧いた = 8種そろったことが確定する。
        // 他プレイヤーがキャプチャした分はメッセージが来ないので、ここで全種そろった扱いにする
        GameState.CritterSafari.markAllCaptured();
        GameState.CritterSafari.wumpaStatus = GameState.CritterSafari.STATUS_SPAWNED;
        GameState.CritterSafari.wumpaCapsuleHits = 0;

        if (ModConfig.INSTANCE.foraging.enableWumpaSpawnTitle) {
            MutableText title = Text.literal("WUMPA SPAWNED!")
                    .formatted(Formatting.AQUA, Formatting.BOLD);
            MutableText subtitle = Text.literal("Icy Biome")
                    .formatted(Formatting.YELLOW);
            client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
        }
        return true;
    }

    // Macaw は Birdfeeder に寄ってきたメッセージでしか分からないので、見逃さないようタイトルで知らせる
    private static boolean handleMacawSpawn(String unformattedMsg, MinecraftClient client) {
        if (!ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.MACAW_ATTRACTED_MSG)) return false;

        if (ModConfig.INSTANCE.foraging.enableMacawSpawnTitle) {
            MutableText title = Text.literal("MACAWS ATTRACTED!")
                    .formatted(Formatting.RED, Formatting.BOLD);
            MutableText subtitle = Text.literal("Forest Biome")
                    .formatted(Formatting.YELLOW);
            client.execute(() -> NotificationUtils.showTitle(client, title, subtitle));
        }
        return true;
    }

    // Icy Biome の Critter を8種すべてキャプチャすると Wumpa が湧くため、進捗を記録してHUDに出す。
    // 他バイオームのCritterも同じ文面でキャプチャされるので、8種に一致するものだけ拾う
    private static boolean handleCritterCapture(String unformattedMsg) {
        String captured = ModConstants.findCapturedCritter(unformattedMsg);
        if (captured == null) return false;

        // チェックリストは全バイオームの Critter が対象なので、名前をそのまま記録する
        GameState.CritterSafari.markCaptured(captured);

        String critter = ModConstants.findIcyBiomeCritter(captured);
        if (critter == null) return true;

        // Wumpa は1回のSafari入場につき1体しか湧かない。湧いた後もCritter自体は捕まえ続けられるが、
        // それを数え直すと 8/8 (Spawned) の表示が 1/8 に巻き戻ってしまうため、以降は進捗を触らない
        if (GameState.CritterSafari.wumpaStatus != null) return true;

        // 8種そろった時点で Wumpa が湧く。足音のメッセージを待たずに状態を進める
        if (GameState.CritterSafari.icyCapturedCount() >= ModConstants.ICY_BIOME_CRITTERS.size()) {
            GameState.CritterSafari.wumpaStatus = GameState.CritterSafari.STATUS_SPAWNED;
            GameState.CritterSafari.wumpaCapsuleHits = 0;
        }
        return true;
    }

    // Doomspiral の儀式の進行を追う。キャンドルの本数はメッセージの文面から確定させるので、
    // 途中のメッセージを見逃しても次の1本で正しい本数に復帰する
    private static boolean handleDoomspiral(String unformattedMsg, MinecraftClient client) {
        // Doomspiral も1回のSafari入場につき1体まで。湧いた後にキャンドルをともしても
        // 2体目にはならないので、儀式の進行はスポーン確定後は更新しない
        int candles = ModConstants.doomspiralCandleCount(unformattedMsg);
        if (candles > 0 && GameState.Doomspiral.status != null) return true;
        if (candles > 0) {
            GameState.Doomspiral.litCandles = candles;
            // 4本目をともした時点ではまだ湧いていないので、召喚メッセージが来るまでは Spawning 扱い
            GameState.Doomspiral.status = candles == ModConstants.DOOMSPIRAL_CANDLE_TOTAL
                    ? GameState.Doomspiral.STATUS_SPAWNING
                    : null;
            return true;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.DOOMSPIRAL_SUMMON_MSG)) {
            GameState.Doomspiral.litCandles = ModConstants.DOOMSPIRAL_CANDLE_TOTAL;
            GameState.Doomspiral.status = GameState.Doomspiral.STATUS_SPAWNED;
            GameState.Doomspiral.capsuleHits = 0;
            return true;
        }

        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.DOOMSPIRAL_CAPTURED_MSG)) {
            GameState.Doomspiral.status = GameState.Doomspiral.STATUS_CAPTURED;
            GameState.CritterSafari.markCaptured(ModConstants.DOOMSPIRAL_NAME);
            if (ModConfig.INSTANCE.foraging.enableDoomspiralCapsuleMessage) {
                if (ModConfig.INSTANCE.foraging.enableDoomspiralCapsuleMessage) {
                announceCapsuleUsage(client, "§5§lDoomspiral", GameState.Doomspiral.capsuleHits);
            }
            }
            return true;
        }

        // 戦闘に負けると地中へ戻り、この周回ではもうキャプチャできない
        if (ModConstants.containsIgnoreCase(unformattedMsg, ModConstants.DOOMSPIRAL_DESPAWN_MSG)) {
            GameState.Doomspiral.status = GameState.Doomspiral.STATUS_DESPAWNED;
            return true;
        }

        return false;
    }

    // Critter Capsule を当てるたびに1回分数える。20回以内に必ずキャプチャできるので、
    // ネームプレートに残り回数の目安として出す
    private static boolean handleCapsuleThrow(String unformattedMsg) {
        Matcher matcher = ModConstants.CAPSULE_THROW_PATTERN.matcher(unformattedMsg);
        if (!matcher.find()) return false;

        String target = matcher.group(1).trim();
        if (ModConstants.WUMPA_NAME.equalsIgnoreCase(target)) {
            GameState.CritterSafari.wumpaCapsuleHits++;
        } else if (ModConstants.DOOMSPIRAL_NAME.equalsIgnoreCase(target)) {
            GameState.Doomspiral.capsuleHits++;
        }
        return true;
    }

    // キャプチャに何個 Critter Capsule を使ったかをチャットに残す。
    // 投擲のメッセージは戦闘中に流れて埋もれるため、決着した時点で改めて出す
    private static void announceCapsuleUsage(MinecraftClient client, String coloredName, int hits) {
        String unit = hits == 1 ? "Critter Capsule" : "Critter Capsules";
        MutableText message = Text.literal(
                coloredName + " §fcaptured! Used §e" + hits + " §f" + unit + ".");
        client.execute(() -> NotificationUtils.sendSystemChat(client, message));
    }
}
