package com.deeply.gankura.handler;

import com.deeply.gankura.data.BazaarNames;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.SkyblockItemId;
import com.deeply.gankura.util.SlotColorCache;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bazaar に出している自分の注文の様子を、枠の色で知らせる。
 *
 *   緑 … 約定済み。受け取りに行ける
 *   黄 … 誰かに一番手を取られている。このままでは進まない
 *   なし … まだ自分が一番手
 */
public final class BazaarOrderHandler {

    // 注文の一覧を出している画面。共同農園の有無で題が変わる
    private static final String ORDERS_TITLE = "Your Bazaar Orders";
    private static final String COOP_ORDERS_TITLE = "Co-op Bazaar Orders";

    // 品の名前は "BUY Wheat" "SELL Wheat" の形で書かれている
    private static final Pattern ORDER_NAME = Pattern.compile("^(BUY|SELL) (.+)$");
    private static final Pattern PRICE_PER_UNIT = Pattern.compile("Price per unit: ([\\d,.]+) coins");
    // 約定の進み具合。100%!(感嘆符付き)で書かれたものだけが約定済み
    private static final Pattern FILLED = Pattern.compile("Filled: \\S+/\\S+ (\\S+)");
    private static final String FILLED_MARK = "100%!";

    // 濃さは設定で決まるので、ここでは色味だけを持つ
    private static final int FILLED_COLOR = 0x55FF55;
    private static final int OUTBID_COLOR = 0xFFFF55;

    // 値段は小数第1位まで書かれる。丸めの差で一番手を取られたと見なさないための余裕
    private static final double PRICE_EPSILON = 0.05;

    private static final SlotColorCache COLORS = new SlotColorCache();

    private BazaarOrderHandler() {
    }

    /** 注文の一覧を出している画面か */
    public static boolean inOrderMenu(String title) {
        return title.equals(ORDERS_TITLE) || title.equals(COOP_ORDERS_TITLE);
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        return COLORS.get(slot, BazaarOrderHandler::compute);
    }

    private static Integer compute(ItemStack stack) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableAuctionTweaks || !config.highlightBazaarOrders) return null;
        if (!GameState.Server.isSkyblock() || stack.isEmpty()) return null;

        Matcher name = ORDER_NAME.matcher(stack.getHoverName().getString().trim());
        if (!name.matches()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        Double price = null;
        for (Component line : lore.lines()) {
            String text = line.getString();

            Matcher filled = FILLED.matcher(text);
            if (filled.find()) {
                if (filled.group(1).equals(FILLED_MARK)) return FILLED_COLOR;
                continue;
            }

            Matcher unit = PRICE_PER_UNIT.matcher(text);
            if (unit.find()) price = parse(unit.group(1));
        }
        if (price == null) return null;

        // 値段を照らし合わせるので、古いままにしない
        ItemPrices.refreshIfStale();

        ItemPrices.Bazaar market = market(stack, name.group(2).trim());
        if (market == null) return null;

        return outbid(name.group(1).equals("BUY"), price, market) ? OUTBID_COLOR : null;
    }

    /**
     * 買い注文は、自分より高い注文が出ていれば先に約定してしまう。
     * 売り注文は、自分より安い品が出ていれば先に売れてしまう
     */
    private static boolean outbid(boolean buying, double price, ItemPrices.Bazaar market) {
        if (buying) {
            return market.highestOrder() > 0 && price < market.highestOrder() - PRICE_EPSILON;
        }
        return market.lowestOffer() > 0 && price > market.lowestOffer() + PRICE_EPSILON;
    }

    /**
     * その品の今の相場。
     *
     * 品に ID が書かれていればそれで引く。
     * 書かれていない場合に備えて、表示名からも引けるようにしてある
     */
    private static ItemPrices.Bazaar market(ItemStack stack, String displayName) {
        String itemId = SkyblockItemId.of(stack);
        if (itemId != null) {
            ItemPrices.Bazaar market = ItemPrices.bazaar(itemId);
            if (market != null) return market;
        }

        String byName = BazaarNames.idOf(displayName);
        return byName == null ? null : ItemPrices.bazaar(byName);
    }

    private static Double parse(String number) {
        try {
            return Double.parseDouble(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
