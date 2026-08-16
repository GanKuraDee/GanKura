package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.EntityHighlightManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 地面に落ちている採取物 (Floor Drop) の目印。
 * 見た目は minecraft:string を持つ ItemDisplay 3体が同じブロックに重なったもので、
 * ブロック自体は置かれていないため、湧いた瞬間に出る HAPPY_VILLAGER のパーティクルが唯一の手掛かりになる。
 *
 * Skyblocker の Floor Drops (skyblock/hunting/FloorDrops.java) を参考にした実装。
 * 本家は Galatea / Torrhus Canyon / Critter Safari で動くが、こちらは Critter Safari のみに絞っている。
 */
public class FloorDropHandler {

    // 1つの Floor Drop を成す ItemDisplay の数
    private static final int FLOOR_DROP_DISPLAY_COUNT = 3;
    // 目印を出したまま放置しないよう、この間隔でまだ在るか確かめ直す
    private static final long RECHECK_INTERVAL_MS = 2000L;
    // 最後に確認できてからこの時間が過ぎたら、採取されたものとして目印を消す
    private static final long EXPIRE_MS = 5000L;
    // パーティクルだけ届いて tick が回らない状況で溜まり続けないようにする上限
    private static final int MAX_PENDING = 256;
    // クリックで消した位置を覚えておく上限の時間。
    // 見た目がいつまでも消えない場合に、覚えたままにしないための保険
    private static final long DISMISS_MAX_MS = 30_000L;

    // パーティクルはネットワークスレッドで届く。ワールドに触ると壊れるので、
    // ここでは位置を控えるだけにして、実際の照合は tick 側で行う
    private static final Queue<BlockPos> pending = new ConcurrentLinkedQueue<>();
    // 見つけた Floor Drop の位置と、最後に存在を確認できた時刻。
    // tick スレッドと描画スレッドの両方から触るため並行マップにする
    private static final Map<BlockPos, Long> nodes = new ConcurrentHashMap<>();
    // クリックで消した位置と、その時刻。
    // 採取した直後も見た目はしばらく残るので、その間に登録し直さないために使う
    private static final Map<BlockPos, Long> dismissed = new ConcurrentHashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FloorDropHandler::tick);

        // 採取すると ItemDisplay も消えるが、目印が消えるまで最大で EXPIRE_MS かかる。
        // 手を出したブロックはその場で落としておくと、採取した手応えが目印にもすぐ出る
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            dismiss(pos);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            dismiss(hitResult.getBlockPos());
            return InteractionResult.PASS;
        });
    }

    // ネットワークスレッドから呼ばれる
    public static void onFloorDropParticle(double x, double y, double z) {
        if (pending.size() >= MAX_PENDING) return;
        // パーティクルは Floor Drop の1ブロック上に出るので、足下のブロックへ落とす
        pending.add(BlockPos.containing(x, y - 1, z));
    }

    public static void reset() {
        pending.clear();
        nodes.clear();
        dismissed.clear();
    }

    // 目印を降ろし、見た目が消えるまでは登録し直さないよう覚えておく。
    // これが無いと、採取したあとに飛んでくるパーティクルで目印が復活してしまう
    private static void dismiss(BlockPos pos) {
        if (nodes.remove(pos) == null) return;
        dismissed.put(pos.immutable(), System.currentTimeMillis());
    }

    // 目印を出してよい状況か
    public static boolean isActive() {
        return ModConfig.INSTANCE.foraging.enableFloorDrops && GameState.Server.isSafari();
    }

    // 描画側から参照する、目印を出す位置
    public static Set<BlockPos> positions() {
        return nodes.keySet();
    }

    private static void tick(Minecraft client) {
        if (!isActive() || client.level == null || client.player == null) {
            if (!pending.isEmpty() || !nodes.isEmpty()) reset();
            return;
        }

        long now = System.currentTimeMillis();
        // Critter Safari の Mob Visuals と同じく、今いるバイオームの分だけを扱う。
        // 中心付近では隣のバイオームの Floor Drop まで見えてしまうため
        double originX = client.player.getX();
        double originZ = client.player.getZ();

        // パーティクルは Floor Drop 以外でも飛んでくるので、
        // 実際に ItemDisplay が揃っている場所だけを登録する
        BlockPos pos;
        while ((pos = pending.poll()) != null) {
            if (nodes.containsKey(pos)) continue;
            // クリックで消したものは、見た目が消えるまで登録し直さない
            if (dismissed.containsKey(pos)) continue;
            if (!EntityHighlightManager.inSameSafariBiome(pos, originX, originZ)) continue;
            if (isFloorDrop(client, pos)) nodes.put(pos, now);
        }

        // 採取されて消えたものを落とす。毎tick数え直すのは無駄なので、確認は RECHECK_INTERVAL_MS ごとに行う
        for (Map.Entry<BlockPos, Long> entry : nodes.entrySet()) {
            // バイオームをまたいで移動したら、前のバイオームの分は目印を降ろす
            if (!EntityHighlightManager.inSameSafariBiome(entry.getKey(), originX, originZ)) {
                nodes.remove(entry.getKey());
                continue;
            }

            long lastConfirmed = entry.getValue();
            if (lastConfirmed + RECHECK_INTERVAL_MS > now) continue;

            if (isFloorDrop(client, entry.getKey())) {
                entry.setValue(now);
            } else if (lastConfirmed + EXPIRE_MS <= now) {
                nodes.remove(entry.getKey());
            }
        }

        // 見た目が実際に消えたら、覚えているのをやめる。
        // 同じ場所に改めて湧いたものは、また目印を出したいため
        for (Map.Entry<BlockPos, Long> entry : dismissed.entrySet()) {
            if (entry.getValue() + DISMISS_MAX_MS <= now || !isFloorDrop(client, entry.getKey())) {
                dismissed.remove(entry.getKey());
            }
        }
    }

    // そのブロックに Floor Drop の見た目が揃っているか
    private static boolean isFloorDrop(Minecraft client, BlockPos pos) {
        if (client.level == null) return false;

        AABB box = AABB.ofSize(Vec3.atCenterOf(pos), 1.0, 1.0, 1.0);
        int strings = 0;
        for (Display.ItemDisplay display : client.level.getEntitiesOfClass(Display.ItemDisplay.class, box)) {
            Display.ItemDisplay.ItemRenderState state = display.itemRenderState();
            if (state == null) continue;

            ItemStack stack = state.itemStack();
            if (!stack.isEmpty() && stack.getItem() == Items.STRING) strings++;
        }
        return strings == FLOOR_DROP_DISPLAY_COUNT;
    }
}
