package com.deeply.gankura.handler;

import com.deeply.gankura.data.BazaarPriceType;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ItemRecipes;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.CoinText;
import com.deeply.gankura.util.PetBin;
import com.deeply.gankura.util.SkyblockItemId;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ツールチップの一番下に、Auction House の最安 BIN と Bazaar の値段、
 * それに材料から作った場合の値段を出す。
 *
 * 値段は {@link ItemPrices} が、作り方は {@link ItemRecipes} が裏で取ってくる。
 * まだ届いていない品には何も足さないので、行が増えたり減ったりするだけで済む
 */
public final class ItemPriceTooltipHandler {

    // 値段の行の書式。説明文の中で目が留まるよう太字にし、値段そのものは金色にする
    private static final String LABEL_STYLE = "\u00a77\u00a7l";
    private static final String COIN_STYLE = "\u00a76\u00a7l";

    private ItemPriceTooltipHandler() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
            // Tooltips の Enable が親。ここが切れていれば中の設定は見ない
            if (!config.enableItemTooltipTweaks || !config.enableItemPrice) return;
            if (!config.showLowestBin && !config.showBazaarPrice && !config.showCraftCost) return;
            if (!GameState.Server.isSkyblock()) return;

            // 何か持ち上げた時点で取りに行く。次に見たときには間に合っている
            ItemPrices.refreshIfStale();

            String itemId = SkyblockItemId.of(stack);
            if (itemId == null) return;

            SkyblockItemId.Pet pet = SkyblockItemId.pet(stack);
            List<Component> priceLines = pet == null
                    ? build(config, itemId, stack.getCount())
                    : buildPet(config, pet, lines);
            if (priceLines.isEmpty()) return;

            // 元の説明と地続きに見えないよう、1行空けてから足す
            lines.add(Component.empty());
            lines.addAll(priceLines);
        });
    }

    private static List<Component> build(ModConfig.InterfaceCategory config, String itemId, int count) {
        List<Component> lines = new ArrayList<>();

        if (config.showLowestBin) {
            Double lowestBin = ItemPrices.lowestBin(itemId);
            if (lowestBin != null) lines.add(line(config, "Lowest BIN", lowestBin, count));
        }

        if (config.showBazaarPrice) {
            ItemPrices.Bazaar bazaar = ItemPrices.bazaar(itemId);
            if (bazaar != null) {
                BazaarPriceType shown = config.bazaarPriceType;
                if (shown != BazaarPriceType.INSTANT_SELL) {
                    lines.add(line(config, "Bazaar Buy", bazaar.instantBuy(), count));
                }
                if (shown != BazaarPriceType.INSTANT_BUY) {
                    lines.add(line(config, "Bazaar Sell", bazaar.instantSell(), count));
                }
            }
        }

        if (config.showCraftCost) addCraftCost(config, lines, itemId, count);

        return lines;
    }

    /**
     * ペットの値段。
     *
     * ペットは Bazaar には無く、作ることもできないので最安 BIN だけを出す
     */
    private static List<Component> buildPet(ModConfig.InterfaceCategory config, SkyblockItemId.Pet pet,
                                            List<Component> lore) {
        if (!config.showLowestBin) return List.of();

        PetBin.Result found = PetBin.of(pet, lore);
        if (found == null) return List.of();

        // 手元のペットと違うレベルの値段なら、どのレベルのものかを添える
        String label = found.listed() == found.level()
                ? "Lowest BIN"
                : "Lowest BIN (Lvl " + found.listed() + ")";
        return List.of(line(config, label, found.price(), 1));
    }

    /**
     * 材料をそろえた場合の値段。
     *
     * Bazaar の材料は買い方で値段が変わるので、
     * 今すぐ買う場合と、買い注文を出して待つ場合の両方を出す
     */
    private static void addCraftCost(ModConfig.InterfaceCategory config, List<Component> lines,
                                     String itemId, int count) {
        ItemRecipes.Recipe recipe = ItemRecipes.of(itemId);
        if (recipe == null) return;

        Double instant = craftCost(recipe, true);
        Double order = craftCost(recipe, false);
        if (instant == null || order == null) return;

        // Auction House だけで揃う品は、待っても値段が変わらない
        if (instant.equals(order)) {
            lines.add(line(config, "Craft", instant, count));
            return;
        }

        lines.add(line(config, "Craft (Instant Buy)", instant, count));
        lines.add(line(config, "Craft (Buy Order)", order, count));
    }

    /** 材料のどれか1つでも値段が分からなければ null。当てにならない合計は出さない */
    private static Double craftCost(ItemRecipes.Recipe recipe, boolean instant) {
        double total = 0;

        for (Map.Entry<String, Integer> ingredient : recipe.ingredients().entrySet()) {
            Double unit = unitPrice(ingredient.getKey(), instant);
            if (unit == null) return null;

            total += unit * ingredient.getValue();
        }

        return total / recipe.count();
    }

    /**
     * 材料1つ分の値段。
     *
     * Bazaar にある品は、今すぐ買うなら即購入の値段、
     * 買い注文を出して待つなら即売却の値段あたりで手に入る
     */
    private static Double unitPrice(String itemId, boolean instant) {
        ItemPrices.Bazaar bazaar = ItemPrices.bazaar(itemId);
        if (bazaar != null) return instant ? bazaar.instantBuy() : bazaar.instantSell();

        return ItemPrices.lowestBin(itemId);
    }

    private static Component line(ModConfig.InterfaceCategory config, String label, double price, int count) {
        StringBuilder text = new StringBuilder(LABEL_STYLE).append(label)
                .append(": ").append(COIN_STYLE).append(format(config, price));

        // まとめて持っているときは、その分の合計も添える
        if (count > 1 && config.showStackPrice) {
            text.append(' ').append(LABEL_STYLE).append('(')
                    .append(COIN_STYLE).append(format(config, price * count))
                    .append(LABEL_STYLE).append(" for ").append(count).append(')');
        }

        return Component.literal(text.toString());
    }

    private static String format(ModConfig.InterfaceCategory config, double price) {
        return CoinText.format(price, config.shortPriceNumbers);
    }
}
