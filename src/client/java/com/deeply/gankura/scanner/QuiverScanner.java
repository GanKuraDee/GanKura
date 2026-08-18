package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.util.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 矢筒で選んでいる矢と残り本数を読む。
//
// Hypixel はホットバーの一番右(スロット8)を SkyBlock Menu に差し替えていて、
// その説明文に "Active Arrow: <矢の名前> (<残数>)" の行がある。
// 弓を持っている間だけ、この枠が矢そのものの見本に差し替わり、
// 名前が矢の種類、説明文が "Arrows Remaining: <残数>" になる。どちらからでも読めるようにしておく
public class QuiverScanner {

    // Hypixel が SkyBlock Menu / 矢の見本を置いているホットバーの位置
    private static final int MENU_SLOT = 8;

    private static final Pattern ACTIVE_ARROW = Pattern.compile("Active Arrow: (?<type>.*) \\((?<amount>[\\d,]+)\\)");
    private static final Pattern ARROWS_REMAINING = Pattern.compile("Arrows Remaining: (?<amount>[\\d,]+)");

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(QuiverScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (!GameState.Server.isSkyblock()) {
            clear();
            return;
        }

        ItemStack stack = client.player.getInventory().getStack(MENU_SLOT);
        if (stack.isEmpty()) {
            clear();
            return;
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            clear();
            return;
        }

        // 弓を持っていないときは SkyBlock Menu の説明文に選択中の矢が載っている
        for (Text line : lore.lines()) {
            String legacy = ScoreboardUtils.toLegacyString(line);
            Matcher matcher = ACTIVE_ARROW.matcher(ScoreboardUtils.stripColor(legacy));
            if (matcher.find()) {
                // レアリティごとの色が付いているので、色コードごと切り出して持っておく
                apply(coloredSlice(legacy, matcher.start("type"), matcher.end("type")), matcher.group("amount"));
                return;
            }
        }

        // 弓を持っている間は矢そのものが置かれ、名前が種類・説明文が残数になる
        for (Text line : lore.lines()) {
            Matcher matcher = ARROWS_REMAINING.matcher(ScoreboardUtils.stripColor(ScoreboardUtils.toLegacyString(line)));
            if (matcher.find()) {
                apply(ScoreboardUtils.toLegacyString(stack.getName()).trim(), matcher.group("amount"));
                return;
            }
        }

        clear();
    }

    private static void apply(String arrow, String amount) {
        if (ScoreboardUtils.stripColor(arrow).isBlank()) {
            clear();
            return;
        }
        GameState.Player.quiverArrow = arrow;
        GameState.Player.quiverArrowCount = parseAmount(amount);
    }

    private static void clear() {
        GameState.Player.quiverArrow = null;
        GameState.Player.quiverArrowCount = 0;
    }

    private static int parseAmount(String text) {
        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 色コードを除いた位置 [start, end) にあたる部分を、そこで効いている色ごと切り出す。
    // 色を付けたまま名前だけを取り出したいので、位置合わせは色を抜いた文字数で行う
    private static String coloredSlice(String legacy, int start, int end) {
        StringBuilder active = new StringBuilder();
        StringBuilder out = new StringBuilder();
        String prefix = null;
        int plain = 0;

        for (int i = 0; i < legacy.length(); i++) {
            char c = legacy.charAt(i);

            if (c == '§' && i + 1 < legacy.length()) {
                String code = legacy.substring(i, i + 2);
                if (plain >= start && plain < end) {
                    out.append(code);
                } else if (plain < start) {
                    // 色コードと §r はそれまでの装飾を打ち消すので、そこから積み直す
                    char kind = Character.toLowerCase(legacy.charAt(i + 1));
                    if ((kind >= '0' && kind <= '9') || (kind >= 'a' && kind <= 'f') || kind == 'r') {
                        active.setLength(0);
                    }
                    active.append(code);
                }
                i++;
                continue;
            }

            if (plain == start && prefix == null) prefix = active.toString();
            if (plain >= start && plain < end) out.append(c);
            plain++;
        }

        // 色が付いていなければ白。HUD側で前の色を引きずらないようにする
        return (prefix == null || prefix.isEmpty() ? "§f" : prefix) + out;
    }
}
