package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
        Minecraft client = Minecraft.getInstance();

        // =======================================================
        // Stage 4 (Imminent) 検知
        // =======================================================
        if ("Imminent".equals(newStage)) {
            GameState.Broodmother.stage4StartTime = System.currentTimeMillis();

            // ★変更: TitleとSoundを独立して判定
            client.execute(() -> {
                if (ModConfig.INSTANCE.spidersDen.enableStage4Title) {
                    MutableComponent title = Component.literal("BROODMOTHER SOON").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                    NotificationUtils.showTitle(client, title, null);
                }
                if (ModConfig.INSTANCE.spidersDen.enableStage4Sound) {
                    NotificationUtils.playSound(client, SoundEvents.CREEPER_HURT, 1.0f, 1.0f);
                }
            });
        }
        // =======================================================
        // Stage 5 (Alive!) 検知
        // =======================================================
        else if ("Alive!".equals(newStage)) {
            if ("Imminent".equals(oldStage) && GameState.Broodmother.stage4StartTime > 0) {
                long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;

                if (ModConfig.INSTANCE.spidersDen.showBroodmotherStage4Duration) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            client.execute(() -> {
                                Component durationText = Component.literal(String.format("§aBroodmother Stage 4 Duration: %dm %ds", seconds / 60, seconds % 60));
                                NotificationUtils.sendSystemChat(client, durationText);
                            });
                        }
                    }, 100);
                }
            }
            GameState.Broodmother.stage4StartTime = 0;

            // ★変更: TitleとSoundを独立して判定
            client.execute(() -> {
                if (ModConfig.INSTANCE.spidersDen.enableStage5Title) {
                    MutableComponent title = Component.literal("BROODMOTHER SPAWNED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                    NotificationUtils.showTitle(client, title, null);
                }
                if (ModConfig.INSTANCE.spidersDen.enableStage5Sound) {
                    NotificationUtils.playSound(client, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
                }
            });
        }
        else {
            if (!"Imminent".equals(newStage)) {
                GameState.Broodmother.stage4StartTime = 0;
            }
        }
    }
}