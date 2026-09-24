package com.deeply.gankura.mixin;

import com.deeply.gankura.config.ConfigAccordions;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigElement;
import com.teamresourceful.resourcefulconfig.client.components.options.Options;
import com.teamresourceful.resourcefulconfig.client.components.options.OptionsListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 設定画面の一覧を組み立てるところに割り込み、MoulConfig の頃のような折りたたみ見出しを差し込む。
 * GanKura 以外の設定画面にはそのまま元の処理を通す
 */
@Mixin(Options.class)
public class ResourcefulConfigOptionsMixin {

    @Inject(method = "populateOptions", at = @At("HEAD"), cancellable = true)
    private static void gankura$populateWithAccordions(OptionsListWidget widget,
                                                       List<ResourcefulConfigElement> elements,
                                                       CallbackInfo ci) {
        if (ConfigAccordions.populate(widget, elements)) ci.cancel();
    }
}
