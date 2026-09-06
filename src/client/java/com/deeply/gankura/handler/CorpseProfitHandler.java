package com.deeply.gankura.handler;

import com.deeply.gankura.data.BazaarNames;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.CoinText;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corpse を開けたときの取り分を勘定する。
 *
 * 中身は次の形で、1行ずつ別のチャットとして届く。
 * <pre>
 * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
 * LAPIS CORPSE LOOT!
 *
 * REWARDS
 *   Green Goblin Egg
 *   ❈ Flawed Onyx Gemstone ×20
 *   Glacite Powder ×4,390
 * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
 * </pre>
 *
 * そのため、見出しの行で溜め始め、締めの区切り行(または品らしくない行)で締める。
 * Lapis 以外は鍵を消費するので、その値段を引いたものを取り分とする
 */
public class CorpseProfitHandler {

    private static final Pattern LOOT_HEADER = Pattern.compile("^([A-Z]+) CORPSE LOOT!$");
    private static final String REWARDS_HEADER = "REWARDS";

    /**
     * 品の行。
     *
     * 頭に飾りの記号が付くことがあり、個数は "×20" や "x4,390" の形で後ろに付く。
     * 個数が1のときは省かれる。
     * 名前に使われる字を絞ってあるので、"+5 Kill Combo" のような別の行はここで弾ける
     */
    private static final Pattern REWARD_LINE = Pattern.compile(
            "^(?:[^\\p{L}\\p{N}\\s]+\\s+)?(?<name>\\p{L}[\\p{L}\\p{N} '-]*?)(?:\\s+[x×](?<count>[\\d,]+))?$");

    // 締めの区切り行。同じ記号が並ぶだけの行を、この長さ以上あれば区切りとみなす
    private static final int SEPARATOR_MIN_LENGTH = 8;

    // 内訳の中で、品の並びと合計を分ける線の長さ
    private static final int BREAKDOWN_RULE_LENGTH = 20;

    // 開けるのに要る鍵。Lapis は鍵が要らないので載せない
    private static final Map<String, String> KEY_IDS = Map.of(
            "UMBER", "UMBER_KEY",
            "TUNGSTEN", "TUNGSTEN_KEY",
            "VANGUARD", "SKELETON_KEY");

    // 種類ごとの色。ワールド上の目印(CorpseScanner)と同じ割り当てにしてある
    private static final Map<String, String> KIND_COLORS = Map.of(
            "LAPIS", "§9",
            "UMBER", "§6",
            "TUNGSTEN", "§7",
            "VANGUARD", "§b");

    // 知らない種類が増えたときの色。色が無いより、白で出るほうが気付ける
    private static final String DEFAULT_KIND_COLOR = "§f";

    /** 溜めている最中の品 1 つ分 */
    private record Reward(String name, int count) {
    }

    private static String kind = null;
    // REWARDS の見出しを過ぎたか。ここから先だけを品の並びとして読む
    private static boolean collecting = false;
    private static final List<Reward> rewards = new ArrayList<>();

    public static void handleMessage(String unformattedMessage, Minecraft client) {
        if (!ModConfig.INSTANCE.mining.showCorpseProfit) {
            reset();
            return;
        }

        // 値段の元は取りに行ってから使えるまでに間があるので、Mineshaft にいる間は先に温めておく。
        // Corpse を開けてから頼むと、その1体分だけ値段が出ない
        warmUpPrices();

        String line = unformattedMessage.trim();

        Matcher header = LOOT_HEADER.matcher(line);
        if (header.matches()) {
            // 2体続けて開けると見出しが続けて来るので、溜まっている分を先に出し切る
            report(client);
            kind = header.group(1);
            collecting = false;
            return;
        }

        if (kind == null) return;
        if (line.isEmpty()) return;

        if (REWARDS_HEADER.equals(line)) {
            collecting = true;
            return;
        }

        // 見出しと REWARDS の間には "+1 bonus drop!" のような行が挟まることがある。
        // 品の並びが始まる前なので、ここで締めずに読み飛ばす
        if (!collecting) return;

        Matcher reward = REWARD_LINE.matcher(line);
        if (isSeparator(line) || !reward.matches()) {
            report(client);
            return;
        }

        rewards.add(new Reward(reward.group("name").trim(), count(reward.group("count"))));
    }

    /**
     * 値段の元を先に読み込ませておく。
     *
     * どちらも裏で取りに行く作りなので、頼んだその場では使えない。
     * Mineshaft に入った時点から頼んでおけば、Corpse を開ける頃には揃っている。
     * どちらも取得済みなら何もしないので、毎行呼んでも構わない
     */
    private static void warmUpPrices() {
        if (!GameState.Server.isMineshaft()) return;

        ItemPrices.refreshIfStale();
        BazaarNames.prefetch();
    }

    private static int count(String text) {
        if (text == null) return 1;

        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // 同じ記号だけが並ぶ行か。Hypixel は品書きの前後をこれで挟んでいる
    private static boolean isSeparator(String line) {
        if (line.length() < SEPARATOR_MIN_LENGTH) return false;

        char first = line.charAt(0);
        if (Character.isLetterOrDigit(first)) return false;

        return line.chars().allMatch(c -> c == first);
    }

    private static void report(Minecraft client) {
        if (kind == null || rewards.isEmpty()) {
            reset();
            return;
        }

        String label = label(kind);
        String color = KIND_COLORS.getOrDefault(kind, DEFAULT_KIND_COLOR);
        // 内訳はチャットに並べるとうるさいので、行に重ねたときだけ出す
        List<String> breakdown = new ArrayList<>();
        breakdown.add(color + "§l" + label + " Corpse");

        double value = 0;
        for (Reward reward : rewards) {
            Double unit = sellPrice(reward.name());
            // 値段の分からない品。Glacite Powder のように売り買いできないものが混じる
            if (unit != null) value += unit * reward.count();

            breakdown.add(rewardLine(reward, unit));
        }

        double keyCost = keyCost();
        double profit = value - keyCost;

        breakdown.add("§8" + "-".repeat(BREAKDOWN_RULE_LENGTH));
        breakdown.add("§7Drops: §6" + CoinText.format(value, true));
        if (keyCost > 0) {
            breakdown.add("§7" + label + " Key: §c-" + CoinText.format(keyCost, true));
        }
        breakdown.add("§7Profit: " + coins(profit));

        // 種類の名前は太字にする。続く §7 で太字は解除されるので、後ろまで太くはならない
        String message = color + "§l" + label + " Corpse §7Profit: " + coins(profit);
        MutableComponent hover = Component.literal(String.join("\n", breakdown));
        MutableComponent line = Component.literal(message)
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));

        reset();
        client.execute(() -> NotificationUtils.sendSystemChat(client, line));
    }

    // 内訳の1行。"Flawed Onyx Gemstone x20  1.2M" の形にする
    private static String rewardLine(Reward reward, Double unit) {
        StringBuilder text = new StringBuilder("§7").append(reward.name());
        if (reward.count() > 1) {
            text.append(" §8x").append(String.format(Locale.US, "%,d", reward.count()));
        }

        // 値段の付かない品も、抜けたように見えないよう行だけは出す
        text.append(' ').append(unit == null ? "§8no price" : "§6" + CoinText.format(unit * reward.count(), true));
        return text.toString();
    }

    // 損得が一目で分かるよう、符号と色を揃える
    private static String coins(double amount) {
        return (amount >= 0 ? "§6+" : "§c-") + CoinText.format(Math.abs(amount), true);
    }

    /**
     * その品を売ったときに入る額。分からなければ null。
     *
     * Bazaar にある品は即売却の値段、無い品は最安 BIN で見る
     */
    private static Double sellPrice(String name) {
        String itemId = BazaarNames.idOf(name);
        if (itemId == null) return null;

        ItemPrices.Bazaar bazaar = ItemPrices.bazaar(itemId);
        if (bazaar != null) return bazaar.instantSell();

        return ItemPrices.lowestBin(itemId);
    }

    /** 開けるのに使った鍵の値段。鍵の要らない Lapis と、値段が分からないときは 0 */
    private static double keyCost() {
        String keyId = KEY_IDS.get(kind);
        if (keyId == null) return 0;

        ItemPrices.Bazaar bazaar = ItemPrices.bazaar(keyId);
        // 買う側の値段。手持ちの鍵でも、使えばその分だけ売れる物が減る
        if (bazaar != null) return bazaar.instantBuy();

        Double bin = ItemPrices.lowestBin(keyId);
        return bin == null ? 0 : bin;
    }

    // "LAPIS" のような大文字だけの名前を "Lapis" に直す
    private static String label(String rawKind) {
        return rawKind.charAt(0) + rawKind.substring(1).toLowerCase(Locale.US);
    }

    private static void reset() {
        kind = null;
        collecting = false;
        rewards.clear();
    }
}
