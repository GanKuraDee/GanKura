package com.deeply.gankura.gui;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix3x2fStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Inventory Button のアイコン。NotEnoughUpdates の同機能から移植した。
 *
 * アイコンは文字列1つで表す。
 *   "extra:<名前>" … Mod が持っている 16x16 の絵
 *   "skull:<テクスチャID>" … その見た目のプレイヤーヘッド
 *   それ以外 … アイテムID("minecraft:chest" など)
 *
 * NotEnoughUpdates のプリセットや共有コードは 1.8 時代のアイテム名("WORKBENCH" など)を
 * 使っているので、読むときだけ今の名前に読み替える
 */
public final class InventoryButtonIcons {

    public static final String EXTRA_PREFIX = "extra:";
    public static final String SKULL_PREFIX = "skull:";

    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    // skull は本来のアイテムより一回り小さく見えるので、少しだけ大きく描く
    private static final float SKULL_SCALE = 1.2F;

    // 1.8 時代のアイテム名から今のIDへ。NotEnoughUpdates 由来の設定を読むためだけに使う
    private static final Map<String, String> LEGACY_ITEMS = new HashMap<>();

    static {
        LEGACY_ITEMS.put("WORKBENCH", "minecraft:crafting_table");
        LEGACY_ITEMS.put("BOOK_AND_QUILL", "minecraft:writable_book");
        LEGACY_ITEMS.put("GOLD_BARDING", "minecraft:golden_horse_armor");
        LEGACY_ITEMS.put("IRON_BARDING", "minecraft:iron_horse_armor");
        LEGACY_ITEMS.put("DIAMOND_BARDING", "minecraft:diamond_horse_armor");
        LEGACY_ITEMS.put("EMPTY_MAP", "minecraft:map");
        LEGACY_ITEMS.put("RAW_FISH", "minecraft:cod");
        LEGACY_ITEMS.put("COOKED_FISH", "minecraft:cooked_cod");
        LEGACY_ITEMS.put("COMMAND", "minecraft:command_block");
        LEGACY_ITEMS.put("SKULL_ITEM", "minecraft:player_head");
        LEGACY_ITEMS.put("WATCH", "minecraft:clock");
        LEGACY_ITEMS.put("SIGN", "minecraft:oak_sign");
        LEGACY_ITEMS.put("EXP_BOTTLE", "minecraft:experience_bottle");
        LEGACY_ITEMS.put("FIREBALL", "minecraft:fire_charge");
        LEGACY_ITEMS.put("NETHER_STALK", "minecraft:nether_wart");
        LEGACY_ITEMS.put("ENDER_PORTAL_FRAME", "minecraft:end_portal_frame");
        LEGACY_ITEMS.put("STORAGE_MINECART", "minecraft:chest_minecart");
        LEGACY_ITEMS.put("SKYBLOCK_MENU", EXTRA_PREFIX + "skyblock_menu");
    }

    // 一覧の先頭に出すアイコン。よく使うものを探さずに選べるようにする
    private static final List<String> PRIORITISED = List.of(
            "minecraft:crafting_table", "minecraft:leather_chestplate", "minecraft:chest",
            "minecraft:bone", "minecraft:ender_chest", "minecraft:golden_horse_armor",
            "minecraft:compass", "minecraft:gold_block", "minecraft:map", "minecraft:cod",
            "minecraft:fishing_rod", "minecraft:emerald", "minecraft:iron_sword",
            "minecraft:potion", "minecraft:nether_star", "minecraft:painting",
            "minecraft:command_block", "minecraft:book");

    /**
     * スキンで選べるヘッド。NotEnoughUpdates の一覧をそのまま持ってきている。
     * キーは探すときの手掛かりの言葉で、値がアイコンの文字列
     */
    private static final LinkedHashMap<String, String> SKULL_ICONS = new LinkedHashMap<>();

    static {
        SKULL_ICONS.put("personal bank", "skull:e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852");
        SKULL_ICONS.put("skyblock hub", "skull:d7cc6687423d0570d556ac53e0676cb563bbdd9717cd8269bdebed6f6d4e7bf8");
        SKULL_ICONS.put("private island", "skull:c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56");
        SKULL_ICONS.put("castle", "skull:f4559d75464b2e40a518e4de8e6cf3085f0a3ca0b1b7012614c4cd96fed60378");
        SKULL_ICONS.put("sirius shack", "skull:7ab83858ebc8ee85c3e54ab13aabfcc1ef2ad446d6a900e471c3f33b78906a5b");
        SKULL_ICONS.put("crypts", "skull:25d2f31ba162fe6272e831aed17f53213db6fa1c4cbe4fc827f3963cc98b9");
        SKULL_ICONS.put("spiders den", "skull:c754318a3376f470e481dfcd6c83a59aa690ad4b4dd7577fdad1c2ef08d8aee6");
        SKULL_ICONS.put("top of the nest", "skull:9d7e3b19ac4f3dee9c5677c135333b9d35a7f568b63d1ef4ada4b068b5a25");
        SKULL_ICONS.put("the end", "skull:7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5");
        SKULL_ICONS.put("the end dragons nest", "skull:a1cd6d2d03f135e7c6b5d6cdae1b3a68743db4eb749faf7341e9fb347aa283b");
        SKULL_ICONS.put("the park", "skull:a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b");
        SKULL_ICONS.put("the park jungle", "skull:79ca3540621c1c79c32bf42438708ff1f5f7d0af9b14a074731107edfeb691c");
        SKULL_ICONS.put("the park howling cave", "skull:1832d53997b451635c9cf9004b0f22bb3d99ab5a093942b5b5f6bb4e4de47065");
        SKULL_ICONS.put("gold mines", "skull:73bc965d579c3c6039f0a17eb7c2e6faf538c7a5de8e60ec7a719360d0a857a9");
        SKULL_ICONS.put("deep caverns", "skull:569a1f114151b4521373f34bc14c2963a5011cdc25a6554c48c708cd96ebfc");
        SKULL_ICONS.put("the barn", "skull:4d3a6bd98ac1833c664c4909ff8d2dc62ce887bdcf3cc5b3848651ae5af6b");
        SKULL_ICONS.put("mushroom desert", "skull:6b20b23c1aa2be0270f016b4c90d6ee6b8330a17cfef87869d6ad60b2ffbf3b5");
        SKULL_ICONS.put("dungeon hub", "skull:9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d");
        SKULL_ICONS.put("dwarven mines", "skull:51539dddf9ed255ece6348193cd75012c82c93aec381f05572cecf7379711b3b");
        SKULL_ICONS.put("hotm heart of the mountain", "skull:86f06eaa3004aeed09b3d5b45d976de584e691c0e9cade133635de93d23b9edb");
        SKULL_ICONS.put("bazaar dude", "skull:c232e3820897429157619b0ee099fec0628f602fff12b695de54aef11d923ad7");
        SKULL_ICONS.put("museum", "skull:438cf3f8e54afc3b3f91d20a49f324dca1486007fe545399055524c17941f4dc");
        SKULL_ICONS.put("crystal hollows", "skull:21dbe30b027acbceb612563bd877cd7ebb719ea6ed1399027dcee58bb9049d4a");
        SKULL_ICONS.put("dwarven forge", "skull:5cbd9f5ec1ed007259996491e69ff649a3106cf920227b1bb3a71ee7a89863f");
        SKULL_ICONS.put("forgotton skull", "skull:6becc645f129c8bc2faa4d8145481fab11ad2ee75749d628dcd999aa94e7");
        SKULL_ICONS.put("crystal nucleus", "skull:34d42f9c461cee1997b67bf3610c6411bf852b9e5db607bbf626527cfb42912c");
        SKULL_ICONS.put("void sepulture", "skull:eb07594e2df273921a77c101d0bfdfa1115abed5b9b2029eb496ceba9bdbb4b3");
        SKULL_ICONS.put("crimson isle", "skull:c3687e25c632bce8aa61e0d64c24e694c3eea629ea944f4cf30dcfb4fbce071");
        SKULL_ICONS.put("trapper den", "skull:6102f82148461ced1f7b62e326eb2db3a94a33cba81d4281452af4d8aeca4991");
        SKULL_ICONS.put("arachne sanctuary", "skull:35e248da2e108f09813a6b848a0fcef111300978180eda41d3d1a7a8e4dba3c3");
        SKULL_ICONS.put("garden", "skull:f4880d2c1e7b86e87522e20882656f45bafd42f94932b2c5e0d6ecaa490cb4c");
        SKULL_ICONS.put("winter", "skull:6dd663136cafa11806fdbca6b596afd85166b4ec02142c8d5ac8941d89ab7");
        SKULL_ICONS.put("wizard tower", "skull:838564e28aba98301dbda5fafd86d1da4e2eaeef12ea94dcf440b883e559311c");
        SKULL_ICONS.put("dwarven mines base camp", "skull:2461ec3bd654f62ca9a393a32629e21b4e497c877d3f3380bcf2db0e20fc0244");
    }

    // Mod が持っている絵。ファイル名は extraicons.json から読む
    private static LinkedHashMap<String, String> extraIcons = null;

    // 同じアイコンを毎フレーム組み立て直さないための控え
    private static final Map<String, ItemStack> stackCache = new HashMap<>();

    private InventoryButtonIcons() {
    }

    // -------------------------------------------------- 描画

    public static void render(GuiGraphicsExtractor graphics, String icon, int x, int y) {
        String resolved = resolve(icon);
        if (resolved.isEmpty()) return;

        if (resolved.startsWith(EXTRA_PREFIX)) {
            Identifier texture = Identifier.fromNamespaceAndPath("gankura",
                    "textures/invbuttons/extraicons/" + resolved.substring(EXTRA_PREFIX.length()) + ".png");
            graphics.blit(PIPELINE, texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            return;
        }

        ItemStack stack = getStack(resolved);
        if (stack.isEmpty()) return;

        if (!resolved.startsWith(SKULL_PREFIX)) {
            graphics.item(stack, x, y);
            return;
        }

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + 8.0F, y + 8.0F);
        pose.scale(SKULL_SCALE, SKULL_SCALE);
        pose.translate(-8.0F, -8.0F);
        graphics.item(stack, 0, 0);
        pose.popMatrix();
    }

    // -------------------------------------------------- アイコンの中身

    // 古い名前で書かれていたら今の名前に直す
    public static String resolve(String icon) {
        if (icon == null) return "";

        String trimmed = icon.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.startsWith(EXTRA_PREFIX) || trimmed.startsWith(SKULL_PREFIX)) return trimmed;

        String legacy = LEGACY_ITEMS.get(trimmed.toUpperCase(Locale.ROOT));
        if (legacy != null) return legacy;

        // 1.8 の名前は大文字。名前空間が無ければ minecraft のものとして扱う
        String id = trimmed.toLowerCase(Locale.ROOT);
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }

    /**
     * アイコンが表しているアイテム。extra: の絵は画像なので空を返す。
     * 見つからないIDのときも空になるので、描く側は何も描かない
     */
    public static ItemStack getStack(String icon) {
        String resolved = resolve(icon);
        if (resolved.isEmpty() || resolved.startsWith(EXTRA_PREFIX)) return ItemStack.EMPTY;

        ItemStack cached = stackCache.get(resolved);
        if (cached != null) return cached;

        ItemStack stack = resolved.startsWith(SKULL_PREFIX)
                ? createSkull(resolved.substring(SKULL_PREFIX.length()))
                : createItem(resolved);

        stackCache.put(resolved, stack);
        return stack;
    }

    private static ItemStack createItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // テクスチャIDだけを持つプレイヤーヘッド。名前とUUIDは見た目に関係しないので、
    // 同じテクスチャなら毎回同じ値になるように作る
    private static ItemStack createSkull(String texture) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        PropertyMap properties = new PropertyMap(
                ImmutableMultimap.of("textures", new Property("textures", encoded)));

        UUID uuid = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, uuid.toString().substring(0, 16), properties);

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return stack;
    }

    // -------------------------------------------------- 一覧と検索

    // アイテム・ヘッド・Mod が持つ絵の3種類。エディタのタブと同じ並び
    public enum IconType {
        ITEM, SKULL, EXTRA
    }

    public static List<String> search(IconType type, String query) {
        String trimmed = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return switch (type) {
            case ITEM -> searchItems(trimmed);
            case SKULL -> searchNamed(SKULL_ICONS, trimmed);
            case EXTRA -> searchNamed(extraIcons(), trimmed);
        };
    }

    private static List<String> searchItems(String query) {
        List<String> found = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) continue;

            if (!query.isEmpty() && !matches(id, item, query)) continue;
            found.add(id.toString());
        }

        found.sort(PRIORITY_ORDER);
        return found;
    }

    // IDと表示名のどちらで探しても見つかるようにする
    private static boolean matches(Identifier id, Item item, String query) {
        if (id.getPath().replace('_', ' ').contains(query)) return true;
        return new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static final Comparator<String> PRIORITY_ORDER = (left, right) -> {
        int leftIndex = PRIORITISED.indexOf(left);
        int rightIndex = PRIORITISED.indexOf(right);

        if (leftIndex >= 0 && rightIndex >= 0) return Integer.compare(leftIndex, rightIndex);
        if (leftIndex >= 0) return -1;
        if (rightIndex >= 0) return 1;
        return left.compareTo(right);
    };

    // 手掛かりの言葉で探す。並びは書いた順のままにして、いつも同じ場所に出るようにする
    private static List<String> searchNamed(Map<String, String> icons, String query) {
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, String> entry : icons.entrySet()) {
            if (query.isEmpty() || entry.getKey().contains(query)) found.add(entry.getValue());
        }
        return found;
    }

    private static LinkedHashMap<String, String> extraIcons() {
        if (extraIcons != null) return extraIcons;

        extraIcons = new LinkedHashMap<>();
        try (InputStream stream = InventoryButtonIcons.class
                .getResourceAsStream("/assets/gankura/invbuttons/extraicons.json")) {
            if (stream != null) {
                JsonObject json = new Gson().fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        extraIcons.put(entry.getKey(), EXTRA_PREFIX + entry.getValue().getAsString());
                    }
                }
            }
        } catch (Exception ignored) {
            // 絵が読めないだけなので、一覧が空になるに留める
        }
        return extraIcons;
    }
}
