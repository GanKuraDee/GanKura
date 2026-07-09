package com.deeply.gankura.config;

import io.github.notenoughupdates.moulconfig.gui.GuiComponent;
import io.github.notenoughupdates.moulconfig.gui.component.SliderComponent;
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor;
import io.github.notenoughupdates.moulconfig.observer.GetSetter;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

// MoulConfig標準の@ConfigEditorSliderは、スライダーの隣に数値入力欄(幅20pxの固定幅)を
// 表示するが、この欄はクリックしてキー入力するまで値が全体表示されず見切れてしまう。
// この欄自体を排除し、スライダーバーだけを表示する代替エディタ
@SuppressWarnings("unchecked")
public class PlainSliderEditor extends ComponentEditor {
    private static final int WIDTH = 100;

    private final GuiComponent component;

    public PlainSliderEditor(ProcessedOption option, float minValue, float maxValue, float minStep) {
        super(option);
        component = wrapComponent(new SliderComponent((GetSetter<Float>) option.intoProperty(), minValue, maxValue, minStep, WIDTH));
    }

    @Override
    public GuiComponent getDelegate() {
        return component;
    }
}
