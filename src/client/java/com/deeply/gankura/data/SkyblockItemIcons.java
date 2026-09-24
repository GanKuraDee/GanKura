package com.deeply.gankura.data;

import com.deeply.gankura.gui.InventoryButtonIcons;
import com.deeply.gankura.util.JsonFetch;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SkyBlock のアイテム ID から、画面に描くためのアイテムを作る。
 *
 * 見た目は NotEnoughUpdates が集めている一覧から取る。一覧は 1.8 の書き方
 * （"minecraft:dye" と damage 4 で Lapis Lazuli、のような形）なので、
 * Minecraft が古いワールドを読むときに使う変換にそのまま通して、今のアイテムに直す。
 * 頭のアイテムはテクスチャを抜き出してプレイヤーヘッドを作り、
 * エンチャントの付いたものは光らせる。
 *
 * 宝石のように Hypixel がリソースパックのモデルで描いている品は、1.8 の書き方では
 * 紙の仮置きになっている。パックが読み込まれていればそのモデルを使い、無ければ仮置きのまま描く
 */
public final class SkyblockItemIcons {

    private static final Logger LOGGER = LoggerFactory.getLogger("GanKura/SkyblockItemIcons");

    private static final String ITEM_URL =
            "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/%s.json";

    // 1.8 の書き方で書かれたデータの版。変換はここから今の版まで進める
    private static final int LEGACY_DATA_VERSION = 99;

    // 頭のテクスチャ。1.8 の書き方では SkullOwner の中に Value:"<base64>" で入っている
    private static final Pattern SKULL_VALUE = Pattern.compile("Value:\\s*\"([A-Za-z0-9+/=]+)\"");
    // テクスチャの URL の末尾が、プレイヤーヘッドを作るのに使う ID
    private static final Pattern TEXTURE_ID = Pattern.compile("textures\\.minecraft\\.net/texture/([0-9a-fA-F]+)");
    // エンチャントの付いた品は、中身が空でも ench が書かれている
    private static final Pattern ENCHANTED = Pattern.compile("\\bench:\\s*\\[");
    // Hypixel のリソースパックで描く品のモデル
    private static final Pattern ITEM_MODEL = Pattern.compile("ItemModel:\\s*\"([^\"]+)\"");
    // 必ず存在しないモデルの ID。欠落モデルがどれなのかを知るためだけに使う
    private static final Identifier NO_MODEL = Identifier.fromNamespaceAndPath("gankura", "no_such_item_model");

    /** 一覧から読んだ、アイテムを作るのに要る分だけ */
    private record Legacy(String itemId, int damage, String skullTexture, boolean glint, Identifier itemModel) {
    }

    // 一覧に載っていないことも答えのうちなので、それ用の目印を控えておく
    private static final Legacy MISSING = new Legacy("", 0, null, false, null);

    private static final Map<String, Legacy> legacy = new ConcurrentHashMap<>();
    private static final Set<String> asked = ConcurrentHashMap.newKeySet();
    // 変換は描画の側でだけ行い、結果を控えておく
    private static final Map<String, ItemStack> stacks = new ConcurrentHashMap<>();
    // リソースパックのモデルを付けたもの。パックが無いときは使わない
    private static final Map<String, ItemStack> modeled = new ConcurrentHashMap<>();

    private SkyblockItemIcons() {
    }

    /** まだ用意できていない。裏で一覧を取りに行っている */
    public static final ItemStack LOADING = ItemStack.EMPTY;

    /**
     * その品の見た目。
     *
     * まだ手元に無ければ裏で取りに行き、その場は {@link #LOADING} を返す。
     * 一覧に載っていない品や、今のアイテムに直せなかった品は null
     */
    public static ItemStack of(String skyblockId) {
        Legacy data = legacy.get(skyblockId);
        if (data == null) {
            if (asked.add(skyblockId)) JsonFetch.run(() -> fetch(skyblockId));
            return LOADING;
        }
        if (data == MISSING) return null;

        // パックは SkyBlock への出入りで読み込み直されるので、あるかどうかは描くたびに見る
        if (data.itemModel() != null && hasModel(data.itemModel())) {
            return modeled.computeIfAbsent(skyblockId, id -> withModel(data));
        }

        ItemStack stack = stacks.computeIfAbsent(skyblockId, id -> build(data));
        return stack.isEmpty() ? null : stack;
    }

    /** そのモデルが今読み込まれているか */
    private static boolean hasModel(Identifier id) {
        var models = Minecraft.getInstance().getModelManager();
        ItemModel missing = models.getItemModel(NO_MODEL);
        return models.getItemModel(id) != missing;
    }

    private static ItemStack withModel(Legacy data) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, data.itemModel());
        if (data.glint()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static void fetch(String skyblockId) {
        // "INK_SACK:3" のように ":" の付く ID は、一覧では "-" に置き換えたファイル名になっている
        String file = skyblockId.replace(':', '-');
        try (JsonReader reader = JsonFetch.open(String.format(ITEM_URL, file))) {
            if (reader == null) {
                legacy.put(skyblockId, MISSING);
                return;
            }
            legacy.put(skyblockId, parse(JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (Exception e) {
            // 通信に失敗しただけかもしれないので、覚えずにもう一度聞けるようにする
            asked.remove(skyblockId);
            LOGGER.warn("Could not read the item {}: {}", skyblockId, e.toString());
        }
    }

    private static Legacy parse(JsonObject root) {
        if (!root.has("itemid")) return MISSING;

        String itemId = root.get("itemid").getAsString();
        int damage = root.has("damage") ? root.get("damage").getAsInt() : 0;
        String nbt = root.has("nbttag") ? root.get("nbttag").getAsString() : "";

        Matcher model = ITEM_MODEL.matcher(nbt);
        Identifier itemModel = model.find() ? Identifier.tryParse(model.group(1)) : null;

        return new Legacy(itemId, damage, skullTexture(nbt), ENCHANTED.matcher(nbt).find(), itemModel);
    }

    /** 頭のアイテムなら、見た目のテクスチャ ID。それ以外は null */
    private static String skullTexture(String nbt) {
        Matcher value = SKULL_VALUE.matcher(nbt);
        if (!value.find()) return null;

        try {
            String json = new String(Base64.getDecoder().decode(value.group(1)), StandardCharsets.UTF_8);
            Matcher id = TEXTURE_ID.matcher(json);
            return id.find() ? id.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ItemStack build(Legacy data) {
        ItemStack stack = data.skullTexture() != null
                ? InventoryButtonIcons.getStack(InventoryButtonIcons.SKULL_PREFIX + data.skullTexture()).copy()
                : new ItemStack(modernItem(data.itemId(), data.damage()));

        if (stack.isEmpty() || stack.is(Items.AIR)) return ItemStack.EMPTY;
        if (data.glint()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * 1.8 の ID と damage を今のアイテムに直す。
     * 色違いの羊毛や染料のように damage で見分けていた品も、ここで別々のアイテムになる
     */
    private static Item modernItem(String itemId, int damage) {
        CompoundTag old = new CompoundTag();
        old.putString("id", itemId);
        old.putByte("Count", (byte) 1);
        old.putShort("Damage", (short) damage);

        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        Dynamic<?> fixed = DataFixers.getDataFixer()
                .update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, old), LEGACY_DATA_VERSION, current);

        String id = fixed.get("id").asString("");
        Identifier key = Identifier.tryParse(id);
        return key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
    }
}
