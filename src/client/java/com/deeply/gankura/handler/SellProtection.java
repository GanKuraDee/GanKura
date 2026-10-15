package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ItemRarity;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.NotificationUtils;
import com.deeply.gankura.util.SkyblockItemId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Set;

/**
 * レアなアイテムを、うっかり NPC に売ってしまうのを防ぐ。
 *
 * NPC ショップを開いている間は、自分のインベントリのアイテムを押すとそのまま売れる。
 * 設定したレアリティ以上のアイテムか、設定で選べば Recombobulate 済みのアイテムを押したときだけ、
 * その操作をサーバーへ送らずに止める。
 * 設定で許していれば、同じ品を3回続けて押したときだけ通す
 */
public final class SellProtection {

    // 自分のインベントリの枠数。開いているメニューの枠のうち、後ろのこれだけが自分の分
    private static final int PLAYER_SLOTS = 36;
    // NPC ショップの売却の窓口は、最下段の真ん中にある。メニューの最後の枠から数えて5つ前
    private static final int SELL_SLOT_FROM_END = 5;
    // 窓口の説明文の最終行。売る前は売り方の案内、売った後は買い戻しの案内になる
    private static final Set<String> SELL_SLOT_LAST_LINES = Set.of("them to this Shop!", "Click to buyback!");
    // 同じ枠をこの回数続けて押したら、売るのを通す
    private static final int CLICKS_TO_SELL = 3;
    // 続けて押したとみなすクリックの間隔
    private static final long CLICK_INTERVAL_MS = 500;

    // 続けて押されている枠と、そのときの品、押された回数と最後に押された時刻
    private static Slot clickSlot = null;
    private static ItemStack clickStack = ItemStack.EMPTY;
    private static int clickCount = 0;
    private static long lastClickAt = 0;
    // 3回目で通した後、この時刻までは同じ枠への操作を通し続ける
    private static long passUntil = 0;

    private SellProtection() {
    }

    /** そのクリックを止めるか。止めるときは知らせも出す */
    public static boolean shouldBlock(AbstractContainerMenu menu, Slot slot, ContainerInput input) {
        if (!ModConfig.Interface.enableSellProtection || !GameState.Server.isSkyblock()) return false;
        // 売れるのは自分のインベントリのアイテムだけ。ショップの棚を押すのは買う操作なので止めない
        if (slot == null || !slot.hasItem() || !(slot.container instanceof Inventory)) return false;
        if (!isNpcShop(menu)) return false;

        ItemStack stack = slot.getItem();
        ItemRarity rarity = ItemRarity.of(stack);
        boolean rare = rarity != null && rarity.atLeast(ModConfig.Interface.sellProtectionMinRarity);
        boolean recombobulated = ModConfig.Interface.sellProtectRecombobulated && isRecombobulated(stack);
        if (!rare && !recombobulated) return false;

        long now = System.currentTimeMillis();
        boolean allowTripleClick = ModConfig.Interface.sellProtectionAllowTripleClick;
        boolean sameTarget = slot == clickSlot && ItemStack.isSameItemSameComponents(stack, clickStack);

        // 3回目で通した直後。Minecraft は続けて押すと、離した瞬間に「同じ品を集める」操作も送ってくるので、
        // 少しの間は同じ枠への操作をまとめて通す
        if (allowTripleClick && sameTarget && now <= passUntil) return false;

        // その「同じ品を集める」操作は、押した回数には数えずに止める
        if (input == ContainerInput.PICKUP_ALL) return true;

        if (sameTarget && now - lastClickAt <= CLICK_INTERVAL_MS) {
            clickCount++;
        } else {
            clickSlot = slot;
            clickStack = stack.copy();
            clickCount = 1;
        }
        lastClickAt = now;

        if (allowTripleClick && clickCount >= CLICKS_TO_SELL) {
            clickCount = 0;
            passUntil = now + CLICK_INTERVAL_MS;
            return false;
        }

        if (allowTripleClick) {
            // 押すたびに、売れるまであと何回かを知らせる
            warn(stack, rarity, recombobulated, CLICKS_TO_SELL - clickCount);
        } else if (clickCount == 1) {
            // 何回押しても売れないので、知らせは押し始めの1回だけにする
            warn(stack, rarity, recombobulated, 0);
        }
        return true;
    }

    /** Recombobulator 3000 を使った品か。使うと属性の rarity_upgrades が 1 になる */
    private static boolean isRecombobulated(ItemStack stack) {
        return SkyblockItemId.attribute(stack, "rarity_upgrades") > 0;
    }

    private static boolean isNpcShop(AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - PLAYER_SLOTS;
        if (containerSlots < SELL_SLOT_FROM_END) return false;

        ItemLore lore = menu.getSlot(containerSlots - SELL_SLOT_FROM_END).getItem().get(DataComponents.LORE);
        if (lore == null) return false;

        List<Component> lines = lore.lines();
        return !lines.isEmpty() && SELL_SLOT_LAST_LINES.contains(lines.getLast().getString().trim());
    }

    // clicksLeft は売れるまであと何回押せばよいか。0 なら押しても売れないので書かない
    private static void warn(ItemStack stack, ItemRarity rarity, boolean recombobulated, int clicksLeft) {
        Minecraft client = Minecraft.getInstance();
        // 止めた理由を括弧に書く。Recombobulate 済みならそれを添える
        String reason = rarity == null ? "§dRecombobulated"
                : rarity.colored() + (recombobulated ? "§c, §dRecombobulated" : "");
        String hint = clicksLeft <= 0 ? ""
                : " §7Click §e" + clicksLeft + " §7more " + (clicksLeft == 1 ? "time" : "times") + " to sell it anyway.";
        Component message = Component.literal("§cStopped selling ")
                .append(stack.getHoverName())
                .append(Component.literal(" §c(" + reason + "§c) to the NPC." + hint));
        NotificationUtils.sendSystemChat(client, message);
        NotificationUtils.playSound(client, SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
    }
}
