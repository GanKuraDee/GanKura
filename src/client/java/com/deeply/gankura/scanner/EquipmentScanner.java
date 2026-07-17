package com.deeply.gankura.scanner;

import com.deeply.gankura.data.EquipmentState;
import com.deeply.gankura.data.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

// "/equipment" (Your Equipment and Stats) メニューを開いた時にNecklace/Cloak/Belt/Gloves等を読み取る。
// これはSkyblock独自のスロットでありAPI経由では取得できないため、メニューを開いた瞬間のスロットを直接スキャンする。
// メニューはラージチェスト(6行9列, スロット0-53)と同じ構造で、Equipmentは上から2行目〜5行目の2列目
// (スロット10, 19, 28, 37) に上から順に並んでいる。
// "/loadouts" (Loadouts) メニューはロードアウト切り替えスロットをクリックしない限り最新の内容が
// 保証されないため、こちらは継続スキャンの対象外とし、LoadoutsClickMixin経由のonLoadoutsSlotClickedで
// クリックされた瞬間にのみ読み取る。
public class EquipmentScanner {
    private static final String EQUIPMENT_TITLE = "Your Equipment and Stats";
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EquipmentScanner::scan);
    }

    private static void scan(Minecraft client) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.contains(EQUIPMENT_TITLE)) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (menu.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int slotIndex : EQUIPMENT_SLOTS) {
            ItemStack stack = menu.slots.get(slotIndex).getItem();
            found.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        EquipmentState.items = found;
        // CLIENT_STOPPING 時には既にワールドから切断済みでレジストリ情報が失われている場合があるため、
        // スキャンできた時点でその都度保存しておく
        if (client.level != null) {
            EquipmentState.save(client.level.registryAccess());
        }
    }

    // Loadoutsメニューのロードアウト切り替えスロットがクリックされた瞬間に呼び出される。
    // 切り替え直後はサーバーからの更新が届くまでスロットの中身が古いロードアウトのままになる場合があるため、
    // まず全EquipmentスロットをBarrier(Unknown)化した上で、その時点で読み取れたスロットだけを実際のアイテムで置き換える。
    public static void onLoadoutsSlotClicked(AbstractContainerMenu menu) {
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;
        if (menu.slots.size() <= EQUIPMENT_SLOTS[EQUIPMENT_SLOTS.length - 1]) return;

        List<ItemStack> found = new ArrayList<>();
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            found.add(new ItemStack(Items.BARRIER));
        }
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            ItemStack stack = menu.slots.get(EQUIPMENT_SLOTS[i]).getItem();
            if (!stack.isEmpty()) {
                found.set(i, stack.copy());
            }
        }

        EquipmentState.items = found;
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            EquipmentState.save(client.level.registryAccess());
        }
    }
}
