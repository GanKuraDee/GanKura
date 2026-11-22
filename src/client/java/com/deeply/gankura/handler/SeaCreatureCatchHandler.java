package com.deeply.gankura.handler;

import com.deeply.gankura.data.MobVisual;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.SeaCreatureCatches;
import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 自分が釣り上げた Sea Creature を、チャットの文言から扱う。
 *
 *   ・Mob Visuals に登録している種なら、エンティティの検知と同じタイトルを出す
 *   ・設定が入っていれば、長い文言を短い形に差し替える
 *
 * エンティティの検知だけだと、湧いた場所が読み込み範囲の外だったときや、
 * まだ正体(型やスキン)が分かっていない種を取りこぼす。
 * 釣り上げたときの文言は種ごとに決まっているので、そこからも知らせる。
 */
public class SeaCreatureCatchHandler {

    // 1度に2匹釣れた合図。Hypixel は釣り上げの文言の直前にこの行を出す
    private static final Pattern DOUBLE_HOOK_PATTERN =
            Pattern.compile("It's a Double Hook!(?: Woot woot!)?");
    // その合図を、直後の釣り上げの文言と同じ1回とみなす時間(ミリ秒)
    private static final long DOUBLE_HOOK_WINDOW_MS = 500;

    // Hypixel 公式サーバーリソースパックの Mob Types アイコン (assets/hypixel_skyblock/textures/gui/mobs.png)
    private static final Identifier HYPIXEL_MOBS_FONT_TEXTURE =
            Identifier.fromNamespaceAndPath("hypixel_skyblock", "textures/gui/mobs.png");
    private static final String AQUATIC_ICON = "\uE072";
    private static final String MAGMATIC_ICON = "\uE07D";
    // サーバーリソースパックが無効な環境でも豆腐(□)にならないようにする代替記号
    private static final String AQUATIC_FALLBACK_ICON = "\u2693"; // ⚓
    private static final String MAGMATIC_FALLBACK_ICON = "\u2646"; // ♆

    // Mithril Grubber は Small / Medium / Large / Bloated の4サイズで湧くが、扱いは1種
    private static final String MITHRIL_GRUBBER_NAME = "Mithril Grubber";

    // Mob Visuals に登録できる Sea Creature を、釣り上げ文言に出てくる名前から引く
    private static final Map<String, MobVisual> MOB_VISUALS = new HashMap<>();

    // Double Hook の合図を見た時刻。次に来る釣り上げの文言に付け直すために覚えておく
    private static long doubleHookMillis;

    static {
        for (MobVisual.SeaCreature target : MobVisual.SeaCreature.values()) {
            MOB_VISUALS.put(target.plainLabel(), target);
        }
    }

    /**
     * @return 元のメッセージを差し止めるなら true。短縮形に差し替えたときだけ true になる
     */
    public static boolean handleMessage(String unformattedMsg, Minecraft client) {
        // プレイヤーの発言は NetworkHandler で弾かれているので、ここには来ない
        if (DOUBLE_HOOK_PATTERN.matcher(unformattedMsg.trim()).matches()) {
            doubleHookMillis = System.currentTimeMillis();
            // 短縮するときは、釣り上げの文言に "x2" を付けてまとめるのでこの行は出さない
            return ModConfig.Fishing.shortenSeaCreatureMessage;
        }

        SeaCreatureCatches.Catch caught = SeaCreatureCatches.byMessage(unformattedMsg);
        if (caught == null) return false;

        // タイトルにも短縮メッセージにも要るので、ここで1度だけ取り出す
        boolean doubleHook = wasDoubleHook();

        MobVisual target = mobVisual(caught.name());
        // Mob Visuals で選んでいる種だけ知らせる。
        // 表示の全体トグル(Highlight など)とは切り離し、タイトルだけでも使えるようにする
        if (target != null && target.targets().contains(target)) {
            EntityHighlightManager.showSeaCreatureCatchTitle(client, target, doubleHook);
        }

        return shorten(client, caught, doubleHook);
    }

    private static MobVisual mobVisual(String name) {
        MobVisual target = MOB_VISUALS.get(name);
        if (target == null && name.endsWith(MITHRIL_GRUBBER_NAME)) {
            return MobVisual.SeaCreature.MITHRIL_GRUBBER;
        }
        return target;
    }

    /**
     * 長い文言を Hypixel の Trophy Frog 通知に似た
     * "{Aquatic/Magmatic記号} SEA CREATURE! You caught {名前}[ x2]!" の形に差し替える。
     * 差し替えたら true。元のメッセージは呼び出し側で差し止める。
     */
    private static boolean shorten(Minecraft client, SeaCreatureCatches.Catch caught, boolean doubleHook) {
        if (!ModConfig.Fishing.shortenSeaCreatureMessage
                || client.player == null || client.gui == null || client.gui.hud == null) {
            return false;
        }

        boolean lava = caught.lava();
        ChatFormatting headerColor = lava ? ChatFormatting.RED : ChatFormatting.BLUE;
        String icon = mobTypeIcon(client, lava);

        // ビットマップ記号は太字にすると横に潰れるため通常体、続く "SEA CREATURE!" を太字にする
        MutableComponent msg = Component.empty()
                .append(Component.literal(icon + " ").withStyle(headerColor))
                .append(Component.literal("SEA CREATURE!").withStyle(headerColor, ChatFormatting.BOLD))
                .append(Component.literal(" You caught ").withStyle(ChatFormatting.WHITE))
                .append(caught.formattedName());

        if (doubleHook) {
            msg.append(Component.literal(" x2").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        msg.append(Component.literal("!").withStyle(ChatFormatting.WHITE));

        client.gui.hud.getChat().addClientSystemMessage(msg);
        return true;
    }

    private static String mobTypeIcon(Minecraft client, boolean lava) {
        boolean hasHypixelPack = client.getResourceManager().getResource(HYPIXEL_MOBS_FONT_TEXTURE).isPresent();
        if (hasHypixelPack) {
            return lava ? MAGMATIC_ICON : AQUATIC_ICON;
        }
        return lava ? MAGMATIC_FALLBACK_ICON : AQUATIC_FALLBACK_ICON;
    }

    // 直前に Double Hook の合図が来ていたか。1度使ったら忘れる
    private static boolean wasDoubleHook() {
        boolean recent = System.currentTimeMillis() - doubleHookMillis < DOUBLE_HOOK_WINDOW_MS;
        doubleHookMillis = 0;
        return recent;
    }
}
