package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.SlotColorCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Garden の来客の知らせと、欲しがっている品を読む。
 *
 * 来客の画面は題が来客の名前になっていて画面としては見分けが付かないので、
 * "Accept Offer" の枠に書かれている説明を手掛かりにする。
 * <pre>
 * Accept Offer
 * Items Required:
 *   Enchanted Melon x4
 *
 * Rewards:
 *   +552 Farming XP
 *
 * Missing items to accept!
 * </pre>
 * 足りているかどうかは持ち物を数えずに、この最後の行から取る。
 * Hypixel が数えた結果なので、倉庫や器の中まで含めて合っている
 */
public final class VisitorHandler {

    // 品を渡す枠。来客の画面かどうかもこの名前で見分ける
    private static final String ACCEPT_OFFER = "Accept Offer";
    private static final String REQUIRED_HEADER = "Items Required:";
    // 足りないときに出る行。"Missing items!" と "Missing items to accept!" の2通りがある
    private static final String MISSING_PREFIX = "Missing items";

    // "Rusty has arrived on your Garden!" と知らせが来る
    private static final Pattern ARRIVAL = Pattern.compile("^(?<name>.+) has arrived on your Garden!$");
    private static final String ARRIVAL_SUBTITLE = "§eVisited!";

    // タブリストの Next Visitor の行。もう待たせられないときはここが埋まる
    // 待っている来客の数。"Visitors: (2)" と書かれている
    private static final String VISITORS_PREFIX = "Visitors:";
    private static final String NEXT_VISITOR_PREFIX = "Next Visitor:";
    // HUD に出す並び。次が来るまでが先、待っている人数が後
    private static final String[] STATUS_PREFIXES = {NEXT_VISITOR_PREFIX, VISITORS_PREFIX};
    private static final String QUEUE_FULL_LINE = "Next Visitor: Queue Full!";
    private static final String QUEUE_FULL_TITLE = "§cQueue Full!";
    private static final String QUEUE_FULL_SUBTITLE = "§eVisitors";
    // 耕す手を止めるほどの知らせではないので、音は低くして目立たせすぎない
    private static final float QUEUE_FULL_VOLUME = 1.0f;
    private static final float QUEUE_FULL_PITCH = 0.5f;

    // "Enchanted Melon x4"。1つだけのときは個数が省かれる
    private static final Pattern REQUIRED_LINE = Pattern.compile("^(?<name>.+?) [x×](?<count>[\\d,]+)$");

    // 耕している最中に出るので、じわりと出入りされるより、ぱっと出てぱっと消える方がよい
    private static final int TITLE_FADE = 0;
    private static final int TITLE_STAY = 70;

    // 濃さは設定で決まるので、ここでは色味だけを持つ
    private static final int READY_COLOR = 0x55FF55;
    private static final int MISSING_COLOR = 0xFF5555;

    private static final SlotColorCache COLORS = new SlotColorCache();

    /** 来客が欲しがっている品 1 つ分 */
    public record Required(String name, int count) {
    }

    /** 来客が求めているものと、それが揃っているか */
    public record Offer(List<Required> required, boolean missing) {
    }

    // 中身が変わるまでは、前に読んだものを使い回す
    private static AbstractContainerMenu cachedMenu;
    private static int cachedState;
    private static Offer cachedOffer;

    // 直前に見たときに枠が埋まっていたか。埋まった瞬間にだけ知らせる
    private static boolean queueFull = false;
    // タブリストに出ている来客まわりの行。HUD に並べるために控えておく
    private static List<String> statusLines = List.of();

    /** タブリストの来客まわりの行。まだ読めていなければ空 */
    public static List<String> statusLines() {
        return statusLines;
    }

    private VisitorHandler() {
    }

    /**
     * 来客の枠が埋まったことを画面の真ん中に出す。
     *
     * 埋まっている間は次が来ないので、耕す手を止めて捌きに行く合図になる。
     * タブリストは中身が変わったときだけ渡ってくるが、
     * 出入りのたびに出し直さないよう、埋まった瞬間かどうかはこちらでも見る
     */
    public static void processTabList(List<String> formattedLines, List<String> unformattedLines,
                                      Minecraft client) {
        if (!GameState.Server.isGarden()) {
            queueFull = false;
            statusLines = List.of();
            return;
        }

        boolean full = false;
        List<String> status = new ArrayList<>();

        // タブリストの行はこちらに届く順が決まっていないので、並べる順はこちらで決める
        for (String prefix : STATUS_PREFIXES) {
            for (int i = 0; i < unformattedLines.size(); i++) {
                String line = unformattedLines.get(i).trim();
                if (!line.startsWith(prefix)) continue;

                // HUD にはタブリストの見た目をそのまま出したいので、色の付いた方を控える
                status.add(formattedLines.get(i).trim());
                if (line.equals(QUEUE_FULL_LINE)) full = true;
                break;
            }
        }
        statusLines = List.copyOf(status);

        if (full && !queueFull && ModConfig.INSTANCE.farming.garden.showVisitorQueueFullTitle) {
            NotificationUtils.showTitle(client,
                    Component.literal(QUEUE_FULL_TITLE), Component.literal(QUEUE_FULL_SUBTITLE),
                    TITLE_FADE, TITLE_STAY, TITLE_FADE);
            NotificationUtils.playSound(client, SoundEvents.NOTE_BLOCK_PLING.value(),
                    QUEUE_FULL_VOLUME, QUEUE_FULL_PITCH);
        }
        queueFull = full;
    }

    /**
     * 来客が来たことを画面の真ん中に出す。
     *
     * 畑を耕している間はチャットが流れていくので、1行では気付けない
     */
    public static void handleMessage(Component message, Minecraft client) {
        if (!ModConfig.INSTANCE.farming.garden.showVisitorArrivalTitle) return;
        if (!GameState.Server.isGarden()) return;

        String plain = ChatFormatting.stripFormatting(message.getString());
        if (plain == null) return;

        Matcher arrival = ARRIVAL.matcher(plain.trim());
        if (!arrival.matches()) return;

        NotificationUtils.showTitle(client,
                coloured(message, arrival.group("name").trim().length()),
                Component.literal(ARRIVAL_SUBTITLE),
                TITLE_FADE, TITLE_STAY, TITLE_FADE);
    }

    /**
     * 知らせの頭から、見える字を length 文字ぶんだけ切り出す。
     *
     * 来客の名前はレアリティごとに色が違うので、その色を落とさずに題へ持っていく。
     * Hypixel の知らせは色コードの付いた文字列ではなく、色を持った部品の連なりで届くので、
     * 文字列にせず部品のまま切り出して、色をそのまま引き継ぐ
     */
    private static Component coloured(Component message, int length) {
        MutableComponent name = Component.empty();
        int[] taken = {0};

        message.visit((style, part) -> {
            int room = length - taken[0];
            if (room <= 0) return Optional.of(Boolean.TRUE);

            String piece = part.length() <= room ? part : part.substring(0, room);
            name.append(Component.literal(piece).withStyle(style));
            taken[0] += piece.length();
            return Optional.empty();
        }, Style.EMPTY);

        return name;
    }

    /**
     * 今開いている画面の来客の求め。来客の画面でなければ null。
     *
     * 品の並びは説明文にしか無いので、枠を一通り見て "Accept Offer" を探す
     */
    public static Offer offer(AbstractContainerMenu menu) {
        if (!GameState.Server.isGarden()) return null;

        int state = menu.getStateId();
        if (menu == cachedMenu && state == cachedState) return cachedOffer;

        cachedMenu = menu;
        cachedState = state;
        cachedOffer = read(menu);
        return cachedOffer;
    }

    private static Offer read(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            Offer offer = offer(slot.getItem());
            if (offer != null) return offer;
        }
        return null;
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        if (!ModConfig.INSTANCE.farming.garden.highlightVisitorOffer) return null;
        if (!GameState.Server.isGarden()) return null;

        return COLORS.get(slot, VisitorHandler::color);
    }

    private static Integer color(ItemStack stack) {
        Offer offer = offer(stack);
        if (offer == null) return null;

        return offer.missing() ? MISSING_COLOR : READY_COLOR;
    }

    /** その品が "Accept Offer" なら、書かれている求め。違えば null */
    private static Offer offer(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!ACCEPT_OFFER.equals(plain(stack.getHoverName()))) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        List<Required> required = new ArrayList<>();
        boolean missing = false;
        boolean listing = false;

        for (Component line : lore.lines()) {
            String text = plain(line);
            if (text == null) continue;

            if (text.startsWith(MISSING_PREFIX)) {
                missing = true;
                continue;
            }

            if (text.equals(REQUIRED_HEADER)) {
                listing = true;
                continue;
            }
            if (!listing) continue;

            // 品の並びは空行で終わり、その後ろに "Rewards:" が続く
            if (text.isEmpty() || text.endsWith(":")) {
                listing = false;
                continue;
            }

            required.add(required(text));
        }

        return required.isEmpty() ? null : new Offer(List.copyOf(required), missing);
    }

    private static Required required(String text) {
        Matcher line = REQUIRED_LINE.matcher(text);
        if (!line.matches()) return new Required(text, 1);

        return new Required(line.group("name").trim(), count(line.group("count")));
    }

    private static int count(String text) {
        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String plain(Component text) {
        String stripped = ChatFormatting.stripFormatting(text.getString());
        return stripped == null ? null : stripped.trim();
    }
}
