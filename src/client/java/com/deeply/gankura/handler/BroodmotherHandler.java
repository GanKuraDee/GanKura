package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import java.util.List;
import java.util.regex.Matcher;

public class BroodmotherHandler {
    public static void processTabList(List<String> lines) {
        for (String line : lines) {
            Matcher bmMatcher = ModConstants.BROODMOTHER_PATTERN.matcher(line);
            if (bmMatcher.find()) {
                String bmStageName = bmMatcher.group(1).trim();
                updateBroodmotherStage(bmStageName);
                return;
            }
        }
    }

    private static void updateBroodmotherStage(String newStage) {
        String oldStage = GameState.Broodmother.stage;
        if (oldStage.equals(newStage)) return;
        GameState.Broodmother.stage = newStage;
        if ("Imminent".equals(newStage)) {
            GameState.Broodmother.stage4StartTime = System.currentTimeMillis();
        } else if (!"Alive!".equals(newStage) && !"Imminent".equals(newStage)) {
            GameState.Broodmother.stage4StartTime  = 0;
        }
    }
}