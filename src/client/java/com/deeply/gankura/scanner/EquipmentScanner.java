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

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// これはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
// メニューはラージチェスト(6行9列, スロット0-53)と同じ構造で、Equipmentは上から2行目〜5行目の2列目
// (スロット10, 19, 28, 37) に上から順に並んでいる。
// "/loadouts" (Loadouts) メニューはロードアウト切り替えスロットをクリックした直後、サーバーから
// コンテナ内容の同期パケットが届くまでスロットの中身が古いロードアウトのままになる。
// そのため継続スキャンの対象外とし、クリック直後はLoadoutsClickMixin経由のresetToUnknownで
// 一旦Barrier(Unknown)化するだけにとどめ、実際の読み取りはLoadoutsContentSyncMixin経由の
// onLoadoutsContentsSynced(サーバーから内容が同期された瞬間)で行う。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.contains(EQUIPMENT_TITLE)) return;

        ScreenHandler handler = screen.getScreenHandler();
        if (handler.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int slotIndex : EQUIPMENT_SLOTS) {
            ItemStack stack = handler.slots.get(slotIndex).getStack();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

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
