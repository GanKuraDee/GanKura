package com.deeply.gankura.mixin;

import com.teamresourceful.resourcefulconfig.client.ConfigScreen;
import com.teamresourceful.resourcefulconfig.client.ConfigScreenContext;
import com.teamresourceful.resourcefulconfig.client.components.options.OptionsListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 折りたたみを開閉したとき、一覧の組み直しと検索中かどうかの判定に使う */
@Mixin(ConfigScreen.class)
public interface ResourcefulConfigScreenAccessor {

    @Accessor("optionsList")
    OptionsListWidget gankura$getOptionsList();

    @Accessor("context")
    ConfigScreenContext gankura$getContext();
}
