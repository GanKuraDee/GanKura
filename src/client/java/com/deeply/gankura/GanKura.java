package com.deeply.gankura;

import com.deeply.gankura.handler.NetworkHandler;
import com.deeply.gankura.scanner.ItemDropScanner;
import com.deeply.gankura.scanner.LocationScanner;
import com.deeply.gankura.scanner.StageScanner;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GanKura implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("GanKura");

    @Override
    public void onInitializeClient() {
        NetworkHandler.init();
        StageScanner.register();
        LocationScanner.register();

        // ★追加
        ItemDropScanner.register();

        LOGGER.info("GanKura initialized (Refactored).");
    }
}