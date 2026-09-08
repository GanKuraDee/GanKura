package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Greenhouse の DNA Analyzer を解く。
 *
 * 盤は 9 列 4 行に並んだ色付きの DNA で、
 * 「隣り合う列の同じ行、または斜め上下に同じ色がある」状態にすると通る。
 * 動かせるのは同じ列の中での入れ替えだけなので、
 * 各列を並べ替えた 24 通りを列ごとの状態と見て、
 * 端から端まで繋がる並びを最小の入れ替え回数で探す。
 *
 * 答えは何手も先まで出るが、盤は 1 手ごとに送られ直してくるので、
 * 次に触る 2 枠だけを塗って示す
 */
public final class DnaAnalyzerHandler {

    // 題は "<何か> DNA" で終わる
    private static final String MENU_SUFFIX = " DNA";

    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    // 盤は 2 段目から 5 段目まで。1 段目は飾りで、6 段目に閉じるボタンが並ぶ
    private static final int FIRST_SLOT = COLUMNS;
    private static final int LAST_SLOT = FIRST_SLOT + COLUMNS * ROWS - 1;
    private static final int CLOSE_SLOT = 49;

    // 両端の列は動かせない
    private static final boolean ALLOW_ENDS = false;

    // どの並びからも辿り着けないことを示す、十分に大きい手数
    private static final int UNREACHABLE = 1000;

    // 次に触る 2 枠に敷く色。濃さは設定で決まるので、ここでは色味だけ
    private static final int SWAP_COLOR = 0x55FF55;

    private static final int[][] PERMUTATIONS = permutations();

    // 盤の中身が変わるまで解き直さないための覚え書き
    private static AbstractContainerMenu menu;
    private static int state = -1;
    private static boolean valid = false;
    private static int firstSlot = -1;
    private static int secondSlot = -1;

    private DnaAnalyzerHandler() {
    }

    /** DNA Analyzer の画面か */
    public static boolean inMenu(String title) {
        return title.endsWith(MENU_SUFFIX);
    }

    /** その枠に塗る色。塗らないときは null */
    public static Integer colorFor(Slot slot) {
        if (!isEnabled()) return null;
        if (!refresh()) return null;
        if (firstSlot < 0) return null;

        AbstractContainerMenu open = Minecraft.getInstance().player.containerMenu;
        // スロット番号だけで見ると持ち物側の枠と重なるので、器の枠そのものと突き合わせる
        if (slot == open.getSlot(firstSlot) || slot == open.getSlot(secondSlot)) return SWAP_COLOR;
        return null;
    }

    /** 閉じるボタンを踏まないように、その押下を握り潰すか */
    public static boolean blocksClick(int slotId) {
        return isEnabled() && refresh()
                && slotId == CLOSE_SLOT
                && ModConfig.INSTANCE.farming.garden.blockDnaAnalyzerClose;
    }

    /** 入れ替えを中クリック扱いにするか。持ち上げが挟まらず、続けて触れる */
    public static boolean usesMiddleClick() {
        return isEnabled() && refresh() && ModConfig.INSTANCE.farming.garden.dnaAnalyzerMiddleClick;
    }

    /** その枠の説明を伏せるか。盤の上では邪魔にしかならない */
    public static boolean hidesTooltip(int slotId) {
        return isEnabled() && refresh()
                && slotId >= FIRST_SLOT && slotId <= LAST_SLOT
                && ModConfig.INSTANCE.farming.garden.hideDnaAnalyzerTooltips;
    }

    private static boolean isEnabled() {
        return ModConfig.INSTANCE.farming.garden.solveDnaAnalyzer && GameState.Server.isSkyblock();
    }

    /**
     * 今開いている盤を読み直す。
     *
     * 盤の中身は同じでも枠は毎フレーム描かれるので、
     * 器が入れ替わったときと、器から新しい中身が届いたときだけ解き直す
     */
    private static boolean refresh() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        AbstractContainerMenu open = client.player.containerMenu;
        int openState = open.getStateId();
        if (open == menu && openState == state) return valid;

        menu = open;
        state = openState;
        valid = false;
        firstSlot = -1;
        secondSlot = -1;

        if (open.slots.size() <= LAST_SLOT) return false;

        Colour[][] board = new Colour[COLUMNS][ROWS];
        for (int slotId = FIRST_SLOT; slotId <= LAST_SLOT; slotId++) {
            Colour colour = colourOf(open.getSlot(slotId).getItem());
            // DNA でない品が混じっているなら、題が似ているだけの別の画面
            if (colour == null) return false;
            board[slotId % COLUMNS][slotId / COLUMNS - 1] = colour;
        }

        // 同じ列に同じ色が2つあると入れ替えでは崩せない。届き切っていない盤なので待つ
        for (Colour[] column : board) {
            for (int a = 0; a < ROWS; a++) {
                for (int b = a + 1; b < ROWS; b++) {
                    if (column[a] == column[b]) return false;
                }
            }
        }

        valid = true;

        int[] swap = solve(board);
        if (swap == null) return true;

        firstSlot = swap[0] + (swap[1] + 1) * COLUMNS;
        secondSlot = swap[0] + (swap[2] + 1) * COLUMNS;
        return true;
    }

    /** 次に触る入れ替え。{列, 行, 行} で返す。既に通っているなら null */
    private static int[] solve(Colour[][] board) {
        int first = ALLOW_ENDS ? 0 : 1;
        int last = ALLOW_ENDS ? COLUMNS - 1 : COLUMNS - 2;
        int count = last - first + 1;
        int patterns = PERMUTATIONS.length;

        int[][] cost = new int[count][patterns];
        int[][][] swaps = new int[count][patterns][];
        for (int i = 0; i < count; i++) {
            Colour[] column = board[first + i];
            for (int p = 0; p < patterns; p++) {
                int[] result = minimumSwaps(column, permute(column, PERMUTATIONS[p]));
                cost[i][p] = result[0];
                swaps[i][p] = result;
            }
        }

        int[][] best = new int[count][patterns];
        int[][] parent = new int[count][patterns];
        for (int i = 0; i < count; i++) {
            for (int p = 0; p < patterns; p++) {
                best[i][p] = UNREACHABLE;
                parent[i][p] = -1;
            }
        }

        for (int p = 0; p < patterns; p++) {
            Colour[] column = permute(board[first], PERMUTATIONS[p]);
            if (!ALLOW_ENDS && !connects(board[0], column)) continue;
            best[0][p] = cost[0][p];
        }

        for (int i = 1; i < count; i++) {
            for (int p = 0; p < patterns; p++) {
                Colour[] current = permute(board[first + i], PERMUTATIONS[p]);
                for (int q = 0; q < patterns; q++) {
                    if (best[i - 1][q] == UNREACHABLE) continue;
                    Colour[] previous = permute(board[first + i - 1], PERMUTATIONS[q]);
                    if (!connects(previous, current)) continue;

                    int total = best[i - 1][q] + cost[i][p];
                    if (total < best[i][p]) {
                        best[i][p] = total;
                        parent[i][p] = q;
                    }
                }
            }
        }

        int cheapest = UNREACHABLE;
        int tail = -1;
        for (int p = 0; p < patterns; p++) {
            Colour[] column = permute(board[last], PERMUTATIONS[p]);
            if (!ALLOW_ENDS && !connects(column, board[COLUMNS - 1])) continue;
            if (best[count - 1][p] < cheapest) {
                cheapest = best[count - 1][p];
                tail = p;
            }
        }
        if (tail < 0) return null;

        // 右の列から辿ると、最後に出てくるのが一番左の列の手になる。
        // 盤は 1 手ごとに送られ直してくるので、その手だけを示す
        int[] next = null;
        int current = tail;
        for (int i = count - 1; i >= 0 && current >= 0; i--) {
            int[] made = swaps[i][current];
            for (int k = 1; k + 1 < made.length; k += 2) {
                next = new int[]{first + i, made[k], made[k + 1]};
            }
            current = parent[i][current];
        }
        return next;
    }

    /** 左右の列が繋がるか。同じ行か、その上下に同じ色があればよい */
    private static boolean connects(Colour[] left, Colour[] right) {
        for (int r = 0; r < ROWS; r++) {
            Colour colour = left[r];
            if (right[r] == colour) continue;
            if (r > 0 && right[r - 1] == colour) continue;
            if (r < ROWS - 1 && right[r + 1] == colour) continue;
            return false;
        }
        return true;
    }

    private static Colour[] permute(Colour[] column, int[] order) {
        Colour[] result = new Colour[ROWS];
        for (int r = 0; r < ROWS; r++) result[r] = column[order[r]];
        return result;
    }

    /**
     * 並べ替えに要る入れ替えの最小手。
     * {手数, 行, 行, 行, 行, ...} で返す。
     *
     * 行き先を巡回に分けると、長さ n の巡回は n-1 回で片付く
     */
    private static int[] minimumSwaps(Colour[] from, Colour[] to) {
        int[] destination = new int[ROWS];
        for (int i = 0; i < ROWS; i++) destination[indexOf(from, to[i])] = i;

        boolean[] visited = new boolean[ROWS];
        List<Integer> pairs = new ArrayList<>();
        int cost = 0;

        for (int i = 0; i < ROWS; i++) {
            if (visited[i]) continue;

            List<Integer> cycle = new ArrayList<>();
            int cursor = i;
            while (!visited[cursor]) {
                visited[cursor] = true;
                cycle.add(cursor);
                cursor = destination[cursor];
            }
            if (cycle.size() <= 1) continue;

            cost += cycle.size() - 1;
            for (int k = 1; k < cycle.size(); k++) {
                pairs.add(cycle.get(0));
                pairs.add(cycle.get(k));
            }
        }

        int[] result = new int[pairs.size() + 1];
        result[0] = cost;
        for (int i = 0; i < pairs.size(); i++) result[i + 1] = pairs.get(i);
        return result;
    }

    private static int indexOf(Colour[] column, Colour colour) {
        for (int i = 0; i < ROWS; i++) {
            if (column[i] == colour) return i;
        }
        return 0;
    }

    /** 4 行の並べ替え 24 通り */
    private static int[][] permutations() {
        List<int[]> result = new ArrayList<>();
        collect(result, new int[]{0, 1, 2, 3}, 0);
        return result.toArray(new int[0][]);
    }

    private static void collect(List<int[]> result, int[] order, int at) {
        if (at == ROWS) {
            result.add(order.clone());
            return;
        }
        for (int i = at; i < ROWS; i++) {
            int keep = order[at];
            order[at] = order[i];
            order[i] = keep;

            collect(result, order, at + 1);

            keep = order[at];
            order[at] = order[i];
            order[i] = keep;
        }
    }

    /**
     * DNA の色。
     *
     * 同じ赤でも、真ん中の列は concrete、両端の列は terracotta で
     * 見た目の濃さが違うため、頼りになるのは品の名前の色。
     * その色が § 記号で来るか、書式として来るかは繋ぎ先次第なので、
     * どちらでも読めるようにし、最後は品そのものの種類から拾う
     */
    private static Colour colourOf(ItemStack stack) {
        if (stack.isEmpty()) return null;

        Component name = stack.getHoverName();

        Colour byCode = fromLegacyCode(name.getString());
        if (byCode != null) return byCode;

        Colour byStyle = fromStyle(name);
        if (byStyle != null) return byStyle;

        return fromItemKind(stack);
    }

    /** 名前に埋まっている § 記号から拾う */
    private static Colour fromLegacyCode(String name) {
        for (int at = name.indexOf('§'); at >= 0 && at + 1 < name.length();
             at = name.indexOf('§', at + 2)) {
            Colour colour = switch (name.charAt(at + 1)) {
                case 'c' -> Colour.RED;
                case 'e' -> Colour.YELLOW;
                case '9' -> Colour.BLUE;
                case 'a' -> Colour.GREEN;
                default -> null;
            };
            if (colour != null) return colour;
        }
        return null;
    }

    /** 名前に付いている書式の色から拾う */
    private static Colour fromStyle(Component name) {
        Colour[] found = new Colour[1];
        name.visit((FormattedText.StyledContentConsumer<Object>) (style, text) -> {
            if (found[0] != null || text.isBlank()) return Optional.empty();

            TextColor colour = style.getColor();
            if (colour != null) found[0] = fromRgb(colour.getValue());
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static Colour fromRgb(int rgb) {
        return switch (rgb) {
            case 0xFF5555 -> Colour.RED;
            case 0xFFFF55 -> Colour.YELLOW;
            case 0x5555FF -> Colour.BLUE;
            case 0x55FF55 -> Colour.GREEN;
            default -> null;
        };
    }

    /** 品そのものの種類から拾う。"red_concrete" のように色が名前に入っている */
    private static Colour fromItemKind(ItemStack stack) {
        String id = stack.getItem().getDescriptionId();
        String kind = id.substring(id.lastIndexOf('.') + 1);

        // light_blue や lime を巻き込まないよう、色は頭から見る
        if (kind.startsWith("red_")) return Colour.RED;
        if (kind.startsWith("yellow_")) return Colour.YELLOW;
        if (kind.startsWith("blue_")) return Colour.BLUE;
        if (kind.startsWith("green_")) return Colour.GREEN;
        return null;
    }

    private enum Colour {
        RED,
        YELLOW,
        BLUE,
        GREEN
    }
}
