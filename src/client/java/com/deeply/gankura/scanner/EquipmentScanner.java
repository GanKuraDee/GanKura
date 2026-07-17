package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// これはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
// メニューはラージチェスト(6行9列, スロット0-53)と同じ構造で、Equipmentは上から2行目〜5行目の2列目
// (スロット10, 19, 28, 37) に上から順に並んでいる。
// "(1/2) Equipment Sets" のようにページ番号付きで表示されるメニューでも、5行目の各列に
// "Slot X: Equipped" と表示されている列(=現在有効なセット)を探し、その列の上4行を同様に読み取る。
// "/loadouts" (Loadouts) メニューはロードアウト切り替えスロットをクリックした直後、サーバーから
// コンテナ内容の同期パケットが届くまでスロットの中身が古いロードアウトのままになる。
// そのため継続スキャンの対象外とし、クリック直後はLoadoutsClickMixin経由のresetToUnknownで
// 一旦Barrier(Unknown)化するだけにとどめ、実際の読み取りはLoadoutsContentSyncMixin経由の
// onLoadoutsContentsSynced(サーバーから内容が同期された瞬間)で行う。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final Pattern EQUIPMENT_SETS_TITLE_PATTERN = Pattern.compile("\\(\\d+/\\d+\\)\\s*Equipment Sets");
    private static final Pattern EQUIPPED_SLOT_PATTERN = Pattern.compile("Slot \\d+:\\s*Equipped", Pattern.CASE_INSENSITIVE);
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};
    // ラージチェスト(6行9列)のうち、Equipment Setsの各セットが1列を占める
    private static final int MENU_WIDTH = 9;
    private static final int EQUIPPED_LABEL_ROW = 4; // 5行目(0-index)

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        ScreenHandler handler = screen.getScreenHandler();

        if (title.contains(EQUIPMENT_TITLE)) {
            scanFixedSlots(handler, client);
        } else if (EQUIPMENT_SETS_TITLE_PATTERN.matcher(title).find()) {
            scanEquipmentSets(handler, client);
        }
    }

    private static void scanFixedSlots(ScreenHandler handler, MinecraftClient client) {
        if (handler.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int slotIndex : EQUIPMENT_SLOTS) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        commit(found, client);
    }

    // "Equipment Sets" メニューの5行目を左から走査し、"Slot X: Equipped" と表示されている
    // 列(=現在有効なセット)を探して、その列の上4行(1〜4行目)を上から順に読み取る
    private static void scanEquipmentSets(ScreenHandler handler, MinecraftClient client) {
        int labelRowStart = EQUIPPED_LABEL_ROW * MENU_WIDTH;
        if (handler.slots.size() <= labelRowStart + MENU_WIDTH - 1) return;

        int activeColumn = -1;
        for (int col = 0; col < MENU_WIDTH; col++) {
            ItemStack stack = handler.slots.get(labelRowStart + col).getStack();
            if (stack.isEmpty()) continue;
            if (EQUIPPED_SLOT_PATTERN.matcher(stack.getName().getString()).find()) {
                activeColumn = col;
                break;
            }
        }
        if (activeColumn == -1) return;

        List<ItemStack> found = new ArrayList<>();
        for (int row = 0; row < 4; row++) {
            ItemStack stack = handler.slots.get(row * MENU_WIDTH + activeColumn).getStack();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        commit(found, client);
    }

    private static void commit(List<ItemStack> found, MinecraftClient client) {
        EquipmentState.items = found;
        // CLIENT_STOPPING 時には既にワールドから切断済みでレジストリ情報が失われている場合があるため、
        // スキャンできた時点でその都度保存しておく
        if (client.world != null) {
            EquipmentState.save(client.world.getRegistryManager());
        }
    }

    // Loadoutsメニューのロードアウト切り替えスロットがクリックされた瞬間に呼び出される。
    // 実際の読み取りはまだ行わず、切り替え中であることを示すため全EquipmentスロットをBarrier(Unknown)化するだけにとどめる。
    public static void resetToUnknown() {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        List<ItemStack> barrier = new ArrayList<>();
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            barrier.add(new ItemStack(Items.BARRIER));
        }
        EquipmentState.items = barrier;
    }

    // Loadoutsメニューが開かれている間にサーバーからコンテナ内容の同期パケットが届いた瞬間に呼び出される。
    // この時点でスロットの中身は実際に切り替わった後の最新の状態が保証されているため、
    // まず全EquipmentスロットをBarrier(Unknown)化した上で、読み取れたスロットだけを実際のアイテムで置き換える。
    public static void onLoadoutsContentsSynced(ScreenHandler handler) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (handler.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            found.add(new ItemStack(Items.BARRIER));
        }
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            ItemStack stack = handler.slots.get(EQUIPMENT_SLOTS[i]).getStack();
            if (!stack.isEmpty()) {
                found.set(i, stack.copy());
            }
        }

        EquipmentState.items = found;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            EquipmentState.save(client.world.getRegistryManager());
        }
    }
}
