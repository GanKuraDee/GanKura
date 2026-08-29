package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.ScoreboardUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.FishingHook;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Golden Fish の湧き待ちを追う。
 *
 * 溶岩に竿を投げた時点から数え始め、8分で湧き得るようになり、12分で必ず湧く。
 * 竿を投げないまま3分経つと数えが消えるので、その前に知らせる。
 *
 * 湧いた後は3回やり取りすると弱り、釣り上げられるようになる。
 * やり取りのたびに1分の消失までの時間が延びる。
 */
public class GoldenFishHandler {

    // 投げてから湧き得るようになるまでと、必ず湧くまでの時間(ミリ秒)。
    // どちらも Goldfin Shard のレベル1つにつき30秒短くなる
    private static final long BASE_MIN_SPAWN_MS = 8 * 60 * 1000L;
    private static final long BASE_MAX_SPAWN_MS = 12 * 60 * 1000L;
    public static final long SHARD_BONUS_MS = 30 * 1000L;
    // 竿を投げないままこの時間が経つと、数えが消える(ミリ秒)
    private static final long ROD_TIMEOUT_MS = 3 * 60 * 1000L;
    // 湧いた後、潜ってしまうまでの時間(ミリ秒)
    private static final long DESPAWN_MS = 60 * 1000L;
    // 釣り上げられるようになるまでのやり取り回数
    public static final int MAX_INTERACTIONS = 3;

    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 30;
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.4f;

    // 溶岩用の竿の見分け方。名前の下に灰色でこの行が入る
    private static final String LAVA_ROD_LORE = "Lava Rod";
    // 持ち物を見直す間隔(tick)。毎 tick 見る必要はない
    private static final int ROD_CHECK_INTERVAL_TICKS = 20;

    private static final String SPAWN = "You spot a Golden Fish surface from beneath the lava!";
    private static final String INTERACT = "The Golden Fish escapes your hook but looks weakened.";
    private static final String WEAK = "The Golden Fish is weak!";
    private static final String DESPAWN = "The Golden Fish swims back beneath the lava...";
    private static final String TROPHY = "TROPHY FISH!";
    private static final String GOLDEN_FISH = "Golden Fish";

    // 数え始めた時刻と、最後に竿を投げた時刻。0 なら未設定
    private static long spawnBaseMillis;
    private static long lastRodMillis;
    // 湧いている間の、潜るまでの期限。0 なら湧いていない
    private static long despawnMillis;
    private static int interactions;
    // 竿を投げ直せと知らせたか。投げるまでは1度だけ
    private static boolean warnedRod;
    // 浮きが溶岩に入ったことを、その一投で既に数えたか
    private static boolean countedThisCast;

    // 溶岩用の竿を持っているかと、それを見直すまでの tick
    private static boolean hasLavaRod;
    private static int rodCheckTicks;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GoldenFishHandler::tick);
    }

    /** NetworkHandler のチャット振り分けから呼ばれる */
    public static void handleMessage(String unformattedMsg, Minecraft client) {
        if (!ModConfig.INSTANCE.fishing.showGoldenFishTimer) return;

        String message = unformattedMsg.trim();
        if (message.equals(SPAWN)) {
            despawnMillis = System.currentTimeMillis() + DESPAWN_MS;
            interactions = 0;
            title(client, "§6§lGolden Fish!");
            return;
        }
        if (message.equals(INTERACT)) {
            despawnMillis = System.currentTimeMillis() + DESPAWN_MS;
            interactions++;
            return;
        }
        if (message.equals(WEAK)) {
            despawnMillis = System.currentTimeMillis() + DESPAWN_MS;
            interactions = MAX_INTERACTIONS;
            title(client, "§a§lPull!");
            return;
        }
        // 潜ったか釣り上げたら、そこから数え直し
        if (message.equals(DESPAWN)
                || (message.contains(TROPHY) && message.contains(GOLDEN_FISH))) {
            despawnMillis = 0;
            interactions = 0;
            spawnBaseMillis = System.currentTimeMillis();
        }
    }

    /**
     * Golden Fish を狙える状態か。
     *
     * Crimson Isle で溶岩用の竿を持っているときだけ釣れるので、
     * それ以外ではタイマーを出さない
     */
    public static boolean isActive() {
        return ModConfig.INSTANCE.fishing.showGoldenFishTimer
                && GameState.Server.isCrimsonIsle() && hasLavaRod;
    }

    /** 湧いている間の、潜るまでの残り(ミリ秒)。0 なら湧いていない */
    public static long despawnRemaining() {
        if (despawnMillis == 0) return 0;
        return Math.max(0, despawnMillis - System.currentTimeMillis());
    }

    public static int interactions() {
        return interactions;
    }

    // Goldfin Shard のレベル分を引いた、湧き得るようになるまでの時間
    private static long minSpawnMillis() {
        return BASE_MIN_SPAWN_MS - SHARD_BONUS_MS * ModConfig.INSTANCE.fishing.goldfinShardLevel;
    }

    private static long maxSpawnMillis() {
        return BASE_MAX_SPAWN_MS - SHARD_BONUS_MS * ModConfig.INSTANCE.fishing.goldfinShardLevel;
    }

    /** 湧き得るようになるまでの残り(ミリ秒)。数えていなければ -1 */
    public static long spawnRemaining() {
        if (spawnBaseMillis == 0) return -1;
        return Math.max(0, spawnBaseMillis + minSpawnMillis() - System.currentTimeMillis());
    }

    /** 湧き得るようになってからの経過(ミリ秒) */
    public static long availableFor() {
        if (spawnBaseMillis == 0) return 0;
        return Math.max(0, System.currentTimeMillis() - (spawnBaseMillis + minSpawnMillis()));
    }

    /** 湧く確率の目安。0.0～1.0 */
    public static double chance() {
        return Math.min(1.0, (double) availableFor() / (maxSpawnMillis() - minSpawnMillis()));
    }

    /** 竿を投げ直すまでの残り(ミリ秒)。投げていなければ -1 */
    public static long rodRemaining() {
        if (lastRodMillis == 0) return -1;
        return Math.max(0, lastRodMillis + ROD_TIMEOUT_MS - System.currentTimeMillis());
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            reset();
            return;
        }
        if (++rodCheckTicks >= ROD_CHECK_INTERVAL_TICKS) {
            rodCheckTicks = 0;
            hasLavaRod = findLavaRod(client);
        }

        if (!isActive()) {
            reset();
            return;
        }

        countRodThrow(client);

        // 投げないまま時間切れになったら、数えは無効になる
        if (lastRodMillis != 0 && System.currentTimeMillis() - lastRodMillis > ROD_TIMEOUT_MS) {
            spawnBaseMillis = 0;
            lastRodMillis = 0;
        }

        warnRod(client);

        // 潜ってしまったら、数え直しに戻す
        if (despawnMillis != 0 && System.currentTimeMillis() >= despawnMillis) {
            despawnMillis = 0;
            interactions = 0;
            spawnBaseMillis = System.currentTimeMillis();
        }
    }

    // 浮きが溶岩に入ったときに、1投につき1度だけ数える
    private static void countRodThrow(Minecraft client) {
        FishingHook bobber = FishingBobberTracker.bobber(client);
        if (bobber == null) {
            countedThisCast = false;
            return;
        }
        if (countedThisCast || !bobber.isInLava()) return;

        countedThisCast = true;
        warnedRod = false;
        lastRodMillis = System.currentTimeMillis();
        if (spawnBaseMillis == 0) spawnBaseMillis = lastRodMillis;
    }

    // 数えが消える前に、投げ直すよう知らせる
    private static void warnRod(Minecraft client) {
        if (!ModConfig.INSTANCE.fishing.warnGoldenFishRod || warnedRod) return;

        long remaining = rodRemaining();
        if (remaining < 0 || remaining > ModConfig.INSTANCE.fishing.goldenFishRodWarningSeconds * 1000L) return;

        warnedRod = true;
        title(client, "§c§lThrow your rod!");
    }

    // 持ち物の中に溶岩用の竿があるか
    private static boolean findLavaRod(Minecraft client) {
        var inventory = client.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isLavaRod(inventory.getItem(i))) return true;
        }
        return false;
    }

    private static boolean isLavaRod(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        for (net.minecraft.network.chat.Component line : lore.lines()) {
            String text = ScoreboardUtils.stripColor(ScoreboardUtils.toLegacyString(line)).trim();
            if (LAVA_ROD_LORE.equals(text)) return true;
        }
        return false;
    }

    private static void title(Minecraft client, String text) {
        NotificationUtils.showTitle(client, Component.literal(text), null, TITLE_FADE, TITLE_STAY, TITLE_FADE);
        NotificationUtils.playSound(client, SoundEvents.EXPERIENCE_ORB_PICKUP, SOUND_VOLUME, SOUND_PITCH);
    }

    private static void reset() {
        spawnBaseMillis = 0;
        lastRodMillis = 0;
        despawnMillis = 0;
        interactions = 0;
        warnedRod = false;
        countedThisCast = false;
    }
}
