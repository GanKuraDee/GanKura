package com.deeply.gankura.handler;

import com.deeply.gankura.data.AttributeCostSort;
import com.deeply.gankura.data.AttributeShards;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.mixin.ContainerScreenAccessor;
import com.deeply.gankura.util.CoinText;
import com.deeply.gankura.util.PanelBox;
import com.deeply.gankura.util.TierText;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hunting Box の横に、いま持っているシャードを売った額を並べる。
 *
 * 売り方で受け取る額が変わるので、今すぐ売るといくらか(Instant)と、
 * 売り注文を出して待つといくらか(Order)の両方を、高い順に出す。
 * 一覧はページごとなので、合計もそのページに並んでいるぶんの額になる
 */
public final class HuntingBoxPanel {

    // 題は "(1/7) Hunting Box" のようにページ数が頭に付く
    private static final String MENU_TITLE = "Hunting Box";

    // 名前に乗せたときの下敷き。押せることが分かるよう、スロットと同じ明るさで敷く
    private static final int HOVER_COLOR = 0x80FFFFFF;
    private static final String SEARCH_HINT = "Click to search the Bazaar";
    // Bazaar での品名。"Gemzie" ではなく "Gemzie Shard" で並んでいる
    private static final String SHARD_SUFFIX = " Shard";

    private static final int TITLE_COLOR = 0xFF404040;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int TOTAL_COLOR = 0xFF202020;
    private static final int INSTANT_COLOR = 0xFFAA5500;
    private static final int ORDER_COLOR = 0xFF006B6B;

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int COLUMN_GAP = 8;
    // 持ち物の窓との間隔
    private static final int MARGIN = 4;
    private static final int SCREEN_EDGE = 2;

    private static final String TITLE = "Shard Value";
    private static final String OWNED_LABEL = "Owned";
    private static final String INSTANT_LABEL = "Instant";
    private static final String ORDER_LABEL = "Order";
    private static final String TOTAL_LABEL = "Total";
    // 並べ替えに使っている方の見出しに引く線
    private static final int UNDERLINE_HEIGHT = 1;
    private static final int UNDERLINE_GAP = 1;
    // 見出しの行だけは、下線の分だけ次の行との間を空ける
    private static final int HEADER_SPACE = 3;
    // 合計の行を、一覧から少し離す
    private static final int TOTAL_SPACE = 3;

    /** 一覧の1行分。shard は品の名前、owned はいま持っている枚数 */
    private record Entry(String shard, int owned, double instant, double order) {
    }

    // 中身が変わるまでは、前に調べた一覧を使い回す
    private static AbstractContainerMenu cachedMenu;
    private static int cachedState;
    private static AttributeCostSort cachedSort;
    private static List<Entry> cachedEntries = List.of();

    // 直前に描いた見出しの場所。押されたかどうかを見るために控えておく
    private static int instantLeft;
    private static int instantRight;
    private static int orderLeft;
    private static int orderRight;
    private static int labelTop;
    private static int labelBottom;
    private static int rowsTop;
    private static int nameLeft;
    private static int nameRight;
    // 今描いている行。押された行からシャードを引くために控えておく
    private static List<Entry> shownEntries = List.of();
    private static boolean drawn = false;

    private HuntingBoxPanel() {
    }

    /** 箱の見出しを押したら、どちらの額で並べるかを切り替える */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> !clicked(event.x(), event.y()));
        });
    }

    private static boolean clicked(double mouseX, double mouseY) {
        // 出していないうちは、控えてある場所を押されても何も起こさない
        if (!drawn) return false;

        if (mouseY >= labelTop && mouseY <= labelBottom) {
            if (between(mouseX, instantLeft, instantRight)) return pickSort(AttributeCostSort.INSTANT);
            if (between(mouseX, orderLeft, orderRight)) return pickSort(AttributeCostSort.ORDER);
            return false;
        }

        return searchShard(mouseX, mouseY);
    }

    /** 名前を押されたら、そのシャードを Bazaar で探す */
    private static boolean searchShard(double mouseX, double mouseY) {
        int row = hoveredRow((int) mouseX, (int) mouseY, shownEntries.size());
        if (row < 0) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        client.player.connection.sendCommand("bz " + shownEntries.get(row).shard() + SHARD_SUFFIX);
        return true;
    }

    private static boolean between(double value, int left, int right) {
        return value >= left && value <= right;
    }

    private static boolean pickSort(AttributeCostSort picked) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (config.shardValueSort != picked) {
            config.shardValueSort = picked;
            ModConfig.INSTANCE.saveNow();
        }
        return true;
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int mouseX, int mouseY) {
        // 出していないうちに押されても効かないよう、描く前に必ず倒しておく
        drawn = false;

        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableHuntingBoxTweaks || !config.showShardValues) return;
        if (!GameState.Server.isSkyblock()) return;
        if (!screen.getTitle().getString().contains(MENU_TITLE)) return;

        // 額を並べるので、古いままにしない
        ItemPrices.refreshIfStale();

        List<Entry> entries = entries(config.shardValueSort);
        if (entries.isEmpty()) return;

        draw(screen, graphics, entries, mouseX, mouseY);
    }

    // -------------------------------------------------- 中身

    private static List<Entry> entries(AttributeCostSort sort) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return List.of();

        AbstractContainerMenu menu = client.player.containerMenu;
        int state = menu.getStateId();

        if (menu == cachedMenu && state == cachedState && sort == cachedSort) return cachedEntries;

        cachedMenu = menu;
        cachedState = state;
        cachedSort = sort;
        cachedEntries = read(menu, sort);
        return cachedEntries;
    }

    private static List<Entry> read(AbstractContainerMenu menu, AttributeCostSort sort) {
        List<Entry> entries = new ArrayList<>();

        for (Slot slot : menu.slots) {
            Entry entry = entry(slot.getItem());
            if (entry != null) entries.add(entry);
        }

        // 売る話なので、高い方から並べる
        entries.sort(Comparator.comparingDouble(
                sort == AttributeCostSort.ORDER ? Entry::order : Entry::instant).reversed());
        return entries;
    }

    /** そのシャードを売った額。持っていないものや、値段が分からないものは null */
    private static Entry entry(ItemStack stack) {
        Integer owned = TierText.shardsOwned(stack);
        if (owned == null || owned <= 0) return null;

        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name == null) return null;

        String shard = name.trim();
        String shardId = AttributeShards.idOf(shard);
        if (shardId == null) return null;

        ItemPrices.Bazaar market = ItemPrices.bazaar(shardId);
        if (market == null) return null;

        // 買うときとは受け取る側が逆になる。今すぐ売ると買い注文の値、
        // 売り注文を出して待つと売り注文の値で捌ける
        return new Entry(shard, owned, market.instantSell() * owned, market.instantBuy() * owned);
    }

    // -------------------------------------------------- 描画

    private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                             List<Entry> entries, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;

        // 合計はページに並んでいるすべてのシャードぶん。一覧が途中で切れても変わらない
        int totalOwned = 0;
        double totalInstant = 0;
        double totalOrder = 0;
        for (Entry entry : entries) {
            totalOwned += entry.owned();
            totalInstant += entry.instant();
            totalOrder += entry.order();
        }

        List<Entry> shown = entries.subList(0, Math.min(entries.size(), config.shardValueRows));

        List<String> owneds = new ArrayList<>();
        List<String> instants = new ArrayList<>();
        List<String> orders = new ArrayList<>();
        for (Entry entry : shown) {
            owneds.add(String.valueOf(entry.owned()));
            instants.add(CoinText.format(entry.instant(), true));
            orders.add(CoinText.format(entry.order(), true));
        }

        String totalOwnedText = String.valueOf(totalOwned);
        String totalInstantText = CoinText.format(totalInstant, true);
        String totalOrderText = CoinText.format(totalOrder, true);

        int nameWidth = Math.max(width(font, shown.stream().map(Entry::shard).toList()),
                font.width(TOTAL_LABEL));
        int ownedWidth = widest(font, owneds, OWNED_LABEL, totalOwnedText);
        int instantWidth = widest(font, instants, INSTANT_LABEL, totalInstantText);
        int orderWidth = widest(font, orders, ORDER_LABEL, totalOrderText);

        int inner = Math.max(nameWidth + COLUMN_GAP + ownedWidth + COLUMN_GAP + instantWidth
                + COLUMN_GAP + orderWidth, font.width(TITLE));
        int panelWidth = inner + PADDING * 2;
        // 見出しと列名で2行、そのあとが一覧、最後に合計の行
        int panelHeight = PADDING * 2 + LINE_HEIGHT * (shown.size() + 3) + HEADER_SPACE * 2 + TOTAL_SPACE;

        ContainerScreenAccessor box = (ContainerScreenAccessor) screen;
        int x = Math.max(box.gankura$getLeftPos() - panelWidth - MARGIN, SCREEN_EDGE);
        int y = box.gankura$getTopPos();

        PanelBox.draw(graphics, x, y, panelWidth, panelHeight);

        // 額は右端をそろえる。桁が違っても見比べやすい
        int orderRight = x + panelWidth - PADDING;
        int instantRight = orderRight - orderWidth - COLUMN_GAP;
        int ownedRight = instantRight - instantWidth - COLUMN_GAP;
        int textY = y + PADDING;

        graphics.text(font, TITLE, x + PADDING, textY, TITLE_COLOR, false);
        textY += LINE_HEIGHT + HEADER_SPACE;

        graphics.text(font, OWNED_LABEL, ownedRight - font.width(OWNED_LABEL), textY, NAME_COLOR, false);
        label(graphics, font, INSTANT_LABEL, instantRight, textY, INSTANT_COLOR,
                config.shardValueSort == AttributeCostSort.INSTANT);
        label(graphics, font, ORDER_LABEL, orderRight, textY, ORDER_COLOR,
                config.shardValueSort == AttributeCostSort.ORDER);
        remember(font, instantRight, orderRight, textY);
        textY += LINE_HEIGHT + HEADER_SPACE;

        rememberRows(shown, x + PADDING, x + PADDING + nameWidth, textY);

        int hovered = hoveredRow(mouseX, mouseY, shown.size());

        for (int i = 0; i < shown.size(); i++) {
            if (i == hovered) {
                graphics.fill(nameLeft - 1, textY - 1, nameRight + 1, textY + font.lineHeight, HOVER_COLOR);
            }

            graphics.text(font, shown.get(i).shard(), x + PADDING, textY, NAME_COLOR, false);
            graphics.text(font, owneds.get(i), ownedRight - font.width(owneds.get(i)), textY, NAME_COLOR, false);
            graphics.text(font, instants.get(i), instantRight - font.width(instants.get(i)), textY,
                    INSTANT_COLOR, false);
            graphics.text(font, orders.get(i), orderRight - font.width(orders.get(i)), textY, ORDER_COLOR, false);
            textY += LINE_HEIGHT;
        }

        textY += TOTAL_SPACE;
        graphics.text(font, TOTAL_LABEL, x + PADDING, textY, TOTAL_COLOR, false);
        graphics.text(font, totalOwnedText, ownedRight - font.width(totalOwnedText), textY, TOTAL_COLOR, false);
        graphics.text(font, totalInstantText, instantRight - font.width(totalInstantText), textY,
                INSTANT_COLOR, false);
        graphics.text(font, totalOrderText, orderRight - font.width(totalOrderText), textY, ORDER_COLOR, false);

        if (hovered < 0) return;

        // 何が起きるかを添える。箱の外なので、スロットの説明と取り合いにならない
        Component hint = Component.literal(SEARCH_HINT).withStyle(ChatFormatting.GRAY);
        graphics.setTooltipForNextFrame(font, List.of(hint.getVisualOrderText()),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
    }

    /** カーソルが乗っている行。名前の上でなければ -1 */
    private static int hoveredRow(int mouseX, int mouseY, int rows) {
        if (!between(mouseX, nameLeft, nameRight) || mouseY < rowsTop) return -1;

        int row = (mouseY - rowsTop) / LINE_HEIGHT;
        return row < rows ? row : -1;
    }

    /** 並べ替えに使っている方には線を引いて、押せることと今どちらかを見せる */
    private static void label(GuiGraphicsExtractor graphics, Font font, String text, int right, int y,
                              int color, boolean sorting) {
        int left = right - font.width(text);
        graphics.text(font, text, left, y, color, false);

        if (!sorting) return;

        int lineY = y + font.lineHeight + UNDERLINE_GAP;
        graphics.fill(left, lineY, right, lineY + UNDERLINE_HEIGHT, color);
    }

    // 行の場所を控える。名前を押されたかどうかはこれで見る
    private static void rememberRows(List<Entry> entries, int left, int right, int y) {
        shownEntries = entries;
        nameLeft = left;
        nameRight = right;
        rowsTop = y;
    }

    // 見出しの場所を控える。押されたかどうかはこれで見る
    private static void remember(Font font, int instantEdge, int orderEdge, int y) {
        instantRight = instantEdge;
        instantLeft = instantEdge - font.width(INSTANT_LABEL);
        orderRight = orderEdge;
        orderLeft = orderEdge - font.width(ORDER_LABEL);
        labelTop = y;
        labelBottom = y + font.lineHeight;
        drawn = true;
    }

    private static int widest(Font font, List<String> texts, String... extras) {
        int width = width(font, texts);
        for (String extra : extras) width = Math.max(width, font.width(extra));
        return width;
    }

    private static int width(Font font, List<String> texts) {
        int widest = 0;
        for (String text : texts) widest = Math.max(widest, font.width(text));
        return widest;
    }
}
