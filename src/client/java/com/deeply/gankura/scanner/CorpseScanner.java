package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mineshaft の中に埋まっている Corpse を探す。
 *
 * Corpse は装備を着せたアーマースタンドとして置かれていて、
 * 種類は被っている兜で分かる。名前も付いておらず、姿も見えているので、
 * その2つを手掛かりに飾りのアーマースタンドと選り分ける。
 *
 * 見分け方と兜の対応は Skyblocker(LGPL-3.0) のものを使わせてもらっている
 */
public class CorpseScanner {

    /** 見つけた Corpse 1体分。名前と色は種類ごとに決まっている */
    public record Corpse(String name, int argb, BlockPos pos) {
    }

    /**
     * Corpse の種類と、それが被っている兜。
     *
     * 色は開けるのに要る鍵の色味に合わせてある
     */
    private enum Kind {
        LAPIS("Lapis", 0xFF5555FF, "LAPIS_ARMOR_HELMET"),
        UMBER("Umber", 0xFFFFAA00, "ARMOR_OF_YOG_HELMET"),
        TUNGSTEN("Tungsten", 0xFFAAAAAA, "MINERAL_HELMET"),
        VANGUARD("Vanguard", 0xFF55FFFF, "VANGUARD_HELMET");

        private final String label;
        private final int argb;
        private final String helmetId;

        Kind(String label, int argb, String helmetId) {
            this.label = label;
            this.argb = argb;
            this.helmetId = helmetId;
        }

        private static Kind byHelmet(String helmetId) {
            for (Kind kind : values()) {
                if (kind.helmetId.equals(helmetId)) return kind;
            }
            return null;
        }

        private static boolean isKnownLabel(String label) {
            for (Kind kind : values()) {
                if (kind.label.equalsIgnoreCase(label)) return true;
            }
            return false;
        }
    }

    // 走査の間隔。Corpse は動かないので、毎tick探し直す意味はない
    private static final int SCAN_INTERVAL_TICKS = 10;

    // 中身を取ったときに出る知らせ。"☠ LAPIS CORPSE LOOT! ☠" のように種類が入る
    private static final Pattern LOOT_MESSAGE = Pattern.compile("([A-Z]+) CORPSE LOOT!");

    // 知らせには場所が入っていないので、手の届く範囲で一番近いものを開けたものと見なす
    private static final double LOOT_RANGE = 8.0;

    // 描画スレッドからも読むため、丸ごと差し替える形で更新する
    private static volatile List<Corpse> corpses = List.of();

    // 開け終わったものの置き場。走査し直しても目印が戻ってこないようにする
    private static final Set<BlockPos> lootedPositions = new HashSet<>();

    // Vanguard の知らせを出したかどうか。シャフトを出るまで持ち越す
    private static boolean vanguardAnnounced = false;
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CorpseScanner::scan);
    }

    /** 描画側から参照する、目印を出す位置 */
    public static List<Corpse> corpses() {
        return corpses;
    }

    // 目印と知らせのどちらか一方でも使うなら探す。
    // 目印を切っているだけで Vanguard の知らせまで止まるのは分かりにくい
    private static boolean isEnabled() {
        if (!GameState.Server.isMineshaft()) return false;
        return ModConfig.INSTANCE.mining.showCorpseWaypoints
                || ModConfig.INSTANCE.mining.showVanguardTitle;
    }

    private static void scan(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            // Mineshaft を出たら、前のシャフトで見つけたものは当てにならない
            if (!corpses.isEmpty()) corpses = List.of();
            lootedPositions.clear();
            vanguardAnnounced = false;
            return;
        }

        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        List<Corpse> found = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;

            Corpse corpse = corpse(stand);
            if (corpse == null || lootedPositions.contains(corpse.pos())) continue;
            found.add(corpse);
        }

        corpses = List.copyOf(found);
        announceVanguard(client, found);
    }

    /**
     * Vanguard の Corpse があるシャフトに当たったことを、1つのシャフトにつき一度だけ知らせる。
     *
     * Corpse はエンティティが届く範囲に入って初めて見つかるので、
     * 入り口に立った瞬間ではなく、そこそこ近くまで進んだところで出る
     */
    private static void announceVanguard(Minecraft client, List<Corpse> found) {
        if (!ModConfig.INSTANCE.mining.showVanguardTitle || vanguardAnnounced) return;

        boolean hasVanguard = found.stream().anyMatch(corpse -> Kind.VANGUARD.label.equals(corpse.name()));
        if (!hasVanguard) return;

        vanguardAnnounced = true;
        NotificationUtils.showTitle(client,
                Component.literal("VANGUARD CORPSE").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                null);
        // 試練のスポナーが中身を吐き出す音(block.trial_spawner.eject_item)。
        // この MOD の他の知らせでは使っていないので、何の合図か取り違えない
        NotificationUtils.playSound(client, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.0f);
    }

    /**
     * 中身を取り終えた知らせを受けて、その Corpse の目印を消す。
     *
     * 知らせにはどれを開けたのかが入っていないので、
     * 種類が同じもののうち手の届く範囲で一番近いものを開けたものと見なす
     */
    public static void handleMessage(String unformattedMessage, Minecraft client) {
        if (corpses.isEmpty() || client.player == null) return;

        Matcher matcher = LOOT_MESSAGE.matcher(unformattedMessage);
        if (!matcher.find()) return;

        Corpse opened = nearest(client.player.position(), matcher.group(1));
        if (opened == null) return;

        lootedPositions.add(opened.pos());
        // 次の走査を待たずに消す。0.5秒ほど目印が残るのは目に付く
        corpses = corpses.stream().filter(corpse -> !corpse.pos().equals(opened.pos())).toList();
    }

    /** 手の届く範囲で一番近い Corpse。見当たらなければ null */
    private static Corpse nearest(Vec3 eye, String kindName) {
        // 知らない種類名だったときは種類での絞り込みをあきらめ、近さだけで選ぶ
        boolean matchKind = Kind.isKnownLabel(kindName);

        Corpse best = null;
        double bestDistanceSqr = LOOT_RANGE * LOOT_RANGE;
        for (Corpse corpse : corpses) {
            if (matchKind && !corpse.name().equalsIgnoreCase(kindName)) continue;

            double distanceSqr = eye.distanceToSqr(Vec3.atCenterOf(corpse.pos()));
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                best = corpse;
            }
        }
        return best;
    }

    /**
     * そのアーマースタンドが Corpse なら、その1体分。違えば null。
     *
     * 名前を出しているもの・姿の見えないもの・台座を出しているものは、
     * 文字を出すための飾りや別の用途のものなので外す
     */
    private static Corpse corpse(ArmorStand stand) {
        if (stand.hasCustomName() || stand.isInvisible() || stand.showBasePlate()) return null;

        String helmetId = SkyblockItemId.of(stand.getItemBySlot(EquipmentSlot.HEAD));
        if (helmetId == null) return null;

        Kind kind = Kind.byHelmet(helmetId);
        if (kind == null) return null;

        // 足元ではなく体のあたりに出したいので、1つ上を目印にする
        return new Corpse(kind.label, kind.argb, stand.blockPosition().above());
    }
}
