package com.deeply.gankura.handler;

import com.deeply.gankura.data.AttributeCostSort;
import com.deeply.gankura.data.AttributeCostTarget;
import com.deeply.gankura.data.AttributeShards;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemPrices;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.mixin.ContainerScreenAccessor;
import com.deeply.gankura.util.CoinText;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attribute Menu の横に、打ち止めでない Attribute を上げるのにかかる額を並べる。
 *
 * シャードは Bazaar で買えるので、いま買うといくらか(Instantly)と、
 * 買い注文を出して待つといくらか(Order)の両方を、安い順に出す
 */
public final class AttributeCostPanel {

    private static final String MENU_TITLE = "Attribute Menu";

    // そのティアを上げるのに使うシャード。"Source: Black Widow Shard (R87)" と書かれている
    private static final Pattern SOURCE = Pattern.compile("Source: (.+) Shard \\(");
    // あと何枚いるか。まだ見つけていない Attribute には unlock の方が書かれる
    private static final Pattern TO_NEXT = Pattern.compile("Syphon ([\\d,]+) shards? to (?:level up|unlock)!");
    private static final Pattern TO_MAX = Pattern.compile("Syphon ([\\d,]+) shards? to max!");

    // 箱の見た目。持ち物の窓と同じ配色にして、隣に並べても浮かないようにする
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;
    private static final int LIGHT_EDGE_COLOR = 0xFFFFFFFF;
    private static final int DARK_EDGE_COLOR = 0xFF555555;

    // 名前に乗せたときの下敷き。押せることが分かるよう、スロットと同じ明るさで敷く
    private static final int HOVER_COLOR = 0x80FFFFFF;
    private static final String SEARCH_HINT = "Click to search the Bazaar";
    // Bazaar での品名。"Black Widow" ではなく "Black Widow Shard" で並んでいる
    private static final String SHARD_SUFFIX = " Shard";

    private static final int TITLE_COLOR = 0xFF404040;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int INSTANT_COLOR = 0xFFAA5500;
    private static final int ORDER_COLOR = 0xFF006B6B;

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int COLUMN_GAP = 8;
    // 持ち物の窓との間隔
    private static final int MARGIN = 4;
    private static final int SCREEN_EDGE = 2;

    private static final String TITLE = "Shard Cost";
    private static final String NEXT_LABEL = "Next";
    private static final String MAX_LABEL = "Max";
    private static final String NEED_LABEL = "Need";
    private static final String INSTANT_LABEL = "Instant";
    private static final String ORDER_LABEL = "Order";
    // 並べ替えに使っている方の見出しに引く線
    private static final int UNDERLINE_HEIGHT = 1;
    private static final int UNDERLINE_GAP = 1;
    // 見出しの行だけは、下線の分だけ次の行との間を空ける
    private static final int HEADER_SPACE = 3;

    /** 一覧の1行分。shard は元になるシャードの名前、shards はそこまでに要る枚数 */
    private record Entry(String shard, int shards, double instant, double order) {
    }

    // 中身が変わるまでは、前に調べた一覧を使い回す
    private static AbstractContainerMenu cachedMenu;
    private static int cachedState;
    private static AttributeCostTarget cachedTarget;
    private static AttributeCostSort cachedSort;
    private static List<Entry> cachedEntries = List.of();

    // 直前に描いた見出しの場所。押されたかどうかを見るために控えておく
    private static int nextLeft;
    private static int nextRight;
    private static int maxLeft;
    private static int maxRight;
    private static int titleTop;
    private static int titleBottom;
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

    private AttributeCostPanel() {
    }

    /** 箱の見出しを押したら、どこまでの値段を出すかと、どちらで並べるかを切り替える */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;

            ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> !clicked(event.x(), event.y()));
        });
    }

    private static boolean clicked(double mouseX, double mouseY) {
        if (!drawn) return false;

        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;

        if (mouseY >= titleTop && mouseY <= titleBottom) {
            if (between(mouseX, nextLeft, nextRight)) return pickTarget(config, AttributeCostTarget.NEXT_TIER);
            if (between(mouseX, maxLeft, maxRight)) return pickTarget(config, AttributeCostTarget.MAX_TIER);
            return false;
        }

        if (mouseY >= labelTop && mouseY <= labelBottom) {
            if (between(mouseX, instantLeft, instantRight)) return pickSort(config, AttributeCostSort.INSTANT);
            if (between(mouseX, orderLeft, orderRight)) return pickSort(config, AttributeCostSort.ORDER);
            return false;
        }

        return searchShard(mouseX, mouseY);
    }

    /** 名前を押されたら、その Attribute のシャードを Bazaar で探す */
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

    private static boolean pickTarget(ModConfig.InterfaceCategory config, AttributeCostTarget picked) {
        if (config.attributeCostTarget != picked) {
            config.attributeCostTarget = picked;
            ModConfig.INSTANCE.saveNow();
        }
        return true;
    }

    private static boolean pickSort(ModConfig.InterfaceCategory config, AttributeCostSort picked) {
        if (config.attributeCostSort != picked) {
            config.attributeCostSort = picked;
            ModConfig.INSTANCE.saveNow();
        }
        return true;
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int mouseX, int mouseY) {
        ModConfig.InterfaceCategory config = ModConfig.INSTANCE.interfaceSettings;
        if (!config.enableAttributeMenuTweaks || !config.showAttributeCosts) return;
        if (!GameState.Server.isSkyblock()) return;
        if (!screen.getTitle().getString().contains(MENU_TITLE)) return;

        // 値段を並べるので、古いままにしない
        ItemPrices.refreshIfStale();

        List<Entry> entries = entries(config.attributeCostTarget, config.attributeCostSort);
        if (entries.isEmpty()) {
            drawn = false;
            return;
        }

        draw(screen, graphics, config, entries.subList(0, Math.min(entries.size(), config.attributeCostRows)),
                mouseX, mouseY);
    }

    // -------------------------------------------------- 中身

    private static List<Entry> entries(AttributeCostTarget target, AttributeCostSort sort) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return List.of();

        AbstractContainerMenu menu = client.player.containerMenu;
        int state = menu.getStateId();

        if (menu == cachedMenu && state == cachedState && target == cachedTarget && sort == cachedSort) {
            return cachedEntries;
        }

        cachedMenu = menu;
        cachedState = state;
        cachedTarget = target;
        cachedSort = sort;
        cachedEntries = read(menu, target, sort);
        return cachedEntries;
    }

    private static List<Entry> read(AbstractContainerMenu menu, AttributeCostTarget target, AttributeCostSort sort) {
        List<Entry> entries = new ArrayList<>();

        for (Slot slot : menu.slots) {
            Entry entry = entry(slot.getItem(), target);
            if (entry != null) entries.add(entry);
        }

        entries.sort(Comparator.comparingDouble(
                sort == AttributeCostSort.ORDER ? Entry::order : Entry::instant));
        return entries;
    }

    /** その Attribute を上げるのにかかる額。打ち止めのものや、値段が分からないものは null */
    private static Entry entry(ItemStack stack, AttributeCostTarget target) {
        if (stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        Pattern needed = target == AttributeCostTarget.MAX_TIER ? TO_MAX : TO_NEXT;
        String shard = null;
        Integer shards = null;

        for (Component line : lore.lines()) {
            String text = line.getString();

            Matcher source = SOURCE.matcher(text);
            if (source.find()) {
                shard = source.group(1).trim();
                continue;
            }

            Matcher count = needed.matcher(text);
            if (count.find()) shards = number(count.group(1));
        }
        if (shard == null || shards == null || shards <= 0) return null;

        String shardId = AttributeShards.idOf(shard);
        if (shardId == null) return null;

        ItemPrices.Bazaar market = ItemPrices.bazaar(shardId);
        if (market == null) return null;

        return new Entry(shard, shards, market.instantBuy() * shards, market.instantSell() * shards);
    }

    private static Integer number(String text) {
        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------- 描画

    private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                             ModConfig.InterfaceCategory config, List<Entry> entries,
                             int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;

        List<String> needs = new ArrayList<>();
        List<String> instants = new ArrayList<>();
        List<String> orders = new ArrayList<>();
        for (Entry entry : entries) {
            needs.add(String.valueOf(entry.shards()));
            instants.add(CoinText.format(entry.instant(), true));
            orders.add(CoinText.format(entry.order(), true));
        }

        int nameWidth = width(font, entries.stream().map(Entry::shard).toList());
        int needWidth = Math.max(width(font, needs), font.width(NEED_LABEL));
        int instantWidth = Math.max(width(font, instants), font.width(INSTANT_LABEL));
        int orderWidth = Math.max(width(font, orders), font.width(ORDER_LABEL));

        // 見出しの行は "Shard Cost" と、右端に寄せた Next / Max の切り替え
        int titleWidth = font.width(TITLE) + COLUMN_GAP + font.width(NEXT_LABEL)
                + COLUMN_GAP + font.width(MAX_LABEL);
        int inner = Math.max(nameWidth + COLUMN_GAP + needWidth + COLUMN_GAP + instantWidth
                + COLUMN_GAP + orderWidth, titleWidth);
        int panelWidth = inner + PADDING * 2;
        // 見出しと列名で2行、そのあとが一覧
        int panelHeight = PADDING * 2 + LINE_HEIGHT * (entries.size() + 2) + HEADER_SPACE * 2;

        ContainerScreenAccessor box = (ContainerScreenAccessor) screen;
        int x = Math.max(box.gankura$getLeftPos() - panelWidth - MARGIN, SCREEN_EDGE);
        int y = box.gankura$getTopPos();

        panel(graphics, x, y, panelWidth, panelHeight);

        // 値段は右端をそろえる。桁が違っても見比べやすい
        int orderRight = x + panelWidth - PADDING;
        int instantRight = orderRight - orderWidth - COLUMN_GAP;
        int needRight = instantRight - instantWidth - COLUMN_GAP;
        int textY = y + PADDING;

        graphics.text(font, TITLE, x + PADDING, textY, TITLE_COLOR, false);

        int maxRightEdge = x + panelWidth - PADDING;
        int nextRightEdge = maxRightEdge - font.width(MAX_LABEL) - COLUMN_GAP;
        label(graphics, font, NEXT_LABEL, nextRightEdge, textY, TITLE_COLOR,
                config.attributeCostTarget == AttributeCostTarget.NEXT_TIER);
        label(graphics, font, MAX_LABEL, maxRightEdge, textY, TITLE_COLOR,
                config.attributeCostTarget == AttributeCostTarget.MAX_TIER);
        rememberTitle(font, nextRightEdge, maxRightEdge, textY);
        textY += LINE_HEIGHT + HEADER_SPACE;

        graphics.text(font, NEED_LABEL, needRight - font.width(NEED_LABEL), textY, NAME_COLOR, false);
        label(graphics, font, INSTANT_LABEL, instantRight, textY, INSTANT_COLOR,
                config.attributeCostSort == AttributeCostSort.INSTANT);
        label(graphics, font, ORDER_LABEL, orderRight, textY, ORDER_COLOR,
                config.attributeCostSort == AttributeCostSort.ORDER);
        remember(font, instantRight, orderRight, textY);
        textY += LINE_HEIGHT + HEADER_SPACE;

        rememberRows(entries, x + PADDING, x + PADDING + nameWidth, textY);

        int hovered = hoveredRow(mouseX, mouseY, entries.size());

        for (int i = 0; i < entries.size(); i++) {
            if (i == hovered) {
                graphics.fill(nameLeft - 1, textY - 1, nameRight + 1, textY + font.lineHeight, HOVER_COLOR);
            }

            graphics.text(font, entries.get(i).shard(), x + PADDING, textY, NAME_COLOR, false);
            graphics.text(font, needs.get(i), needRight - font.width(needs.get(i)), textY, NAME_COLOR, false);
            graphics.text(font, instants.get(i), instantRight - font.width(instants.get(i)), textY,
                    INSTANT_COLOR, false);
            graphics.text(font, orders.get(i), orderRight - font.width(orders.get(i)), textY, ORDER_COLOR, false);
            textY += LINE_HEIGHT;
        }

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

    private static void rememberTitle(Font font, int nextEdge, int maxEdge, int y) {
        nextRight = nextEdge;
        nextLeft = nextEdge - font.width(NEXT_LABEL);
        maxRight = maxEdge;
        maxLeft = maxEdge - font.width(MAX_LABEL);
        titleTop = y;
        titleBottom = y + font.lineHeight;
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

    private static int width(Font font, List<String> texts) {
        int widest = 0;
        for (String text : texts) widest = Math.max(widest, font.width(text));
        return widest;
    }

    // 持ち物の窓と同じ、へこんで見える枠
    private static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 2, LIGHT_EDGE_COLOR);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 2, LIGHT_EDGE_COLOR);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, DARK_EDGE_COLOR);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, DARK_EDGE_COLOR);
    }
}
