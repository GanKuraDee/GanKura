package com.deeply.gankura.data;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record CrimsonBossEntry(
        String nameTag,
        int glowColorRGB,
        int tracerColorARGB,
        Supplier<Boolean> enableHighlight,
        Supplier<Boolean> enableTracer,
        Supplier<Boolean> enableNameplate,
        Supplier<Boolean> getIsDetected,
        Consumer<Boolean> setIsDetected,
        Supplier<String> getHealth,
        Consumer<String> setHealth
) {}
