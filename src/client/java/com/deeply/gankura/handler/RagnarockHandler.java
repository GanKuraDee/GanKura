package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ragnarock Axe の能力の詠唱が終わったか、途中で止められたかをタイトルで知らせる。
 *
 * 詠唱は3秒の間ダメージを受けなければ終わる。終わった合図はチャットに出ず、
 * 狼の死ぬ音が決まった高さで鳴るだけなので、その音で見分ける。
 * 終わる直前に持ち替えても、サーバーの遅れで詠唱が通ることがあるので、手元は少し前まで遡って見る。
 * 増える Strength は武器の Strength の 1.5 倍で、サブタイトルに出す。
 * 止められたときはチャットに知らせが出る。
 * HUD の残り時間は、手元で右クリックした時刻から数える。サーバーの知らせは遅れて届くことがあるので、
 * アクションバーの "CASTING IN 3s" は、詠唱が本当に始まったかを確かめるのにだけ使う。
 * 詠唱の3秒はサーバーの 60 Tick なので、ほかのカウントダウンと同じく、測った TPS で進めるサーバー Tick の見積もりで数える
 */
public final class RagnarockHandler {

    private static final String ITEM_ID = "RAGNAROCK_AXE";
    // 詠唱が終わったときに鳴る音の高さ。ほかの場面で狼の音がこの高さで鳴ることはまず無い
    private static final float CAST_PITCH = 1.4920635f;
    private static final float PITCH_EPSILON = 0.0001f;
    // "Ragnarock was cancelled due to taking damage!"。理由の部分は場面によって変わるので問わない
    private static final Pattern CANCEL_PATTERN =
            Pattern.compile("^Ragnarock was cancelled due to .+!$");
    // 止められたときは、チャットとアクションバーの "CANCELLED" の両方で知らされることがある。
    // 片方だけのこともあるので両方見て、この間に重なった分は同じ1回として扱う
    private static final long CANCEL_DEDUPE_MS = 3000;

    // 手から離してからも、この間に鳴った完了の音は自分の詠唱として扱う。
    // サーバーは遅れて持ち替えを知るので、詠唱が終わる直前に持ち替えると、
    // 画面では持っていないのにサーバー側では持ったまま詠唱が終わることがある
    private static final long HELD_GRACE_MS = 1000;

    // 詠唱が終わると、この武器の Strength のこの倍だけ Strength が増える
    private static final double STRENGTH_MULTIPLIER = 1.5;
    // Hypixel の Strength のアイコン。私用領域の文字で、Hypixel のリソースパックがこの文字に絵を当てている。
    // 昔の説明文にあった "❁" とは別の文字なので、そちらを使うと見た目が違ってしまう
    private static final String STRENGTH_ICON = "";
    // 説明文の "Strength: +200 (+20)" のような行。最初の数が、宝石や Reforge 込みの合計
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("^Strength: \\+([\\d,]+(?:\\.\\d+)?)");

    // 詠唱にかかる時間 (サーバー Tick)。サーバーの Tick で数えるので、TPS が落ちるとそのぶん実時間では長くなる
    private static final int CAST_TICKS = 60;
    // 1秒ごとの音の間隔 (サーバー Tick)
    private static final int TICKS_PER_SECOND = 20;
    // 詠唱が終わった知らせも止められた知らせも来ないまま、これだけ経ったら詠唱は無かったことにする (サーバー Tick)
    private static final int CAST_TIMEOUT_TICKS = 100;
    // 右クリックしてからこの間に詠唱が始まったと分からなければ、始まらなかったことにする。
    // マナが足りないときなどは、右クリックしても詠唱は始まらない。
    // 待っているのは知らせが届く遅れなので、TPS ではなく実時間で数える
    private static final long CONFIRM_TIMEOUT_MS = 2000;
    // 詠唱中はアクションバーに "CASTING IN 3s" のように残りの秒数が出る。終わると "CASTING" だけになる
    private static final Pattern CASTING_IN_PATTERN = Pattern.compile("CASTING IN (\\d+)s");
    // クールダウン中に右クリックしたときのチャット
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^This ability is on cooldown for (\\d+)s\\.$");
    // 詠唱中は1秒ごとに、レバーを倒す音が高い音で鳴る。残り3秒・2秒・1秒の3回
    private static final String TICK_SOUND = "block.lever.click";
    private static final float TICK_PITCH = 2.0f;
    private static final int TICKS_PER_CAST = 3;

    // 最後に Ragnarock を手に持っていた時刻。一度も持っていなければ 0
    private static long lastHeldAt = 0;
    // そのとき持っていた Ragnarock の Strength。持ち替えた後に詠唱が通ったときに使う
    private static double lastHeldStrength = 0;
    // 詠唱中か。右クリックしてから、終わるか止められるか諦めるまで
    private static boolean casting = false;
    // 右クリックした実時刻。始まったと分かるまで待つ期限に使う
    private static long clickedAt = 0;
    // 詠唱を始めたサーバー Tick。サーバーの知らせは遅れて届くことがあるので、
    // 手元で右クリックした時点の見積もりから始め、1秒ごとの音が届くたびに合わせ直す
    private static double castStartTick = 0;
    // 詠唱が始まったことを、1秒ごとの音かアクションバーで確かめられたか
    private static boolean castConfirmed = false;
    // 最後に詠唱を始めようと右クリックしたサーバー Tick。確かめる前に諦めた後で、遅れて知らせが来たときに使う。
    // まだ右クリックしていなければ負
    private static double lastClickTick = -1;
    // クールダウンが明ける時刻。それまでの右クリックでは詠唱は始まらない
    private static long cooldownUntil = 0;
    // この詠唱で、1秒ごとの音を何回聞いたか
    private static int ticksHeard = 0;
    // 最後に止められたと知らされた時刻。同じ1回をチャットとアクションバーで2度数えないために使う
    private static long lastCancelAt = 0;

    private RagnarockHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(RagnarockHandler::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandItem();
        if (!ITEM_ID.equals(SkyblockItemId.of(held))) return;
        lastHeldAt = System.currentTimeMillis();
        lastHeldStrength = strengthOf(held);
    }

    /**
     * 詠唱が終わるまでの残り秒数。詠唱中でなければ null。終わる時刻を過ぎても、知らせが来るまでは 0 を返す。
     * 右クリックしただけで、まだ詠唱が始まったと分かっていないときも null
     */
    public static Double secondsLeft() {
        if (!casting) return null;

        double elapsedTicks = serverTicks() - castStartTick;
        if ((!castConfirmed && System.currentTimeMillis() - clickedAt > CONFIRM_TIMEOUT_MS)
                || elapsedTicks > CAST_TIMEOUT_TICKS) {
            casting = false;
            return null;
        }
        // クールダウン中やマナが足りないときは、右クリックしても詠唱は始まらない。
        // 1秒ごとの音かアクションバーで始まったと分かるまでは出さない
        if (!castConfirmed) return null;
        // ほかのカウントダウンと同じく、サーバーの Tick を 20 で割った秒数で出す。
        // TPS が落ちればそのぶんゆっくり減り、サーバーで詠唱が終わる瞬間に 0 になる
        return Math.max(0, CAST_TICKS - elapsedTicks) / TICKS_PER_SECOND;
    }

    /** 右クリックで使う操作をしたときに呼ばれる。Ragnarock を持っていれば、その時点から数え始める */
    public static void onUseItem(Minecraft client) {
        if (client.player == null || !GameState.Server.isSkyblock()) return;
        if (!ITEM_ID.equals(SkyblockItemId.of(client.player.getMainHandItem()))) return;

        // 押しっぱなしだと何度も呼ばれるので、詠唱中やクールダウン中は数え直さない
        long now = System.currentTimeMillis();
        if (casting || now < cooldownUntil) return;

        casting = true;
        clickedAt = now;
        castStartTick = serverTicks();
        castConfirmed = false;
        lastClickTick = castStartTick;
        ticksHeard = 0;
    }

    /**
     * 詠唱中に1秒ごとに鳴る音を受け取ったら、残り時間をそれに合わせ直す。
     * 1回目が残り3秒、2回目が残り2秒、3回目が残り1秒。音は詠唱の完了の音と同じ遅れで届くので、
     * これに合わせておけば、HUD が 0 になるのと完了の知らせが届くのがそろう
     */
    private static void onCastTick() {
        double now = serverTicks();
        if (!casting) {
            // 確かめる前に諦めていたところへ遅れて届いた。右クリックからの Tick 数で、何回目の音かを見積もる
            if (lastClickTick < 0 || now - lastClickTick > CAST_TIMEOUT_TICKS) return;
            ticksHeard = (int) Math.min(TICKS_PER_CAST - 1, Math.round((now - lastClickTick) / TICKS_PER_SECOND));
        }

        int index = Math.min(ticksHeard, TICKS_PER_CAST - 1);
        casting = true;
        castStartTick = now - index * TICKS_PER_SECOND;
        castConfirmed = true;
        ticksHeard = index + 1;
    }

    /**
     * いまのサーバー Tick の見積もり。ほかのカウントダウンと同じく、時刻パケットから測った TPS で進める。
     * まだ一度も時刻パケットが来ていなければ、手元の時計で代用する
     */
    private static double serverTicks() {
        if (GameState.Server.lastPacketArrivalMillis != 0) return GameState.Server.estimatedTicks();
        Minecraft client = Minecraft.getInstance();
        return client.level != null ? client.level.getGameTime() : 0;
    }

    /** アクションバーを受け取るたびに呼ばれる。詠唱の始まりと終わりを見る */
    public static void handleActionBar(Component message) {
        String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) return;

        Matcher matcher = CASTING_IN_PATTERN.matcher(text);
        if (matcher.find()) {
            castConfirmed = true;
            if (casting) return;

            // 知らせが遅れて、確かめる前に諦めていたときは、直前の右クリックから数え直す。
            // 右クリックを見ていなければ、残りの秒数から始めた Tick を割り出す
            double now = serverTicks();
            casting = true;
            if (lastClickTick >= 0 && now - lastClickTick <= CAST_TICKS) {
                castStartTick = lastClickTick;
            } else {
                long secondsLeft = Long.parseLong(matcher.group(1));
                castStartTick = now - Math.max(0, CAST_TICKS - secondsLeft * TICKS_PER_SECOND);
            }
            return;
        }

        // 止められた。持ち替えて止められたときなど、チャットに知らせが出ないことがある。
        // ほかの能力の表示と取り違えないよう、詠唱中か Ragnarock を持っているときだけ拾う
        if (text.contains("CANCELLED")) {
            if (casting || heldRecently()) onCancelled(Minecraft.getInstance());
            return;
        }

        // 詠唱が終わった
        if (text.contains("CASTING")) casting = false;
    }

    /** メインスレッドで、サーバーが鳴らす音を受け取るたびに呼ばれる */
    public static void onSound(ClientboundSoundPacket packet) {
        if (!GameState.Server.isSkyblock()) return;

        // 詠唱中の1秒ごとの音。詠唱は持ったままでないと続かないので、Ragnarock を持っているときだけ拾う
        if (packet.getSound().value().location().getPath().equals(TICK_SOUND)
                && Math.abs(packet.getPitch() - TICK_PITCH) <= PITCH_EPSILON) {
            if (heldRecently()) onCastTick();
            return;
        }

        if (Math.abs(packet.getPitch() - CAST_PITCH) > PITCH_EPSILON) return;
        if (!isWolfDeath(packet.getSound().value().location())) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 今持っているならその Strength、持ち替えて間もないなら持っていたときの Strength を使う
        double strength;
        ItemStack held = client.player.getMainHandItem();
        if (ITEM_ID.equals(SkyblockItemId.of(held))) {
            strength = strengthOf(held);
        } else if (lastHeldAt != 0 && System.currentTimeMillis() - lastHeldAt <= HELD_GRACE_MS) {
            strength = lastHeldStrength;
        } else {
            return;
        }

        casting = false;

        // 説明文から Strength を読めなかったときは、増えた量は出さない
        String gained = strength <= 0 ? null : "+" + format(strength * STRENGTH_MULTIPLIER) + " " + STRENGTH_ICON + " Strength";

        if (ModConfig.Combat.enableRagnarockCastTitle) {
            MutableComponent title = Component.literal("RAGNAROCK CAST!")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            MutableComponent subtitle = gained == null ? null : Component.literal(gained).withStyle(ChatFormatting.RED);
            NotificationUtils.showTitle(client, title, subtitle, 0, 30, 10);
        }

        // タイトルはすぐ消えるので、増えた量を後から見返せるようチャットにも残す
        if (ModConfig.Combat.showRagnarockStrengthChat && gained != null) {
            NotificationUtils.sendSystemChat(client, Component.literal("§aRagnarock Cast §c(" + gained + ")"));
        }
    }

    public static void handleMessage(String unformattedMsg, Minecraft client) {
        String text = unformattedMsg.trim();

        // クールダウン中の右クリック。詠唱は始まっていないので、数え始めていたら取り消す
        Matcher cooldown = COOLDOWN_PATTERN.matcher(text);
        if (cooldown.matches()) {
            if (!castConfirmed) casting = false;
            cooldownUntil = System.currentTimeMillis() + Long.parseLong(cooldown.group(1)) * 1000;
            return;
        }

        if (CANCEL_PATTERN.matcher(text).matches()) onCancelled(client);
    }

    // 詠唱が止められた。ダメージでも持ち替えでも、ここで1回だけ知らせる
    private static void onCancelled(Minecraft client) {
        casting = false;

        // アクションバーの "CANCELLED" はしばらく出続け、チャットと重なることもある
        long now = System.currentTimeMillis();
        if (now - lastCancelAt <= CANCEL_DEDUPE_MS) return;
        lastCancelAt = now;

        if (!ModConfig.Combat.enableRagnarockCancelTitle) return;

        MutableComponent title = Component.literal("RAGNAROCK CANCELLED!")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        client.execute(() -> NotificationUtils.showTitle(client, title, null, 0, 30, 10));
    }

    // 今持っているか、持ち替えてからまだ間もないか
    private static boolean heldRecently() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && ITEM_ID.equals(SkyblockItemId.of(client.player.getMainHandItem()))) return true;
        return lastHeldAt != 0 && System.currentTimeMillis() - lastHeldAt <= HELD_GRACE_MS;
    }

    // 説明文に書かれた武器の Strength。読めなければ 0
    private static double strengthOf(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return 0;

        for (Component line : lore.lines()) {
            String text = ChatFormatting.stripFormatting(line.getString());
            if (text == null) continue;
            Matcher matcher = STRENGTH_PATTERN.matcher(text.trim());
            if (matcher.find()) return Double.parseDouble(matcher.group(1).replace(",", ""));
        }
        return 0;
    }

    // 整数ならそのまま、端数があれば小数第1位まで出す
    private static String format(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    // 狼の鳴き声は種類ごとに分かれている ("entity.wolf.death", "entity.wolf_angry.death" など)。
    // どの種類で来ても拾えるよう、名前の形で見る
    private static boolean isWolfDeath(Identifier sound) {
        String path = sound.getPath();
        return path.startsWith("entity.wolf") && path.endsWith(".death");
    }
}
