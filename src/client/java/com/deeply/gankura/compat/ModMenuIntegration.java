package com.deeply.gankura.compat;

import com.deeply.gankura.data.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // parent には「ModMenuの画面（閉じた後に戻る画面）」が入ってきます
        return parent -> {
            try {
                // GanKura.java で実装したのと同じ手順で MoulConfig の画面を生成します
                MoulConfigProcessor<ModConfig> processor = MoulConfigProcessor.withDefaults(ModConfig.INSTANCE);
                ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);
                driver.processConfig(ModConfig.INSTANCE);

                MoulConfigEditor<ModConfig> editor = new MoulConfigEditor<>(processor);
                GuiElementComponent editorComponent = new GuiElementComponent(editor);
                GuiContext guiContext = new GuiContext(editorComponent);

                // 第3引数に parent を渡すことで、設定画面を閉じた時に ModMenu に戻れるようになります
                return new MoulConfigScreenComponent(
                        Component.literal("GanKura Configuration"),
                        guiContext,
                        parent
                );
            } catch (Exception e) {
                System.err.println("Failed to create MoulConfig screen for ModMenu!");
                e.printStackTrace();
                // エラーが起きた場合は安全のため元の画面（ModMenu）をそのまま返します
                return parent;
            }
        };
    }
}