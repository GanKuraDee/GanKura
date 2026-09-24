package com.deeply.gankura.config;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.mixin.ResourcefulConfigScreenAccessor;
import com.deeply.gankura.mixin.ResourcefulListWidgetAccessor;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigButton;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigElement;
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement;
import com.teamresourceful.resourcefulconfig.client.ConfigScreen;
import com.teamresourceful.resourcefulconfig.client.components.base.ListWidget;
import com.teamresourceful.resourcefulconfig.client.components.options.Options;
import com.teamresourceful.resourcefulconfig.client.components.options.OptionsListWidget;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 設定画面の折りたたみ見出し。MoulConfig のアコーディオンの代わり。
 *
 * ResourcefulConfig には項目をその場で畳む仕組みが無いので、
 * 一覧を組み立てる {@link Options#populateOptions} に割り込んで、
 * 見出しの行を差し込みつつ、閉じている見出しの下の項目を飛ばしている。
 * どの項目がどの見出しの下にあるかは {@link ConfigGroups} にある。
 */
public final class ConfigAccordions {

    /** 見出し1段ぶんの字下げ（GUI 座標） */
    private static final int INDENT_STEP = 12;

    // 見出しの行を作るために元の populateOptions を1項目ずつ呼ぶので、その間は割り込まない
    private static boolean populating;

    // 行ごとの字下げ。一覧を組み直すたびに行は作り直されるので、古い行は勝手に消えていく
    private static final Map<ListWidget.Item, Integer> INDENTS = new WeakHashMap<>();

    private ConfigAccordions() {
    }

    /**
     * GanKura の設定なら自前で一覧を組み立てて true を返す。それ以外の設定には手を出さない
     */
    public static boolean populate(OptionsListWidget widget, List<ResourcefulConfigElement> elements) {
        if (populating) return false;
        if (elements.stream().noneMatch(e -> ConfigGroups.GROUP_OF.containsKey(keyOf(e)))) return false;

        // 検索中は、畳んだ見出しの下にある一致も見えるよう全部開いて出す
        boolean searching = isSearching();
        Set<String> expanded = expandedSet();
        Set<String> shown = new HashSet<>();

        populating = true;
        try {
            for (ResourcefulConfigElement element : elements) {
                boolean visible = true;
                List<String> chain = chainOf(ConfigGroups.GROUP_OF.get(keyOf(element)));
                for (int depth = 0; depth < chain.size() && visible; depth++) {
                    String group = chain.get(depth);
                    boolean open = searching || expanded.contains(group);
                    if (shown.add(group)) {
                        AccordionHeaderItem header = new AccordionHeaderItem(group, open, searching);
                        INDENTS.put(header, depth * INDENT_STEP);
                        widget.add(header);
                    }
                    visible = open;
                }
                if (visible) addIndented(widget, element, chain.size() * INDENT_STEP);
            }
        } finally {
            populating = false;
        }
        return true;
    }

    /** 元の処理で1項目ぶんの行を足し、足された行に字下げを付ける */
    private static void addIndented(OptionsListWidget widget, ResourcefulConfigElement element, int indent) {
        List<ListWidget.Item> items = ((ResourcefulListWidgetAccessor) widget).gankura$getItems();
        int before = items.size();
        Options.populateOptions(widget, List.of(element));
        if (indent == 0) return;
        for (int i = before; i < items.size(); i++) {
            INDENTS.put(items.get(i), indent);
        }
    }

    /** 行の字下げ。登録していない行は 0 */
    public static int indentOf(ListWidget.Item item) {
        Integer indent = INDENTS.get(item);
        return indent == null ? 0 : indent;
    }

    /** 見出しを開く・閉じる。開閉の状態は設定ファイルに残る */
    public static void toggle(String group) {
        Set<String> expanded = expandedSet();
        if (!expanded.remove(group)) expanded.add(group);
        ModConfig.expandedAccordions = expanded.toArray(new String[0]);
        refresh();
    }

    // 並びを組み直す。スクロール位置は保ったままにする
    private static void refresh() {
        if (!(Minecraft.getInstance().screen instanceof ConfigScreen screen)) return;
        OptionsListWidget list = ((ResourcefulConfigScreenAccessor) screen).gankura$getOptionsList();
        if (list == null) return;

        ResourcefulListWidgetAccessor scroll = (ResourcefulListWidgetAccessor) list;
        double position = scroll.gankura$getScroll();
        screen.updateOptions();
        scroll.gankura$setScroll(position);
        scroll.gankura$updateScrollBar();
    }

    private static boolean isSearching() {
        if (!(Minecraft.getInstance().screen instanceof ConfigScreen screen)) return false;
        var context = ((ResourcefulConfigScreenAccessor) screen).gankura$getContext();
        return context != null && !context.getQuery().isBlank();
    }

    private static Set<String> expandedSet() {
        Set<String> set = new LinkedHashSet<>();
        if (ModConfig.expandedAccordions != null) Collections.addAll(set, ModConfig.expandedAccordions);
        return set;
    }

    /** 見出しを外側から順に並べる */
    private static List<String> chainOf(String group) {
        List<String> chain = new ArrayList<>();
        for (String g = group; g != null; g = ConfigGroups.PARENT.get(g)) {
            chain.add(0, g);
        }
        return chain;
    }

    /** 項目の翻訳キー。見出しの表はこのキーで引く */
    private static String keyOf(ResourcefulConfigElement element) {
        if (element instanceof ResourcefulConfigEntryElement entry) {
            return entry.entry().options().title().translation();
        }
        if (element instanceof ResourcefulConfigButton button) {
            return button.title();
        }
        return null;
    }
}
