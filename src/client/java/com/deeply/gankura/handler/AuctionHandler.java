package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.PetBin;
import com.deeply.gankura.util.SkyblockItemId;
import com.deeply.gankura.util.SlotColorCache;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auction House に出している自分の品の様子を、枠の色で知らせる。
 *
 *   緑 … 売れた。コインを受け取りに行ける
 *   赤 … 期限切れ。品を引き取りに行ける
 *   黄 … まだ売れていないが、自分より安い同じ品が出ている
 *   なし … まだ売れていない上に、自分が最安
 */
public final class AuctionHandler {

    // 自分の出品を並べている画面
    private static final String MANAGE_TITLE = "Manage Auctions";

    // 出品の様子。売れたものと期限切れのものだけが書かれる
    private static final String SOLD_LINE = "Status: Sold!";
    private static final String EXPIRED_LINE = "Status: Expired!";
    // 即決の値段。競りに出しているものにはこの行が無い
    private static final Pattern BUY_IT_NOW = Pattern.compile("Buy it now: ([\\d,]+) coins");

    // 濃さは設定で決まるので、ここでは色味だけを持つ
    private static final int SOLD_COLOR = 0x55FF55;
    private static final int EXPIRED_COLOR = 0xFF5555;
    private static final int UNDERCUT_COLOR = 0xFFFF55;

    // 値段はコイン単位で書かれる。丸めの差で抜かれたと見なさないための余裕
    private static final double PRICE_EPSILON = 0.5;

    private static final SlotColorCache COLORS = new SlotColorCache();

    private AuctionHandler() {
    }

    /** 自分の出品を並べている画面か */
    public static boolean inManageMenu(String title) {
        return title.equals(MANAGE_TITLE);
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        return COLORS.get(slot, AuctionHandler::compute);
    }

    private static Integer compute(ItemStack stack) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableAuctionTweaks || !config.highlightOwnAuctions) return null;
        if (!GameState.Server.isSkyblock() || stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        Double price = null;
        for (Component line : lore.lines()) {
            String text = line.getString().trim();

            if (text.equals(SOLD_LINE)) return SOLD_COLOR;
            if (text.equals(EXPIRED_LINE)) return EXPIRED_COLOR;

            Matcher buyItNow = BUY_IT_NOW.matcher(text);
            if (buyItNow.find()) price = parse(buyItNow.group(1));
        }
        // まだ売れていない品だけがここに来る。競りのものは比べる相手が違うので見送る
        if (price == null || !config.highlightUndercutAuctions) return null;

        // 値段を照らし合わせるので、古いままにしない
        ItemPrices.refreshIfStale();

        Double lowestBin = lowestBin(stack, lore);
        if (lowestBin == null) return null;

        // 最安が自分の出品なら、その値段がそのまま返ってくる
        return lowestBin < price - PRICE_EPSILON ? UNDERCUT_COLOR : null;
    }

    /** 同じ品の今の最安 BIN。ペットはレベルと段まで込みで引く */
    private static Double lowestBin(ItemStack stack, ItemLore lore) {
        SkyblockItemId.Pet pet = SkyblockItemId.pet(stack);
        if (pet != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(stack.getHoverName());
            lines.addAll(lore.lines());

            PetBin.Result found = PetBin.of(pet, lines);
            return found == null ? null : found.price();
        }

        String itemId = SkyblockItemId.of(stack);
        return itemId == null ? null : ItemPrices.lowestBin(itemId);
    }

    private static Double parse(String number) {
        try {
            return Double.parseDouble(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
