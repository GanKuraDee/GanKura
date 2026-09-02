package com.deeply.gankura.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** ツールチップの行を組み直すための共通の道具 */
public final class TooltipText {

    private TooltipText() {
    }

    /**
     * 行を頭から {@code keep} 文字だけ残し、その後ろに文字を足す。
     *
     * 行は色ごとに分かれていることがあるので、1文字ずつの書式を保ったまま
     * 組み直す。足す文字には、残した最後の文字と同じ書式を使う
     */
    public static Component appendKeeping(Component line, int keep, String added) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();

        line.visit((FormattedText.StyledContentConsumer<Object>) (style, part) -> {
            text.append(part);
            for (int i = 0; i < part.length(); i++) styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent result = Component.empty();
        StringBuilder run = new StringBuilder();
        Style runStyle = Style.EMPTY;

        for (int i = 0; i < Math.min(keep, text.length()); i++) {
            Style style = styles.get(i);
            if (!run.isEmpty() && !runStyle.equals(style)) {
                result.append(Component.literal(run.toString()).withStyle(runStyle));
                run.setLength(0);
            }
            runStyle = style;
            run.append(text.charAt(i));
        }
        if (!run.isEmpty()) result.append(Component.literal(run.toString()).withStyle(runStyle));

        return result.append(Component.literal(added).withStyle(runStyle));
    }
}
